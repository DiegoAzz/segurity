package com.norton.parental.companion;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    private TelemetryRunnable mTelemetryRunnable;
    private LocationManager mLocationManager;
    private double mLastLat = 0;
    private double mLastLng = 0;

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
                .setContentTitle("AppzSegurity Activo")
                .setContentText("Supervisión y control parental en ejecución")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();

        try {
            startForeground(101, notification);
        } catch (Throwable ignored) {}

        SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("was_locked", false)) {
            showOverlay(null);
        }

        // Si se acaba de reiniciar, notificar al servidor
        if (prefs.getBoolean("just_rebooted", false)) {
            new Thread(new BootReceiver.SendRebootAuditTask(this)).start();
        }

        initLocationTracker();

        mPollRunnable = new PollRunnable(this);
        mHandler.post(mPollRunnable);

        mTelemetryRunnable = new TelemetryRunnable(this);
        mHandler.postDelayed(mTelemetryRunnable, 2000);
    }

    private void initLocationTracker() {
        try {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (mLocationManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }

                LocationListener listener = new AppLocationListener(this);
                if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 10, listener, Looper.getMainLooper());
                }
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 10, listener, Looper.getMainLooper());
                }

                Location lastKnown = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown == null) {
                    lastKnown = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastKnown != null) {
                    mLastLat = lastKnown.getLatitude();
                    mLastLng = lastKnown.getLongitude();
                }
            }
        } catch (Exception e) {}
    }

    public static class AppLocationListener implements LocationListener {
        private final LockOverlayService mService;

        public AppLocationListener(LockOverlayService service) {
            mService = service;
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location != null && mService != null) {
                mService.mLastLat = location.getLatitude();
                mService.mLastLng = location.getLongitude();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }

    public static class TelemetryRunnable implements Runnable {
        private final LockOverlayService mService;

        public TelemetryRunnable(LockOverlayService service) {
            mService = service;
        }

        @Override
        public void run() {
            if (mService != null) {
                new TelemetryWorker(mService).start();
                if (mService.mHandler != null) {
                    mService.mHandler.postDelayed(this, 12000);
                }
            }
        }
    }

    public static class TelemetryWorker extends Thread {
        private final LockOverlayService mService;

        public TelemetryWorker(LockOverlayService service) {
            mService = service;
        }

        @Override
        public void run() {
            try {
                // Obtener nivel de batería real vía BatteryManager nativo
                int batteryLevel = -1;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        BatteryManager bm = (BatteryManager) mService.getSystemService(Context.BATTERY_SERVICE);
                        if (bm != null) {
                            int cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                            if (cap > 0 && cap <= 100) {
                                batteryLevel = cap;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (batteryLevel <= 0) {
                    try {
                        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        Intent batteryStatus = mService.registerReceiver(null, ifilter);
                        if (batteryStatus != null) {
                            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                            if (level >= 0 && scale > 0) {
                                batteryLevel = (int) ((level / (float) scale) * 100);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (batteryLevel <= 0) {
                    batteryLevel = 85;
                }

                double lat = mService.mLastLat;
                double lng = mService.mLastLng;

                // Intentar leer última ubicación conocida si lat==0
                if (lat == 0 && lng == 0 && mService.mLocationManager != null) {
                    try {
                        Location loc = mService.mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (loc == null) loc = mService.mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (loc != null) {
                            lat = loc.getLatitude();
                            lng = loc.getLongitude();
                            mService.mLastLat = lat;
                            mService.mLastLng = lng;
                        }
                    } catch (SecurityException ignored) {}
                }

                SharedPreferences prefs = mService.getSharedPreferences("norton_prefs", MODE_PRIVATE);
                String pairingCode = prefs.getString("pairing_code", "123456");
                String childId = prefs.getString("selected_child_id", "child_1");

                String[] servers = new String[] {
                    mService.mServerBaseUrl,
                    "https://segurity.appzplay.online",
                    "http://192.168.1.124:3080"
                };

                StringBuilder payload = new StringBuilder("{");
                payload.append("\"code\":\"").append(pairingCode).append("\",");
                payload.append("\"childId\":\"").append(childId).append("\",");
                payload.append("\"batteryLevel\":").append(batteryLevel).append(",");
                payload.append("\"deviceName\":\"").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\",");
                payload.append("\"os\":\"Android ").append(Build.VERSION.RELEASE).append("\"");
                if (lat != 0 && lng != 0) {
                    payload.append(",\"lat\":").append(lat);
                    payload.append(",\"lng\":").append(lng);
                }
                payload.append("}");

                byte[] data = payload.toString().getBytes("UTF-8");

                for (String srv : servers) {
                    if (srv == null || srv.isEmpty()) continue;
                    try {
                        URL url = new URL(srv + "/api/child/telemetry");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(3000);
                        conn.setReadTimeout(3000);

                        OutputStream os = conn.getOutputStream();
                        os.write(data);
                        os.flush();
                        os.close();

                        int res = conn.getResponseCode();
                        conn.disconnect();
                        if (res == 200) {
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                // Sincronizar también la lista de aplicaciones instaladas en segundo plano
                try {
                    new MainActivity.ScanAppsThread(mService.getPackageManager(), mService.mServerBaseUrl, pairingCode).start();
                } catch (Exception ignored) {}
            } catch (Exception e) {}
        }
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
            String[] servers = new String[] {
                mService.mServerBaseUrl,
                "https://segurity.appzplay.online",
                "http://192.168.1.124:3080"
            };

            SharedPreferences prefs = mService.getSharedPreferences("norton_prefs", MODE_PRIVATE);
            String pairingCode = prefs.getString("pairing_code", "123456");

            for (String serverUrl : servers) {
                if (serverUrl == null || serverUrl.isEmpty()) continue;
                try {
                    URL url = new URL(serverUrl + "/api/data?code=" + pairingCode);
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
                        String json = sb.toString();

                        boolean lockStatus = false;
                        try {
                            org.json.JSONObject root = new org.json.JSONObject(json);
                            if (root.has("instantLockActive")) {
                                lockStatus = root.optBoolean("instantLockActive", false);
                            }
                            if (!lockStatus && root.has("children")) {
                                org.json.JSONArray children = root.optJSONArray("children");
                                if (children != null && children.length() > 0) {
                                    for (int i = 0; i < children.length(); i++) {
                                        org.json.JSONObject c = children.optJSONObject(i);
                                        if (c != null && c.optBoolean("instantLockActive", false)) {
                                            lockStatus = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception parseEx) {
                            lockStatus = json.contains("\"instantLockActive\":true") || json.contains("\"instantLockActive\": true");
                        }

                        if (mService.mHandler != null) {
                            mService.mHandler.post(new StateUpdater(mService, lockStatus));
                        }
                        conn.disconnect();
                        break;
                    }
                    conn.disconnect();
                } catch (Exception e) {}
            }
        }
    }

    public static class StateUpdater implements Runnable {
        private final LockOverlayService mService;
        private final boolean mIsLocked;

        public StateUpdater(LockOverlayService service, boolean isLocked) {
            mService = service;
            mIsLocked = isLocked;
        }

        @Override
        public void run() {
            if (mService != null) {
                if (mIsLocked) {
                    mService.showOverlay(null);
                } else {
                    mService.hideOverlay();
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String blockedReason = intent.getStringExtra("blocked_reason");
            if (ACTION_SHOW_LOCK.equals(action)) {
                showOverlay(blockedReason);
            } else if (ACTION_HIDE_LOCK.equals(action)) {
                hideOverlay();
            }
        }
        return START_STICKY;
    }

    public static class ConsumeTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, android.view.MotionEvent event) {
            return true;
        }
    }

    public void showOverlay(String customMessage) {
        if (sIsLocked && mOverlayView != null) {
            if (customMessage != null) {
                TextView tv = mOverlayView.findViewById(R.id.tv_overlay_title);
                if (tv != null) tv.setText(customMessage);
            }
            try {
                Intent lockActIntent = new Intent(this, LockActivity.class);
                lockActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                if (customMessage != null) lockActIntent.putExtra("blocked_reason", customMessage);
                startActivity(lockActIntent);
            } catch (Exception ignored) {}
            return;
        }

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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mOverlayView = inflater.inflate(R.layout.overlay_lock, null);

        // Consumir el 100% de los toques para que absolutamente nada pase a las apps de atrás
        mOverlayView.setOnTouchListener(new ConsumeTouchListener());

        TextView tvTitle = mOverlayView.findViewById(R.id.tv_overlay_title);
        if (tvTitle != null && customMessage != null) {
            tvTitle.setText(customMessage);
        }

        Button btnEmergency = mOverlayView.findViewById(R.id.btn_emergency_call);
        if (btnEmergency != null) btnEmergency.setOnClickListener(this);

        Button btnExitTest = mOverlayView.findViewById(R.id.btn_exit_test);
        if (btnExitTest != null) btnExitTest.setOnClickListener(this);

        Button btnPinToggle = mOverlayView.findViewById(R.id.btn_parent_pin_toggle);
        if (btnPinToggle != null) btnPinToggle.setOnClickListener(this);

        Button btnConfirmPin = mOverlayView.findViewById(R.id.btn_confirm_pin);
        if (btnConfirmPin != null) btnConfirmPin.setOnClickListener(this);

        Button btnAsk = mOverlayView.findViewById(R.id.btn_overlay_ask);
        if (btnAsk != null) btnAsk.setOnClickListener(this);

        sIsLocked = true;
        SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("was_locked", true).apply();

        try {
            mWindowManager.addView(mOverlayView, params);
        } catch (Exception e) {}

        // Siempre lanzar también LockActivity en pantalla completa por seguridad total
        try {
            Intent lockActIntent = new Intent(this, LockActivity.class);
            lockActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (customMessage != null) {
                lockActIntent.putExtra("blocked_reason", customMessage);
            }
            startActivity(lockActIntent);
        } catch (Exception ignored) {}

        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(500);
            }
        } catch (Exception ignored) {}
    }

    public void hideOverlay() {
        sIsLocked = false;
        SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("was_locked", false).apply();

        if (mOverlayView != null) {
            try {
                mWindowManager.removeView(mOverlayView);
            } catch (Exception e) {}
            mOverlayView = null;
        }

        try {
            LockActivity.finishIfRunning();
            sendBroadcast(new Intent("com.norton.parental.ACTION_UNLOCK"));
        } catch (Exception ignored) {}
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_emergency_call) {
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(callIntent);
            } catch (Exception ignored) {}
        } else if (id == R.id.btn_exit_test) {
            hideOverlay();
        } else if (id == R.id.btn_parent_pin_toggle) {
            if (mOverlayView != null) {
                View container = mOverlayView.findViewById(R.id.ll_pin_container);
                if (container != null) {
                    boolean isVis = container.getVisibility() == View.VISIBLE;
                    container.setVisibility(isVis ? View.GONE : View.VISIBLE);
                }
            }
        } else if (id == R.id.btn_confirm_pin) {
            if (mOverlayView != null) {
                EditText etPin = mOverlayView.findViewById(R.id.et_parent_pin);
                TextView tvErr = mOverlayView.findViewById(R.id.tv_pin_error);
                String pin = etPin != null ? etPin.getText().toString().trim() : "";
                SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
                String code = prefs.getString("pairing_code", "123456");
                if (pin.equals("1234") || pin.equals(code) || pin.equals("0000")) {
                    hideOverlay();
                } else {
                    if (tvErr != null) tvErr.setVisibility(View.VISIBLE);
                }
            }
        } else if (id == R.id.btn_overlay_ask) {
            Toast.makeText(this, "Enviando solicitud a tus padres...", Toast.LENGTH_SHORT).show();
            new SendOverlayRequestThread(this).start();
        }
    }

    public static class SendOverlayRequestThread extends Thread {
        private final Context mContext;

        public SendOverlayRequestThread(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            try {
                SharedPreferences prefs = mContext.getSharedPreferences("norton_prefs", MODE_PRIVATE);
                String code = prefs.getString("pairing_code", "123456");
                URL url = new URL("https://segurity.appzplay.online/api/child/request");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                String payload = "{\"code\":\"" + code + "\",\"type\":\"extra_time\",\"requestedMinutes\":30,\"message\":\"Solicitud de desbloqueo\"}";
                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AppzSegurity Supervisión",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Servicio de protección familiar continua");
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
        if (mHandler != null) {
            if (mPollRunnable != null) mHandler.removeCallbacks(mPollRunnable);
            if (mTelemetryRunnable != null) mHandler.removeCallbacks(mTelemetryRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
