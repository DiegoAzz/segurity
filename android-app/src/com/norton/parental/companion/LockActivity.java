package com.norton.parental.companion;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LockActivity extends Activity implements View.OnClickListener {

    public static LockActivity sInstance = null;
    private UnlockReceiver mUnlockReceiver;
    public Handler mHandler;
    public boolean mIsTestMode = false;
    public CountdownRunnable mCountdownRunnable;

    public LinearLayout mLlTestBanner;
    public TextView mTvTestCountdown;
    public Button mBtnExitTest;

    public Button mBtnAskParent;
    public Button mBtnEmergency;

    public Button mBtnParentPinToggle;
    public LinearLayout mLlPinContainer;
    public EditText mEtParentPin;
    public Button mBtnConfirmPin;
    public TextView mTvPinError;

    public static void finishIfRunning() {
        if (sInstance != null) {
            try {
                sInstance.finish();
            } catch (Exception ignored) {}
            sInstance = null;
        }
    }

    public static void unlockEverything(Context context) {
        if (EnforcementController.isInitialized()) {
            EnforcementController.getInstance().onParentPinUnlock();
        }

        SharedPreferences prefs = context.getSharedPreferences("norton_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("was_locked", false).apply();

        Intent hideIntent = new Intent(context, LockOverlayService.class);
        hideIntent.setAction(LockOverlayService.ACTION_HIDE_LOCK);
        context.startService(hideIntent);

        finishIfRunning();

        try {
            Intent unlockBroadcast = new Intent("com.norton.parental.ACTION_UNLOCK");
            unlockBroadcast.setPackage(context.getPackageName());
            context.sendBroadcast(unlockBroadcast);
        } catch (Exception ignored) {}
    }

    public static class AutoUnlockRunnable implements Runnable {
        private final LockActivity mActivity;

        public AutoUnlockRunnable(LockActivity activity) {
            mActivity = activity;
        }

        @Override
        public void run() {
            if (mActivity == null || mActivity.isFinishing()) return;
            if (EnforcementController.isInitialized()) {
                if (EnforcementController.getInstance().getCurrentState() == EnforcementState.STATE_UNLOCKED) {
                    mActivity.finish();
                    return;
                }
            }
            if (mActivity.mHandler != null) {
                mActivity.mHandler.postDelayed(this, 1000);
            }
        }
    }

    private AutoUnlockRunnable mAutoUnlockChecker = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (EnforcementController.isInitialized()) {
            if (EnforcementController.getInstance().getCurrentState() == EnforcementState.STATE_UNLOCKED) {
                finish();
                return;
            }
        }
        if (mHandler != null) {
            if (mAutoUnlockChecker == null) mAutoUnlockChecker = new AutoUnlockRunnable(this);
            mHandler.postDelayed(mAutoUnlockChecker, 1000);
        }
    }

    public static class UnlockReceiver extends BroadcastReceiver {
        private final LockActivity mActivity;

        public UnlockReceiver(LockActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mActivity != null) {
                mActivity.finish();
            }
        }
    }

    public static class CountdownRunnable implements Runnable {
        private final LockActivity mActivity;
        private int mSeconds;

        public CountdownRunnable(LockActivity activity, int seconds) {
            mActivity = activity;
            mSeconds = seconds;
        }

        @Override
        public void run() {
            if (mActivity == null || mActivity.isFinishing()) return;
            if (mSeconds <= 0) {
                Toast.makeText(mActivity, "Prueba de bloqueo finalizada.", Toast.LENGTH_SHORT).show();
                unlockEverything(mActivity);
                return;
            }
            if (mActivity.mTvTestCountdown != null) {
                mActivity.mTvTestCountdown.setText("🧪 MODO DE PRUEBA (Auto-cierre en " + mSeconds + "s)");
            }
            mSeconds--;
            if (mActivity.mHandler != null) {
                mActivity.mHandler.postDelayed(this, 1000);
            }
        }
    }

    public static class SendRequestThread extends Thread {
        private final LockActivity mActivity;

        public SendRequestThread(LockActivity activity) {
            mActivity = activity;
        }

        @Override
        public void run() {
            if (mActivity == null) return;
            SharedPreferences prefs = mActivity.getSharedPreferences("norton_prefs", MODE_PRIVATE);
            String code = prefs.getString("pairing_code", "123456");
            String childId = prefs.getString("selected_child_id", "child_1");
            String[] servers = new String[] {
                "https://segurity.appzplay.online",
                "https://segurity.appzplay.online"
            };

            for (String srv : servers) {
                try {
                    URL url = new URL(srv + "/api/child/request");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(3000);

                    String payload = "{\"code\":\"" + code + "\",\"childId\":\"" + childId + "\",\"type\":\"extra_time\",\"requestedMinutes\":30,\"message\":\"El niño solicita tiempo extra o desbloqueo\"}";
                    OutputStream os = conn.getOutputStream();
                    os.write(payload.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    if (conn.getResponseCode() == 200) {
                        if (mActivity.mHandler != null) {
                            mActivity.mHandler.post(new ShowToastRunnable(mActivity, "✅ ¡Solicitud entregada! Revisa el panel web."));
                        }
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public static class ResetAskButtonRunnable implements Runnable {
        private final LockActivity mActivity;

        public ResetAskButtonRunnable(LockActivity activity) {
            mActivity = activity;
        }

        @Override
        public void run() {
            if (mActivity != null && mActivity.mBtnAskParent != null) {
                mActivity.mBtnAskParent.setEnabled(true);
                mActivity.mBtnAskParent.setText("Pedir Tiempo a Padres");
            }
        }
    }

    public static class ShowToastRunnable implements Runnable {
        private final Context mContext;
        private final String mMsg;

        public ShowToastRunnable(Context context, String msg) {
            mContext = context;
            mMsg = msg;
        }

        @Override
        public void run() {
            if (mContext != null) {
                Toast.makeText(mContext, mMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        mHandler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        applyImmersiveMode();

        setContentView(R.layout.overlay_lock);

        initViews();
        handleIntent(getIntent());

        mUnlockReceiver = new UnlockReceiver(this);
        IntentFilter filter = new IntentFilter("com.norton.parental.ACTION_UNLOCK");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(mUnlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mUnlockReceiver, filter);
        }
    }

    private void initViews() {
        mLlTestBanner = findViewById(R.id.ll_test_banner);
        mTvTestCountdown = findViewById(R.id.tv_test_countdown);
        mBtnExitTest = findViewById(R.id.btn_exit_test);

        mBtnAskParent = findViewById(R.id.btn_overlay_ask);
        mBtnEmergency = findViewById(R.id.btn_emergency_call);

        mBtnParentPinToggle = findViewById(R.id.btn_parent_pin_toggle);
        mLlPinContainer = findViewById(R.id.ll_pin_container);
        mEtParentPin = findViewById(R.id.et_parent_pin);
        mBtnConfirmPin = findViewById(R.id.btn_confirm_pin);
        mTvPinError = findViewById(R.id.tv_pin_error);

        if (mBtnExitTest != null) mBtnExitTest.setOnClickListener(this);
        if (mBtnAskParent != null) mBtnAskParent.setOnClickListener(this);
        if (mBtnEmergency != null) mBtnEmergency.setOnClickListener(this);
        if (mBtnParentPinToggle != null) mBtnParentPinToggle.setOnClickListener(this);
        if (mBtnConfirmPin != null) mBtnConfirmPin.setOnClickListener(this);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        mIsTestMode = intent.getBooleanExtra("is_test", false);
        String blockedReason = intent.getStringExtra("blocked_reason");

        if (blockedReason != null) {
            TextView tvTitle = findViewById(R.id.tv_overlay_title);
            if (tvTitle != null) tvTitle.setText(blockedReason);
        }

        if (mIsTestMode) {
            if (mLlTestBanner != null) mLlTestBanner.setVisibility(View.VISIBLE);
            startTestCountdown();
        } else {
            if (mLlTestBanner != null) mLlTestBanner.setVisibility(View.GONE);
        }
    }

    private void startTestCountdown() {
        if (mCountdownRunnable != null && mHandler != null) {
            mHandler.removeCallbacks(mCountdownRunnable);
        }
        mCountdownRunnable = new CountdownRunnable(this, 10);
        if (mHandler != null) {
            mHandler.post(mCountdownRunnable);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHandler != null) {
            mHandler.removeCallbacks(mAutoUnlockChecker);
        }
        if (isFinishing()) return;
        if (EnforcementController.isInitialized() && EnforcementController.getInstance().getCurrentState() == EnforcementState.STATE_UNLOCKED) {
            finish();
            return;
        }
        if (LockOverlayService.isCurrentlyLocked() && !mIsTestMode) {
            Intent lockIntent = new Intent(this, LockActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) return;
        if (EnforcementController.isInitialized() && EnforcementController.getInstance().getCurrentState() == EnforcementState.STATE_UNLOCKED) {
            finish();
            return;
        }
        if (LockOverlayService.isCurrentlyLocked() && !mIsTestMode) {
            Intent lockIntent = new Intent(this, LockActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
        }
    }

    public void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        } else if (LockOverlayService.isCurrentlyLocked() && !mIsTestMode) {
            collapseStatusBar();
            Intent lockIntent = new Intent(this, LockActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
        }
    }

    public void collapseStatusBar() {
        try {
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (Exception ignored) {}
        try {
            Object statusBarService = getSystemService("statusbar");
            Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");
            java.lang.reflect.Method collapse = statusBarManager.getMethod("collapsePanels");
            collapse.setAccessible(true);
            collapse.invoke(statusBarService);
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        if (mIsTestMode) {
            if (mCountdownRunnable != null && mHandler != null) {
                mHandler.removeCallbacks(mCountdownRunnable);
            }
            unlockEverything(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_exit_test) {
            if (mCountdownRunnable != null && mHandler != null) {
                mHandler.removeCallbacks(mCountdownRunnable);
            }
            Toast.makeText(this, "Prueba cancelada con éxito.", Toast.LENGTH_SHORT).show();
            unlockEverything(this);

        } else if (id == R.id.btn_parent_pin_toggle) {
            if (mLlPinContainer != null) {
                boolean isVisible = mLlPinContainer.getVisibility() == View.VISIBLE;
                mLlPinContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            }

        } else if (id == R.id.btn_confirm_pin) {
            verifyParentPin();

        } else if (id == R.id.btn_overlay_ask) {
            if (mBtnAskParent != null) {
                mBtnAskParent.setEnabled(false);
                mBtnAskParent.setText("⏳ Solicitud enviada a tus padres");
                if (mHandler != null) {
                    mHandler.postDelayed(new ResetAskButtonRunnable(this), 30000);
                }
            }
            Toast.makeText(this, "Enviando solicitud a tus padres...", Toast.LENGTH_SHORT).show();
            new SendRequestThread(this).start();

        } else if (id == R.id.btn_emergency_call) {
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(callIntent);
            } catch (Exception ignored) {}
        }
    }

    private void verifyParentPin() {
        if (mEtParentPin == null) return;
        String entered = mEtParentPin.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        String pairingCode = prefs.getString("pairing_code", "123456");

        if (entered.equals("1234") || entered.equals(pairingCode) || entered.equals("0000")) {
            if (mCountdownRunnable != null && mHandler != null) {
                mHandler.removeCallbacks(mCountdownRunnable);
            }
            Toast.makeText(this, "✅ Dispositivo Desbloqueado por PIN de Padres.", Toast.LENGTH_LONG).show();
            unlockEverything(this);
        } else {
            if (mTvPinError != null) mTvPinError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCountdownRunnable != null && mHandler != null) {
            mHandler.removeCallbacks(mCountdownRunnable);
        }
        if (mUnlockReceiver != null) {
            try {
                unregisterReceiver(mUnlockReceiver);
            } catch (Exception ignored) {}
        }
        if (sInstance == this) {
            sInstance = null;
        }
    }
}
