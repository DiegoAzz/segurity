package com.norton.parental.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Receptor de Reinicio del Sistema (Anti-Bypass por Apagado / Reinicio).
 *
 * Si el dispositivo estaba bloqueado antes de reiniciar:
 * 1. Lanza inmediatamente LockActivity al frente para bloquear la pantalla antes del launcher.
 * 2. Reanuda LockOverlayService en primer plano de forma segura.
 * 3. Reporta el evento de reinicio al servidor para auditoría parental.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            // 1. Guardar flag persistente de reinicio detectado
            try {
                SharedPreferences prefs = context.getSharedPreferences("norton_prefs", Context.MODE_PRIVATE);
                prefs.edit().putBoolean("just_rebooted", true).apply();
            } catch (Throwable ignored) {}

            // 2. ANTI-BYPASS: Si el dispositivo estaba bloqueado antes del reinicio,
            // forzar inmediatamente LockActivity al frente sin esperar a que cargue el launcher.
            try {
                SharedPreferences cache = context.getSharedPreferences("norton_policy_cache", Context.MODE_PRIVATE);
                boolean wasLocked = cache.getBoolean("instant_lock", false);
                if (wasLocked) {
                    Intent lockIntent = new Intent(context, LockActivity.class);
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    lockIntent.putExtra("blocked_reason", "🚨 Dispositivo Bloqueado por Control Parental");
                    context.startActivity(lockIntent);
                }
            } catch (Throwable ignored) {}

            // 3. Reanudar el servicio de supervisión en primer plano con manejo seguro de excepciones Android 14+
            try {
                Intent serviceIntent = new Intent(context, LockOverlayService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Throwable ignored) {}

            // 4. Notificar al servidor en hilo secundario con reintentos para dar tiempo a conexión de red
            try {
                new Thread(new SendRebootAuditTask(context)).start();
            } catch (Throwable ignored) {}
        }
    }

    public static class SendRebootAuditTask implements Runnable {
        private final Context mContext;

        public SendRebootAuditTask(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            String[] servers = new String[] {
                "https://segurity.appzplay.online",
                "http://192.168.1.124:3080"
            };

            for (int attempt = 0; attempt < 6; attempt++) {
                try {
                    Thread.sleep(attempt == 0 ? 3000 : 5000);
                    boolean sent = false;

                    for (String srv : servers) {
                        try {
                            URL url = new URL(srv + "/api/child/reboot");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                            conn.setDoOutput(true);
                            conn.setConnectTimeout(4000);
                            conn.setReadTimeout(4000);

                            String jsonPayload = "{\"deviceName\":\"" + Build.MANUFACTURER + " " + Build.MODEL + "\",\"os\":\"Android " + Build.VERSION.RELEASE + "\"}";

                            OutputStream os = conn.getOutputStream();
                            os.write(jsonPayload.getBytes("UTF-8"));
                            os.flush();
                            os.close();

                            int responseCode = conn.getResponseCode();
                            conn.disconnect();

                            if (responseCode == 200) {
                                sent = true;
                                SharedPreferences prefs = mContext.getSharedPreferences("norton_prefs", Context.MODE_PRIVATE);
                                prefs.edit().putBoolean("just_rebooted", false).apply();
                                break;
                            }
                        } catch (Exception ignored) {}
                    }

                    if (sent) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }
    }
}
