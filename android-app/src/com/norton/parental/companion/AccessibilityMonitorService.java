package com.norton.parental.companion;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AccessibilityMonitorService extends AccessibilityService {

    private static final Set<String> sBlockedPackages = Collections.synchronizedSet(new HashSet<String>());
    public Handler mHandler;
    public String mServerBaseUrl = "https://segurity.appzplay.online";
    private SyncRunnable mSyncRunnable;

    public static boolean isPackageBlocked(String packageName) {
        if (sBlockedPackages.contains(packageName)) return true;
        if (sBlockedPackages.contains("com.google.android.youtube")) {
            if (packageName.equals("com.google.android.youtube")
                    || packageName.equals("com.google.android.apps.youtube.kids")
                    || packageName.equals("com.google.android.apps.youtube.music")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        mHandler = new Handler(Looper.getMainLooper());
        mSyncRunnable = new SyncRunnable(this);
        mHandler.post(mSyncRunnable);
    }

    public static class SyncRunnable implements Runnable {
        private final AccessibilityMonitorService mService;

        public SyncRunnable(AccessibilityMonitorService service) {
            mService = service;
        }

        @Override
        public void run() {
            if (mService != null) {
                new FetchRulesThread(mService).start();
                if (mService.mHandler != null) {
                    mService.mHandler.postDelayed(this, 2000);
                }
            }
        }
    }

    public static class FetchRulesThread extends Thread {
        private final AccessibilityMonitorService mService;

        public FetchRulesThread(AccessibilityMonitorService service) {
            mService = service;
        }

        @Override
        public void run() {
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
                        String json = sb.toString();

                        int idx = json.indexOf("\"blockedPackages\":[");
                        if (idx != -1) {
                            int endIdx = json.indexOf("]", idx);
                            if (endIdx != -1) {
                                String arrayStr = json.substring(idx + 19, endIdx);
                                String[] pkgs = arrayStr.replace("\"", "").split(",");
                                sBlockedPackages.clear();
                                for (String p : pkgs) {
                                    String clean = p.trim();
                                    if (!clean.isEmpty()) {
                                        sBlockedPackages.add(clean);
                                    }
                                }
                            }
                        }
                        conn.disconnect();
                        break;
                    }
                    conn.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }

    public static class ToastRunnable implements Runnable {
        private final AccessibilityMonitorService mService;
        private final String mPkg;

        public ToastRunnable(AccessibilityMonitorService service, String pkg) {
            mService = service;
            mPkg = pkg;
        }

        @Override
        public void run() {
            if (mService != null) {
                Toast.makeText(mService, "🚫 Esta aplicación está bloqueada por tus padres.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        final String pkg = event.getPackageName().toString();

        if (pkg.equals(getPackageName())
                || pkg.equals("com.android.systemui")
                || pkg.contains("launcher")
                || pkg.contains("trebuchet")) {
            return;
        }

        // 1. Pausa Instantánea Activa
        if (LockOverlayService.isCurrentlyLocked()) {
            Intent lockIntent = new Intent(this, LockOverlayService.class);
            lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
            startService(lockIntent);
            return;
        }

        // 2. Si la aplicación específica (ej. YouTube) está BLOQUEADA
        if (isPackageBlocked(pkg)) {
            performGlobalAction(GLOBAL_ACTION_HOME);

            Intent lockIntent = new Intent(this, LockOverlayService.class);
            lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
            lockIntent.putExtra("blocked_app", pkg);
            startService(lockIntent);

            if (mHandler != null) {
                mHandler.post(new ToastRunnable(this, pkg));
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null && mSyncRunnable != null) {
            mHandler.removeCallbacks(mSyncRunnable);
        }
    }
}
