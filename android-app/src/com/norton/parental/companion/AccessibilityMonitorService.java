package com.norton.parental.companion;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccessibilityMonitorService extends AccessibilityService {

    private static final Set<String> sBlockedPackages = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> sBlockedDomains = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> sBlockedKeywords = Collections.synchronizedSet(new HashSet<String>());
    private static boolean sAdultBlocked = true;

    private static final String[] DEFAULT_ADULT_KEYWORDS = new String[] {
        "xvideos", "pornhub", "xnxx", "redtube", "youporn", "chaturbate", "stripchat",
        "onlyfans", "erome", "cam4", "brazzers", "xhamster", "spankbang", "hentai",
        "rule34", "beeg", "tube8", "eporner", "txxx", "porno", "porn", "xxx", "sexo", "desnuda"
    };

    public Handler mHandler;
    public String mServerBaseUrl = "https://segurity.appzplay.online";
    private SyncRunnable mSyncRunnable;
    private long mLastBlockTime = 0;
    private static AccessibilityMonitorService sInstance = null;

    public static void updateBlockedPackages(Set<String> pkgs) {
        if (pkgs != null) {
            sBlockedPackages.clear();
            sBlockedPackages.addAll(pkgs);
            if (sInstance != null && sInstance.mHandler != null) {
                sInstance.mHandler.post(new CheckForegroundRunnable(sInstance));
            }
        }
    }

    public static boolean isPackageBlocked(String packageName) {
        if (packageName == null) return false;
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

    private static boolean sRebootChecked = false;
    private long mLastServiceCheckTime = 0;

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
        // Si el tiempo de encendido del teléfono es menor a 3 minutos (180s), se acaba de reiniciar
        if (uptimeMs < 180000) {
            new Thread(new BootReceiver.SendRebootAuditTask(this)).start();
        }

        // Si antes del reinicio estaba bloqueado, reactivar bloqueo inmediatamente
        android.content.SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("was_locked", false)) {
            Intent lockIntent = new Intent(this, LockOverlayService.class);
            lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
            startService(lockIntent);
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        mHandler = new Handler(Looper.getMainLooper());
        ensureLockServiceRunning();
        checkAndReportReboot();
        mSyncRunnable = new SyncRunnable(this);
        mHandler.post(mSyncRunnable);
    }

    public static class CheckForegroundRunnable implements Runnable {
        private final AccessibilityMonitorService mService;

        public CheckForegroundRunnable(AccessibilityMonitorService service) {
            mService = service;
        }

        @Override
        public void run() {
            if (mService != null) {
                mService.checkCurrentForegroundApp();
            }
        }
    }

    public static class SyncRunnable implements Runnable {
        private final AccessibilityMonitorService mService;

        public SyncRunnable(AccessibilityMonitorService service) {
            mService = service;
        }

        @Override
        public void run() {
            if (mService != null) {
                mService.checkCurrentForegroundApp();
                new FetchRulesThread(mService).start();
                if (mService.mHandler != null) {
                    mService.mHandler.postDelayed(this, 1000);
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
            android.content.SharedPreferences prefs = mService.getSharedPreferences("norton_prefs", MODE_PRIVATE);
            String pairingCode = prefs.getString("pairing_code", "123456");
            String myChildId = prefs.getString("selected_child_id", "");
            String myDevice = Build.MANUFACTURER + " " + Build.MODEL;

            String[] servers = new String[] {
                mService.mServerBaseUrl,
                "https://segurity.appzplay.online",
                "http://192.168.1.124:3080"
            };

            for (String serverUrl : servers) {
                if (serverUrl == null || serverUrl.isEmpty()) continue;
                try {
                    String reqUrl = serverUrl + "/api/data?code=" + pairingCode;
                    if (!myChildId.isEmpty()) {
                        reqUrl += "&childId=" + myChildId;
                    }
                    try {
                        reqUrl += "&device=" + java.net.URLEncoder.encode(myDevice, "UTF-8");
                    } catch (Exception ignored) {}
                    URL url = new URL(reqUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(1800);
                    conn.setReadTimeout(1800);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        String json = sb.toString();

                        // Parseo estructurado con org.json para máxima precisión
                        try {
                            org.json.JSONObject rootObj = new org.json.JSONObject(json);
                            org.json.JSONArray bpArray = rootObj.optJSONArray("blockedPackages");
                            if (bpArray != null) {
                                sBlockedPackages.clear();
                                for (int i = 0; i < bpArray.length(); i++) {
                                    String p = bpArray.optString(i, "").trim();
                                    if (!p.isEmpty()) sBlockedPackages.add(p);
                                }
                                // Verificar inmediatamente si la app que el niño tiene abierta en este segundo acaba de ser bloqueada
                                if (mService != null && mService.mHandler != null) {
                                    mService.mHandler.post(new CheckForegroundRunnable(mService));
                                }
                            }

                            org.json.JSONArray bdArray = rootObj.optJSONArray("blockedDomains");
                            if (bdArray != null) {
                                sBlockedDomains.clear();
                                for (int i = 0; i < bdArray.length(); i++) {
                                    String d = bdArray.optString(i, "").trim().toLowerCase();
                                    if (!d.isEmpty()) sBlockedDomains.add(d);
                                }
                            }

                            if (rootObj.has("adultBlocked")) {
                                sAdultBlocked = rootObj.optBoolean("adultBlocked", true);
                            }

                            org.json.JSONArray bkArray = rootObj.optJSONArray("blockedKeywords");
                            if (bkArray != null) {
                                sBlockedKeywords.clear();
                                for (int i = 0; i < bkArray.length(); i++) {
                                    String k = bkArray.optString(i, "").trim().toLowerCase();
                                    if (!k.isEmpty()) sBlockedKeywords.add(k);
                                }
                            }
                        } catch (Exception parseEx) {}

                        conn.disconnect();
                        break;
                    }
                    conn.disconnect();
                } catch (Exception e) {}
            }
        }
    }

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
                Toast.makeText(mService, mMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class ReportWebBlockThread extends Thread {
        private final String mServerBaseUrl;
        private final String mBlockedItem;
        private final String mReason;
        private final String mCode;

        public ReportWebBlockThread(String serverBaseUrl, String blockedItem, String reason, String code) {
            mServerBaseUrl = serverBaseUrl;
            mBlockedItem = blockedItem;
            mReason = reason;
            mCode = code != null && !code.isEmpty() ? code : "123456";
        }

        @Override
        public void run() {
            String[] servers = new String[] {
                mServerBaseUrl,
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
    public void onAccessibilityEvent(AccessibilityEvent event) {
        checkAndReportReboot();
        long now = System.currentTimeMillis();
        if (now - mLastServiceCheckTime > 15000) {
            mLastServiceCheckTime = now;
            ensureLockServiceRunning();
        }

        if (event == null || event.getPackageName() == null) return;
        final String pkg = event.getPackageName().toString();

        // 1. Pausa Instantánea Activa (Bloqueo total del dispositivo por los padres)
        if (LockOverlayService.isCurrentlyLocked()) {
            if (!pkg.equals(getPackageName())) {
                // Si el niño intenta bajar la barra de notificaciones para apagar Wi-Fi o entrar a ajustes:
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

                try {
                    Intent lockActIntent = new Intent(this, LockActivity.class);
                    lockActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(lockActIntent);
                } catch (Exception ignored) {}

                Intent lockIntent = new Intent(this, LockOverlayService.class);
                lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
                startService(lockIntent);
            }
            return;
        }

        // Si es nuestra propia app, ignorar
        if (pkg.equals(getPackageName())) {
            return;
        }

        // Protección contra desinstalación o detención en Ajustes del teléfono
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

        if (pkg.equals("com.android.systemui")
                || pkg.contains("launcher")
                || pkg.contains("trebuchet")) {
            return;
        }

        // 2. Si la aplicación específica (ej. YouTube, TikTok, juegos) está BLOQUEADA
        if (handleBlockedForegroundApp(pkg)) {
            return;
        }

        // 3. Inspección directa del texto del evento (captura lo que el niño escribe en Chrome/buscador)
        List<CharSequence> texts = event.getText();
        if (texts != null) {
            for (CharSequence cs : texts) {
                if (cs != null && cs.length() > 0) {
                    String matched = testStringAgainstBlockRules(cs.toString().toLowerCase().trim());
                    if (matched != null) {
                        blockWebImmediately(matched);
                        return;
                    }
                }
            }
        }

        CharSequence contentDesc = event.getContentDescription();
        if (contentDesc != null && contentDesc.length() > 0) {
            String matched = testStringAgainstBlockRules(contentDesc.toString().toLowerCase().trim());
            if (matched != null) {
                blockWebImmediately(matched);
                return;
            }
        }

        // 4. Inspección profunda de navegadores (Chrome, Samsung Internet, Firefox, Edge, etc.)
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
        if (now - mLastBlockTime < 800) return;

        // Revisar source del evento
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            try {
                String matched = scanNodeForBlockedText(source, 0);
                if (matched != null) {
                    blockWebImmediately(matched);
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
                // Revisar campo de URL de Chrome específicamente
                List<AccessibilityNodeInfo> urlBars = root.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar");
                if (urlBars != null) {
                    for (AccessibilityNodeInfo bar : urlBars) {
                        if (bar.getText() != null) {
                            String matched = testStringAgainstBlockRules(bar.getText().toString().toLowerCase().trim());
                            if (matched != null) {
                                bar.recycle();
                                blockWebImmediately(matched);
                                return;
                            }
                        }
                        bar.recycle();
                    }
                }

                String matched = scanNodeForBlockedText(root, 0);
                if (matched != null) {
                    blockWebImmediately(matched);
                }
            } finally {
                root.recycle();
            }
        }
    }

    private void blockWebImmediately(String matched) {
        mLastBlockTime = System.currentTimeMillis();

        // 1. Expulsar de inmediato al HOME
        performGlobalAction(GLOBAL_ACTION_HOME);

        // 2. Desplegar pantalla de bloqueo obligatoria
        Intent lockIntent = new Intent(this, LockOverlayService.class);
        lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
        lockIntent.putExtra("blocked_reason", "🚫 Contenido para adultos o sitio restringido bloqueado");
        startService(lockIntent);

        // 3. Lanzar también LockActivity fullscreen
        try {
            Intent lockActIntent = new Intent(this, LockActivity.class);
            lockActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            lockActIntent.putExtra("blocked_reason", "🚫 Sitio Web Bloqueado: Contenido no permitido");
            startActivity(lockActIntent);
        } catch (Exception ignored) {}

        if (mHandler != null) {
            mHandler.post(new ToastRunnable(this, "🚫 AppzSegurity: Acceso denegado a " + matched));
        }

        // 4. Reportar alerta en tiempo real al panel de padres
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

        // 1. Revisar reglas específicas del panel (ej: xvideos.com, omegle.com)
        for (String domain : sBlockedDomains) {
            if (text.contains(domain)) {
                return domain;
            }
        }

        // 2. Revisar filtro de pornografía / adultos
        if (sAdultBlocked) {
            for (String kw : DEFAULT_ADULT_KEYWORDS) {
                if (text.contains(kw)) {
                    return kw;
                }
            }
        }

        // 3. Revisar palabras clave adicionales configuradas en el servidor
        for (String kw : sBlockedKeywords) {
            if (text.contains(kw)) {
                return kw;
            }
        }

        return null;
    }

    @Override
    public void onInterrupt() {
    }

    public void checkCurrentForegroundApp() {
        try {
            // 1. Detección por nodo raíz de la ventana activa en primer plano
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                try {
                    CharSequence pkgSeq = root.getPackageName();
                    if (pkgSeq != null && handleBlockedForegroundApp(pkgSeq.toString())) {
                        return;
                    }
                } finally {
                    try { root.recycle(); } catch (Exception ignored) {}
                }
            }

            // 2. Detección por ventanas en pantalla (juegos fullscreen, SurfaceViews, videos)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    List<android.view.accessibility.AccessibilityWindowInfo> windows = getWindows();
                    if (windows != null) {
                        for (android.view.accessibility.AccessibilityWindowInfo w : windows) {
                            if (w != null && (w.isFocused() || w.getType() == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION)) {
                                AccessibilityNodeInfo winRoot = w.getRoot();
                                if (winRoot != null) {
                                    try {
                                        CharSequence wp = winRoot.getPackageName();
                                        if (wp != null && handleBlockedForegroundApp(wp.toString())) {
                                            return;
                                        }
                                    } finally {
                                        try { winRoot.recycle(); } catch (Exception ignored) {}
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public boolean handleBlockedForegroundApp(String currentPkg) {
        if (currentPkg == null || currentPkg.isEmpty()) return false;
        if (currentPkg.equals(getPackageName())
                || currentPkg.equals("com.android.systemui")
                || currentPkg.contains("launcher")
                || currentPkg.contains("trebuchet")) {
            return false;
        }

        if (isPackageBlocked(currentPkg)) {
            // 1. Expulsar de inmediato al HOME
            performGlobalAction(GLOBAL_ACTION_HOME);

            // 2. Lanzar LockActivity al frente para cubrir completamente la app bloqueada
            try {
                Intent lockActIntent = new Intent(this, LockActivity.class);
                lockActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                lockActIntent.putExtra("blocked_reason", "🚫 Aplicación Bloqueada por Control Parental");
                startActivity(lockActIntent);
            } catch (Exception ignored) {}

            // 3. Desplegar overlay
            Intent lockIntent = new Intent(this, LockOverlayService.class);
            lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
            lockIntent.putExtra("blocked_reason", "🚫 Aplicación Bloqueada por Control Parental");
            startService(lockIntent);

            if (mHandler != null) {
                mHandler.post(new ToastRunnable(this, "🚫 Esta aplicación ha sido bloqueada por tus padres en tiempo real."));
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        if (mHandler != null && mSyncRunnable != null) {
            mHandler.removeCallbacks(mSyncRunnable);
        }
    }
}
