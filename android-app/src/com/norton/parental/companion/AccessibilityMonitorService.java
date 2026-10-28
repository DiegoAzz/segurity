package com.norton.parental.companion;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
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
import java.util.Set;

public class AccessibilityMonitorService extends AccessibilityService {

    private static final Set<String> sBlockedPackages = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> sBlockedDomains = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> sBlockedKeywords = Collections.synchronizedSet(new HashSet<String>());
    private static boolean sAdultBlocked = true;

    private static final String[] DEFAULT_ADULT_KEYWORDS = new String[] {
        "xvideos", "pornhub", "xnxx", "redtube", "youporn", "chaturbate", "stripchat",
        "onlyfans", "erome", "cam4", "brazzers", "xhamster", "spankbang", "hentai",
        "rule34", "beeg", "tube8", "eporner", "txxx", "porno", "xxx", "sexo", "desnuda"
    };

    public Handler mHandler;
    public String mServerBaseUrl = "https://segurity.appzplay.online";
    private SyncRunnable mSyncRunnable;
    private long mLastBlockTime = 0;

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

                        // 1. Bloqueo de aplicaciones
                        int idx = json.indexOf("\"blockedPackages\":[");
                        if (idx != -1) {
                            int endIdx = json.indexOf("]", idx);
                            if (endIdx != -1) {
                                String arrayStr = json.substring(idx + 19, endIdx);
                                String[] pkgs = arrayStr.replace("\"", "").split(",");
                                sBlockedPackages.clear();
                                for (String p : pkgs) {
                                    String clean = p.trim();
                                    if (!clean.isEmpty()) sBlockedPackages.add(clean);
                                }
                            }
                        }

                        // 2. Bloqueo de dominios específicos (ej: xvideos.com, omegle.com)
                        int dIdx = json.indexOf("\"blockedDomains\":[");
                        if (dIdx != -1) {
                            int endDIdx = json.indexOf("]", dIdx);
                            if (endDIdx != -1) {
                                String arrayStr = json.substring(dIdx + 18, endDIdx);
                                String[] domains = arrayStr.replace("\"", "").split(",");
                                sBlockedDomains.clear();
                                for (String d : domains) {
                                    String clean = d.trim().toLowerCase();
                                    if (!clean.isEmpty()) sBlockedDomains.add(clean);
                                }
                            }
                        }

                        // 3. Bloqueo de categorías y pornografía
                        int aIdx = json.indexOf("\"adultBlocked\":");
                        if (aIdx != -1) {
                            String sub = json.substring(aIdx + 15, Math.min(aIdx + 20, json.length()));
                            sAdultBlocked = sub.startsWith("true");
                        }

                        // 4. Palabras clave bloqueadas
                        int kIdx = json.indexOf("\"blockedKeywords\":[");
                        if (kIdx != -1) {
                            int endKIdx = json.indexOf("]", kIdx);
                            if (endKIdx != -1) {
                                String arrayStr = json.substring(kIdx + 19, endKIdx);
                                String[] kws = arrayStr.replace("\"", "").split(",");
                                sBlockedKeywords.clear();
                                for (String k : kws) {
                                    String clean = k.trim().toLowerCase();
                                    if (!clean.isEmpty()) sBlockedKeywords.add(clean);
                                }
                            }
                        }

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

        public ReportWebBlockThread(String serverBaseUrl, String blockedItem, String reason) {
            mServerBaseUrl = serverBaseUrl;
            mBlockedItem = blockedItem;
            mReason = reason;
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

                    String payload = "{\"item\":\"" + mBlockedItem + "\",\"reason\":\"" + mReason + "\"}";
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

        // 2. Si la aplicación específica (ej. YouTube, TikTok) está BLOQUEADA
        if (isPackageBlocked(pkg)) {
            performGlobalAction(GLOBAL_ACTION_HOME);

            Intent lockIntent = new Intent(this, LockOverlayService.class);
            lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
            lockIntent.putExtra("blocked_reason", "🚫 Aplicación Bloqueada por Control Parental");
            startService(lockIntent);

            if (mHandler != null) {
                mHandler.post(new ToastRunnable(this, "🚫 Esta aplicación está bloqueada por tus padres."));
            }
            return;
        }

        // 3. Inspección de Navegadores Web para bloqueo de URLs, Pornografía y Sitios Restringidos
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
            checkBrowserContentForBlockedWeb();
        }
    }

    private void checkBrowserContentForBlockedWeb() {
        long now = System.currentTimeMillis();
        // Evitar inspecciones duplicadas en menos de 400ms
        if (now - mLastBlockTime < 1000) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            String matched = scanNodeForBlockedText(root, 0);
            if (matched != null) {
                mLastBlockTime = now;
                // KICK AL HOME INMEDIATAMENTE
                performGlobalAction(GLOBAL_ACTION_HOME);

                // MOSTRAR OVERLAY DE BLOQUEO
                Intent lockIntent = new Intent(this, LockOverlayService.class);
                lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
                lockIntent.putExtra("blocked_reason", "🚫 Sitio Web Bloqueado: Contenido no permitido por tus padres");
                startService(lockIntent);

                if (mHandler != null) {
                    mHandler.post(new ToastRunnable(this, "🚫 Norton Parental: Acceso denegado a " + matched));
                }

                new ReportWebBlockThread(mServerBaseUrl, matched, "Filtro Web / Contenido para Adultos").start();
            }
        } finally {
            root.recycle();
        }
    }

    private String scanNodeForBlockedText(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 20) return null;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null && mSyncRunnable != null) {
            mHandler.removeCallbacks(mSyncRunnable);
        }
    }
}
