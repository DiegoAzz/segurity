package com.norton.parental.companion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LockOverlayService extends Service implements View.OnClickListener {

    public static final String ACTION_SHOW_LOCK = "ACTION_SHOW_LOCK";
    public static final String ACTION_HIDE_LOCK = "ACTION_HIDE_LOCK";
    private static final String CHANNEL_ID = "norton_lock_channel";

    private static boolean sIsLocked = false;
    private WindowManager mWindowManager;
    private View mOverlayView;
    public Handler mHandler;
    public String mServerBaseUrl = "https://segurity.appzplay.online";
    private PollRunnable mPollRunnable;

    public static boolean isCurrentlyLocked() {
        return sIsLocked;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Norton Companion Activo")
                .setContentText("Supervisión y control parental en ejecución")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();

        startForeground(101, notification);

        android.content.SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("was_locked", false)) {
            showOverlay();
        }

        mPollRunnable = new PollRunnable(this);
        mHandler.post(mPollRunnable);
    }

    public static class PollRunnable implements Runnable {
        private final LockOverlayService mService;

        public PollRunnable(LockOverlayService service) {
            mService = service;
        }

        @Override
        public void run() {
            if (mService != null) {
                new PollWorker(mService).start();
                if (mService.mHandler != null) {
                    mService.mHandler.postDelayed(this, 1500);
                }
            }
        }
    }

    public static class PollWorker extends Thread {
        private final LockOverlayService mService;

        public PollWorker(LockOverlayService service) {
            mService = service;
        }

        @Override
        public void run() {
            // Revisa tanto la nube en Coolify como la red local
            String[] servers = new String[] {
                mService.mServerBaseUrl,
                "https://segurity.appzplay.online",
                "http://192.168.1.124:3080"
            };

            for (String serverUrl : servers) {
                if (serverUrl == null || serverUrl.isEmpty()) continue;
                try {
                    URL url = new URL(serverUrl + "/api/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(1500);
                    conn.setReadTimeout(1500);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        final boolean locked = sb.toString().contains("\"instantLockActive\":true");

                        if (mService.mHandler != null) {
                            mService.mHandler.post(new StateUpdater(mService, locked));
                        }
                        conn.disconnect();
                        break; // Servidor activo encontrado
                    }
                    conn.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }

    public static class StateUpdater implements Runnable {
        private final LockOverlayService mService;
        private final boolean mLocked;

        public StateUpdater(LockOverlayService service, boolean locked) {
            mService = service;
            mLocked = locked;
        }

        @Override
        public void run() {
            if (mService != null) {
                if (mLocked && !sIsLocked) {
                    mService.showOverlay();
                } else if (!mLocked && sIsLocked) {
                    mService.hideOverlay();
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_SHOW_LOCK.equals(action)) {
                showOverlay();
            } else if (ACTION_HIDE_LOCK.equals(action)) {
                hideOverlay();
            }
            if (intent.hasExtra("server_url")) {
                mServerBaseUrl = intent.getStringExtra("server_url");
            }
        }
        return START_STICKY;
    }

    public void showOverlay() {
        if (sIsLocked && mOverlayView != null) return;
        sIsLocked = true;

        getSharedPreferences("norton_prefs", MODE_PRIVATE).edit().putBoolean("was_locked", true).apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            return;
        }

        try {
            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.CENTER;

            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            mOverlayView = inflater.inflate(R.layout.overlay_lock, null);

            Button btnAsk = mOverlayView.findViewById(R.id.btn_overlay_ask);
            btnAsk.setOnClickListener(this);

            Button btnCall = mOverlayView.findViewById(R.id.btn_overlay_call);
            btnCall.setOnClickListener(this);

            mWindowManager.addView(mOverlayView, params);

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(400);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_overlay_ask) {
            Toast.makeText(this, "Petición de tiempo enviada a tus padres", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.btn_overlay_call) {
            Toast.makeText(this, "Conectando llamada de emergencia familiar...", Toast.LENGTH_SHORT).show();
        }
    }

    public void hideOverlay() {
        sIsLocked = false;
        getSharedPreferences("norton_prefs", MODE_PRIVATE).edit().putBoolean("was_locked", false).apply();
        if (mOverlayView != null && mWindowManager != null) {
            try {
                mWindowManager.removeView(mOverlayView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mOverlayView = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Norton Companion Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
        if (mHandler != null && mPollRunnable != null) {
            mHandler.removeCallbacks(mPollRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
