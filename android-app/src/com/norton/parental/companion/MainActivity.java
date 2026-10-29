package com.norton.parental.companion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int REQ_CODE_OVERLAY = 1001;
    private static final int REQ_CODE_LOCATION = 1002;
    private TextView tvStatus;
    public EditText etServerUrl;
    public EditText etPairingCode;
    public TextView tvChildAssigned;
    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());

        tvStatus = findViewById(R.id.tv_status);
        etServerUrl = findViewById(R.id.et_server_url);
        etPairingCode = findViewById(R.id.et_pairing_code);
        tvChildAssigned = findViewById(R.id.tv_child_assigned);

        SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        String savedCode = prefs.getString("pairing_code", "123456");
        String savedUrl = prefs.getString("server_url", "https://segurity.appzplay.online");

        if (etPairingCode != null) etPairingCode.setText(savedCode);
        if (etServerUrl != null) etServerUrl.setText(savedUrl);

        updateChildAssignedLabel();

        Button btnSaveCode = findViewById(R.id.btn_save_pairing_code);
        if (btnSaveCode != null) btnSaveCode.setOnClickListener(this);

        Button btnChangeChild = findViewById(R.id.btn_change_child);
        if (btnChangeChild != null) btnChangeChild.setOnClickListener(this);

        Button btnOverlay = findViewById(R.id.btn_overlay_perm);
        if (btnOverlay != null) btnOverlay.setOnClickListener(this);

        Button btnAccessibility = findViewById(R.id.btn_accessibility_perm);
        if (btnAccessibility != null) btnAccessibility.setOnClickListener(this);

        Button btnLocation = findViewById(R.id.btn_location_perm);
        if (btnLocation != null) btnLocation.setOnClickListener(this);

        Button btnAutostart = findViewById(R.id.btn_autostart_perm);
        if (btnAutostart != null) btnAutostart.setOnClickListener(this);

        Button btnDeviceAdmin = findViewById(R.id.btn_device_admin);
        if (btnDeviceAdmin != null) btnDeviceAdmin.setOnClickListener(this);

        Button btnTest = findViewById(R.id.btn_test_lock);
        if (btnTest != null) btnTest.setOnClickListener(this);

        requestLocationPermissionIfNeeded();
        startCompanionService();
        scanAndUploadInstalledApps();
    }

    public void updateChildAssignedLabel() {
        if (tvChildAssigned == null) return;
        SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
        String name = prefs.getString("selected_child_name", "Lucas");
        String avatar = prefs.getString("selected_child_avatar", "👦");
        tvChildAssigned.setText(avatar + " Asignado a: " + name + " (Este celular pertenece a este hijo/a)");
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
        String code = etPairingCode != null ? etPairingCode.getText().toString().trim() : "123456";
        new ScanAppsThread(getPackageManager(), serverUrl, code).start();
    }

    public static class ScanAppsThread extends Thread {
        private final PackageManager mPm;
        private final String mServerUrl;
        private final String mCode;

        public ScanAppsThread(PackageManager pm, String serverUrl, String code) {
            mPm = pm;
            mServerUrl = serverUrl;
            mCode = code != null && !code.isEmpty() ? code : "123456";
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

                    String payload = "{\"code\":\"" + mCode + "\",\"apps\":" + jsonArray.toString() + "}";

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
                } catch (Exception ignored) {}
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

        if (id == R.id.btn_save_pairing_code) {
            String code = etPairingCode.getText().toString().trim();
            String srvUrl = etServerUrl.getText().toString().trim();
            if (code.isEmpty()) code = "123456";

            SharedPreferences prefs = getSharedPreferences("norton_prefs", MODE_PRIVATE);
            prefs.edit()
                 .putString("pairing_code", code)
                 .putString("server_url", srvUrl)
                 .apply();

            Toast.makeText(this, "✅ Código " + code + " guardado. Verificando hijos...", Toast.LENGTH_SHORT).show();
            scanAndUploadInstalledApps();
            fetchFamilyChildren(code, srvUrl);

        } else if (id == R.id.btn_change_child) {
            String code = etPairingCode.getText().toString().trim();
            String srvUrl = etServerUrl.getText().toString().trim();
            if (code.isEmpty()) code = "123456";
            fetchFamilyChildren(code, srvUrl);

        } else if (id == R.id.btn_overlay_perm) {
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
            Toast.makeText(this, "Busca 'AppzSegurity' y actívalo", Toast.LENGTH_LONG).show();

        } else if (id == R.id.btn_location_perm) {
            requestLocationPermissionIfNeeded();

        } else if (id == R.id.btn_autostart_perm) {
            openAutostartSettings();

        } else if (id == R.id.btn_device_admin) {
            android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(this, NortonDeviceAdminReceiver.class);
            if (!dpm.isAdminActive(adminComponent)) {
                Intent intent = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protege AppzSegurity contra desinstalación.");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Protección contra desinstalación ya activa", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.btn_test_lock) {
            // Iniciar vista previa de bloqueo por 10 segundos
            Intent intent = new Intent(this, LockActivity.class);
            intent.putExtra("is_test", true);
            intent.putExtra("blocked_reason", "Vista Previa de Bloqueo Instantáneo");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public void fetchFamilyChildren(final String code, final String srvUrl) {
        new FetchChildrenThread(this, code, srvUrl).start();
    }

    public static class FetchChildrenThread extends Thread {
        private final MainActivity mActivity;
        private final String mCode;
        private final String mSrvUrl;

        public FetchChildrenThread(MainActivity activity, String code, String srvUrl) {
            mActivity = activity;
            mCode = code;
            mSrvUrl = srvUrl;
        }

        @Override
        public void run() {
            String[] servers = new String[] {
                mSrvUrl,
                "https://segurity.appzplay.online",
                "http://192.168.1.124:3080"
            };

            for (String srv : servers) {
                if (srv == null || srv.isEmpty()) continue;
                try {
                    URL url = new URL(srv + "/api/family/children?code=" + mCode);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        conn.disconnect();

                        JSONObject root = new JSONObject(sb.toString());
                        JSONArray children = root.optJSONArray("children");

                        if (children != null && children.length() > 0 && mActivity != null && mActivity.mHandler != null) {
                            mActivity.mHandler.post(new ShowDialogRunnable(mActivity, children));
                            return;
                        }
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }
    }

    public static class ShowDialogRunnable implements Runnable {
        private final MainActivity mActivity;
        private final JSONArray mChildren;

        public ShowDialogRunnable(MainActivity activity, JSONArray children) {
            mActivity = activity;
            mChildren = children;
        }

        @Override
        public void run() {
            if (mActivity != null) {
                mActivity.showSelectChildDialog(mChildren);
            }
        }
    }

    public static class ChildDialogClickListener implements DialogInterface.OnClickListener {
        private final MainActivity mActivity;
        private final List<String> mIds;
        private final List<String> mNames;
        private final List<String> mAvatars;

        public ChildDialogClickListener(MainActivity activity, List<String> ids, List<String> names, List<String> avatars) {
            mActivity = activity;
            mIds = ids;
            mNames = names;
            mAvatars = avatars;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mActivity == null) return;
            String selectedId = mIds.get(which);
            String selectedName = mNames.get(which);
            String selectedAvatar = mAvatars.get(which);

            SharedPreferences prefs = mActivity.getSharedPreferences("norton_prefs", MODE_PRIVATE);
            prefs.edit()
                 .putString("selected_child_id", selectedId)
                 .putString("selected_child_name", selectedName)
                 .putString("selected_child_avatar", selectedAvatar)
                 .apply();

            mActivity.updateChildAssignedLabel();
            Toast.makeText(mActivity, "✅ Celular asignado a: " + selectedAvatar + " " + selectedName, Toast.LENGTH_LONG).show();
            mActivity.startCompanionService();
        }
    }

    private void showSelectChildDialog(final JSONArray children) {
        try {
            final List<String> childNames = new ArrayList<String>();
            final List<String> childIds = new ArrayList<String>();
            final List<String> childAvatars = new ArrayList<String>();

            for (int i = 0; i < children.length(); i++) {
                JSONObject c = children.optJSONObject(i);
                if (c != null) {
                    String id = c.optString("id", "child_" + i);
                    String name = c.optString("name", "Hijo");
                    int age = c.optInt("age", 10);
                    String avatar = c.optString("avatar", "👦");

                    childIds.add(id);
                    childNames.add(name);
                    childAvatars.add(avatar);
                }
            }

            CharSequence[] items = new CharSequence[childNames.size()];
            for (int i = 0; i < childNames.size(); i++) {
                items[i] = childAvatars.get(i) + "  " + childNames.get(i);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("¿A cuál hijo/a pertenece este celular?");
            builder.setItems(items, new ChildDialogClickListener(this, childIds, childNames, childAvatars));
            builder.setNegativeButton("Cancelar", null);
            builder.show();
        } catch (Exception ignored) {}
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
