package com.zaros.app.backend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zaros.app.ui.SplashActivity;

/**
 * ZarOS BootReceiver
 * Fires after MOTO boot animation completes.
 * Launches ZarOS splash immediately so it appears on top of the system lock screen.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Intent splash = new Intent(context, SplashActivity.class);
            splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                           Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(splash);
        }
    }
}
