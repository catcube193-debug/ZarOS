package com.zaros.app.backend;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

public class AppLauncher {

    public static void launch(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        switch (packageName) {
            case "app.fynder.browser":      launchInternal(context, "com.zaros.app.ui.FynderActivity");    return;
            case "com.zaros.app.settings":  launchInternal(context, "com.zaros.app.ui.SettingsActivity");  return;
            case "com.zaros.app.dialer":    launchInternal(context, "com.zaros.app.ui.DialerActivity");    return;
            case "com.zaros.app.messages":  launchInternal(context, "com.zaros.app.ui.MessagesActivity");  return;
            case "com.zaros.app.clock":     launchInternal(context, "com.zaros.app.ui.ClockActivity");     return;
            case "com.zaros.app.store":     launchInternal(context, "com.zaros.app.ui.AppStoreActivity");  return;
            case "com.zaros.app.homeswitcher": launchInternal(context, "com.zaros.app.ui.HomeSwitcherActivity"); return;
        }
        if (packageName.equals("android.media.action.IMAGE_CAPTURE")) {
            Intent intent = new Intent(packageName);
            if (intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(intent);
            else Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show();
            return;
        }
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            try {
                Intent store = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                store.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(store);
            } catch (Exception e) {
                Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static void launchInternal(Context context, String className) {
        try {
            Class<?> cls = Class.forName(className);
            Intent intent = new Intent(context, cls);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isInstalled(Context context, String packageName) {
        try { context.getPackageManager().getPackageInfo(packageName, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }
}
