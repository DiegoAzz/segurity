package com.norton.parental.companion;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class LockActivity extends Activity implements View.OnClickListener {

    public static LockActivity sInstance = null;
    private UnlockReceiver mUnlockReceiver;

    public static void finishIfRunning() {
        if (sInstance != null) {
            try {
                sInstance.finish();
            } catch (Exception ignored) {}
            sInstance = null;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.overlay_lock);

        String blockedReason = getIntent().getStringExtra("blocked_reason");
        if (blockedReason != null) {
            TextView tvTitle = findViewById(R.id.tv_overlay_title);
            if (tvTitle != null) tvTitle.setText(blockedReason);
        }

        Button btnEmergency = findViewById(R.id.btn_emergency_call);
        if (btnEmergency != null) {
            btnEmergency.setOnClickListener(this);
        }

        mUnlockReceiver = new UnlockReceiver(this);
        registerReceiver(mUnlockReceiver, new IntentFilter("com.norton.parental.ACTION_UNLOCK"));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String blockedReason = intent.getStringExtra("blocked_reason");
            if (blockedReason != null) {
                TextView tvTitle = findViewById(R.id.tv_overlay_title);
                if (tvTitle != null) tvTitle.setText(blockedReason);
            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_emergency_call) {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sInstance == this) sInstance = null;
        if (mUnlockReceiver != null) {
            try {
                unregisterReceiver(mUnlockReceiver);
            } catch (Exception ignored) {}
        }
    }
}
