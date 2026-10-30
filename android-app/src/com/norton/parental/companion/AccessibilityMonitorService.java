package com.norton.parental.companion;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Android Accessibility Service for FamilyGuard / AppzSegurity.
 *
 * Architecture Role:
 * - Pure sensor and reporter for window state and foreground activity.
 * - Forwards all events to EnforcementController (Single Authoritative Coordinator).
 * - Zero independent polling or conflicting authority.
 */
public class AccessibilityMonitorService extends AccessibilityService {

    public static AccessibilityMonitorService sInstance = null;

    private static final String[] DEFAULT_ADULT_KEYWORDS = new String[] {
            "xvideos", "pornhub", "xnxx", "redtube", "youporn", "chaturbate", "stripchat",
            "onlyfans", "erome", "cam4", "brazzers", "xhamster", "spankbang", "hentai",
            "rule34", "beeg", "tube8", "eporner", "txxx", "porno", "porn", "xxx", "sexo", "desnuda"
    };

    public Handler mHandler;
    public String mServerBaseUrl = "https://segurity.appzplay.online";
    private long mLastBlockTime = 0;
    private static boolean sRebootChecked = false;
    private long mLastServiceCheckTime = 0;

    public static class ToastRunnable implements Runnable {
        private final AccessibilityMonitorService mService;
        private final String mMessage;

        public ToastRunnable(AccessibilityMonitorService service, String message) {
            mService = service;
            mMessage = message;
        }

        @Override
        public void run() {
            if (mService != null) {
                Toast.makeText(mService, mMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void triggerHomeAction() {
        if (sInstance != null) {
            try {
                sInstance.performGlobalAction(GLOBAL_ACTION_HOME);
            } catch (Exception ignored) {}
        }
    }

    public void ensureLockServiceRunning() {
        try {
            Intent serviceIntent = new Intent(this, LockOverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception ignored) {}
    }

    public void checkAndReportReboot() {
        if (sRebootChecked) return;
        sRebootChecked = true;

        long uptimeMs = android.os.SystemClock.elapsedRealtime();
        if (uptimeMs < 180000) {
            new Thread(new BootReceiver.SendRebootAuditTask(this)).start();
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        mHandler = new Handler(Looper.getMainLooper());

        EnforcementController.init(this);
        ensureLockServiceRunning();
        checkAndReportReboot();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        checkAndReportReboot();

        long now = System.currentTimeMillis();
        if (now - mLastServiceCheckTime > 15000) {
            mLastServiceCheckTime = now;
            ensureLockServiceRunning();
        }

        if (event == null || event.getPackageName() == null) return;
        final String pkg = event.getPackageName().toString();

        // 1. Evitar auto-inspección de nuestra propia app para romper cualquier bucle de reentrada
        if (pkg.equals(getPackageName())) {
            return;
        }

        // 2. Si el teléfono está en Pausa Instantánea Total (FULL_DEVICE_LOCKED), bloquear accesos al sistema
        final EnforcementController controller = EnforcementController.getInstance();
        if (controller.getCurrentState() == EnforcementState.STATE_FULL_DEVICE_LOCKED) {
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
                } catch (Exception ignored) {}
            }
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
            return;
        }

        // 3. Protección contra desinstalación o detención forzada en Ajustes
        if (pkg.equals("com.android.settings") || pkg.contains("settings") || pkg.contains("packageinstaller")) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                try {
                    List<AccessibilityNodeInfo> nodes = source.findAccessibilityNodeInfosByText("AppzSegurity");
                    if (nodes == null || nodes.isEmpty()) {
                        nodes = source.findAccessibilityNodeInfosByText("Desinstalar");
                    }
                    if (nodes != null && !nodes.isEmpty()) {
                        performGlobalAction(GLOBAL_ACTION_HOME);
                        if (mHandler != null) {
                            mHandler.post(new ToastRunnable(this, "🔒 AppzSegurity está protegida contra desinstalación."));
                        }
                        return;
                    }
                } catch (Exception ignored) {
                } finally {
                    try { source.recycle(); } catch (Exception ignored) {}
                }
            }
        }

        // 4. Delegar la evaluación de la app al EnforcementController (coordinador único)
        controller.onForegroundAppChanged(pkg, StructuredLogger.EventSource.ACCESSIBILITY_WINDOW_CHANGE);

        // 5. Inspección profunda de texto en navegadores web (Chrome, Samsung Internet, Brave, etc.)
        boolean isBrowser = pkg.equals("com.android.chrome")
                || pkg.equals("com.chrome.beta")
                || pkg.equals("com.sec.android.app.sbrowser")
                || pkg.equals("org.mozilla.firefox")
                || pkg.equals("com.microsoft.emmx")
                || pkg.equals("com.opera.browser")
                || pkg.equals("com.opera.mini.native")
                || pkg.equals("com.brave.browser")
                || pkg.equals("com.duckduckgo.mobile.android")
                || pkg.contains("browser")
                || pkg.contains("chrome");

        if (isBrowser) {
            checkBrowserContentForBlockedWeb(event);
        }
    }

    private void checkBrowserContentForBlockedWeb(AccessibilityEvent event) {
        long now = System.currentTimeMillis();
        if (now - mLastBlockTime < 1000) return;

        // Revisar source del evento
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            try {
                String matched = scanNodeForBlockedText(source, 0);
                if (matched != null) {
                    onWebViolationDetected(matched);
                    return;
                }
            } finally {
                source.recycle();
            }
        }

        // Revisar ventana activa completa
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try {
                List<AccessibilityNodeInfo> urlBars = root.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar");
                if (urlBars != null) {
                    for (AccessibilityNodeInfo bar : urlBars) {
                        if (bar.getText() != null) {
                            String matched = testStringAgainstBlockRules(bar.getText().toString().toLowerCase().trim());
                            if (matched != null) {
                                bar.recycle();
                                onWebViolationDetected(matched);
                                return;
                            }
                        }
                        bar.recycle();
                    }
                }

                String matched = scanNodeForBlockedText(root, 0);
                if (matched != null) {
                    onWebViolationDetected(matched);
                }
            } finally {
                root.recycle();
            }
        }
    }

