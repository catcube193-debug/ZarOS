package com.zaros.app.backend;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ZarOS AppDrawerManager (Java backend)
 * Discovers all user-launchable apps installed on the device.
 * Returns them sorted alphabetically, excluding ZarOS itself.
 */
public class AppDrawerManager {

    public static class DrawerApp {
        public final String label;
        public final String packageName;
        public final Drawable icon;

        public DrawerApp(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    /**
     * Returns all launchable apps sorted A→Z.
     * Call from a background thread — can be slow on first run.
     */
    public static List<DrawerApp> getAllApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
        List<DrawerApp> apps = new ArrayList<>();

        for (ResolveInfo info : resolved) {
            String pkg = info.activityInfo.packageName;
            // Skip ZarOS itself
            if (pkg.equals("com.zaros.app")) continue;

            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            apps.add(new DrawerApp(label, pkg, icon));
        }

        // Sort A→Z
        Collections.sort(apps, (a, b) ->
                a.label.compareToIgnoreCase(b.label));

        return apps;
    }
}
