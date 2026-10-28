package com.norton.parental.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            // 1. Guardar flag persistente de reinicio detectado
            SharedPreferences prefs = context.getSharedPreferences("norton_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("just_rebooted", true).apply();

            // 2. Reanudar inmediatamente el servicio de bloqueo en primer plano
            Intent serviceIntent = new Intent(context, LockOverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // 3. Notificar al servidor en hilo con reintentos para dar tiempo a que Wi-Fi o Datos conecten
            new Thread(new SendRebootAuditTask(context)).start();
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

            // Intentar hasta 6 veces esperando que la antena de red esté activa
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
