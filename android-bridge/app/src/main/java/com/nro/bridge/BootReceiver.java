package com.nro.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Tự restart bridge sau khi điện thoại reboot */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            String savedUrl = context.getSharedPreferences("nro", Context.MODE_PRIVATE)
                    .getString("ws_url", "");
            if (!savedUrl.isEmpty()) {
                Intent service = new Intent(context, BridgeService.class);
                service.putExtra("WS_URL", savedUrl);
                context.startForegroundService(service);
            }
        }
    }
}
