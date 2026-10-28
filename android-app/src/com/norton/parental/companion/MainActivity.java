package com.norton.parental.companion;

import android.app.Activity;
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

        Button btnTest = findViewById(R.id.btn_test_lock);
        btnTest.setOnClickListener(this);

        startCompanionService();
        scanAndUploadInstalledApps();
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

                URL url = new URL(mServerUrl + "/api/child/installed-apps");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();

                int resCode = conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_overlay_perm) {
            requestOverlayPermission();
        } else if (id == R.id.btn_accessibility_perm) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Busca 'Norton Companion' y activa el servicio", Toast.LENGTH_LONG).show();
        } else if (id == R.id.btn_test_lock) {
            if (checkOverlayPermission()) {
                Intent lockIntent = new Intent(this, LockOverlayService.class);
                lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
                startService(lockIntent);
                Toast.makeText(this, "Bloqueo activado. Abre YouTube para comprobar que no te deja entrar.", Toast.LENGTH_LONG).show();
            } else {
                requestOverlayPermission();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        scanAndUploadInstalledApps();
    }

    private void updateStatus() {
        boolean overlay = checkOverlayPermission();
        if (overlay) {
            tvStatus.setText("● Permiso de Superposición Concedido (Bloqueo Total Activo)");
            tvStatus.setTextColor(0xFF10B981);
        } else {
            tvStatus.setText("⚠️ Falta Permiso de Superposición de Pantalla");
            tvStatus.setTextColor(0xFFF59E0B);
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_CODE_OVERLAY);
            Toast.makeText(this, "Permite que Norton Companion se muestre sobre otras apps", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "El permiso de superposición ya está activo", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCompanionService() {
        Intent serviceIntent = new Intent(this, LockOverlayService.class);
        serviceIntent.putExtra("server_url", etServerUrl.getText().toString().trim());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
