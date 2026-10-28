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
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {

            // 1. Reanudar inmediatamente el servicio de bloqueo en primer plano
            Intent serviceIntent = new Intent(context, LockOverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // 2. Notificar inmediatamente al servidor que el dispositivo fue reiniciado
            new Thread(new SendRebootAuditTask(context)).start();
        }
    }

    private static class SendRebootAuditTask implements Runnable {
        private final Context mContext;

        public SendRebootAuditTask(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            try {
                // Esperar 3 segundos para que la conexión de red (Wi-Fi/Datos) esté disponible tras el arranque
                Thread.sleep(3000);

                URL url = new URL("https://segurity.appzplay.online/api/child/reboot");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                String jsonPayload = "{\"deviceName\":\"" + Build.MANUFACTURER + " " + Build.MODEL + "\",\"os\":\"Android " + Build.VERSION.RELEASE + "\"}";

                OutputStream os = conn.getOutputStream();
                os.write(jsonPayload.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