    private void onWebViolationDetected(String matched) {
        mLastBlockTime = System.currentTimeMillis();
        performGlobalAction(GLOBAL_ACTION_HOME);

        EnforcementController.getInstance().onWebSafetyViolation(matched, "Contenido para adultos o sitio no permitido");

        if (mHandler != null) {
            mHandler.post(new ToastRunnable(this, "🚫 AppzSegurity: Acceso denegado a " + matched));
        }

        android.content.SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        String pairingCode = prefs.getString("pairing_code", "123456");
        new ReportWebBlockThread(mServerBaseUrl, matched, "Filtro Web / Contenido para Adultos", pairingCode).start();
    }

    private String scanNodeForBlockedText(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 25) return null;

        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            String str = text.toString().toLowerCase().trim();
            String matched = testStringAgainstBlockRules(str);
            if (matched != null) return matched;
        }

        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.length() > 0) {
            String str = desc.toString().toLowerCase().trim();
            String matched = testStringAgainstBlockRules(str);
            if (matched != null) return matched;
        }

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                String matched = scanNodeForBlockedText(child, depth + 1);
                child.recycle();
                if (matched != null) return matched;
            }
        }
        return null;
    }

    private String testStringAgainstBlockRules(String text) {
        if (text == null || text.isEmpty()) return null;

        PolicyModel policy = EnforcementController.getInstance().getCurrentPolicy();
        if (policy != null) {
            // 1. Dominios bloqueados
            for (String domain : policy.getBlockedDomains()) {
                if (text.contains(domain)) return domain;
            }

            // 2. Filtro de adultos
            if (policy.isAdultBlocked()) {
                for (String kw : DEFAULT_ADULT_KEYWORDS) {
                    if (text.contains(kw)) return kw;
                }
            }

            // 3. Palabras clave adicionales
            for (String kw : policy.getBlockedKeywords()) {
                if (text.contains(kw)) return kw;
            }
        } else {
            for (String kw : DEFAULT_ADULT_KEYWORDS) {
                if (text.contains(kw)) return kw;
            }
        }

        return null;
    }

    public static class ReportWebBlockThread extends Thread {
        private final String mServerUrl;
        private final String mBlockedItem;
        private final String mReason;
        private final String mCode;

        public ReportWebBlockThread(String serverUrl, String blockedItem, String reason, String code) {
            mServerUrl = serverUrl;
            mBlockedItem = blockedItem;
            mReason = reason;
            mCode = code;
        }

        @Override
        public void run() {
            String[] servers = new String[] {
                    mServerUrl,
                    "https://segurity.appzplay.online",
                    "http://192.168.1.124:3080"
            };

            for (String srv : servers) {
                if (srv == null || srv.isEmpty()) continue;
                try {
                    URL url = new URL(srv + "/api/child/web-block");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    String payload = "{\"code\":\"" + mCode + "\",\"item\":\"" + mBlockedItem + "\",\"reason\":\"" + mReason + "\"}";
                    OutputStream os = conn.getOutputStream();
                    os.write(payload.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int res = conn.getResponseCode();
                    conn.disconnect();
                    if (res == 200) break;
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }
}
