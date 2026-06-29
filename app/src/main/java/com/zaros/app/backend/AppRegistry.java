package com.zaros.app.backend;

import android.graphics.Color;
import com.zaros.app.R;
import java.util.ArrayList;
import java.util.List;

/**
 * ZarOS AppRegistry — defines home grid and dock apps.
 * Each app references a glossy gradient background drawable
 * for the iOS-style icon look (ZarOS 4.0.0).
 */
public class AppRegistry {

    public static List<AppInfo> getHomeApps() {
        List<AppInfo> apps = new ArrayList<>();
        apps.add(new AppInfo("YT Music",  "com.google.android.apps.youtube.music", R.drawable.ic_app_ytmusic,  Color.parseColor("#CC0000"), Color.parseColor("#FF4444"), R.drawable.bg_glossy_red));
        apps.add(new AppInfo("App Store", "com.zaros.app.store",                   R.drawable.ic_app_store,    Color.parseColor("#007AFF"), Color.parseColor("#34AADC"), R.drawable.bg_glossy_blue));
        apps.add(new AppInfo("Settings",  "com.zaros.app.settings",                R.drawable.ic_app_settings, Color.parseColor("#3A3A4A"), Color.parseColor("#555568"), R.drawable.bg_glossy_grey));
        apps.add(new AppInfo("Fynder",    "app.fynder.browser",                    R.drawable.ic_app_fynder,   Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"), R.drawable.bg_glossy_white));
        apps.add(new AppInfo("Phone",     "com.zaros.app.dialer",                  R.drawable.ic_app_phone,    Color.parseColor("#22C55E"), Color.parseColor("#15803D"), R.drawable.bg_glossy_green));
        apps.add(new AppInfo("Messages",  "com.zaros.app.messages",                R.drawable.ic_app_messages, Color.parseColor("#0EA5E9"), Color.parseColor("#0369A1"), R.drawable.bg_glossy_blue));
        apps.add(new AppInfo("Clock",     "com.zaros.app.clock",                   R.drawable.ic_app_clock,    Color.parseColor("#6366F1"), Color.parseColor("#4338CA"), R.drawable.bg_glossy_indigo));
        apps.add(new AppInfo("Camera",    "android.media.action.IMAGE_CAPTURE",    R.drawable.ic_app_camera,   Color.parseColor("#1C2A4A"), Color.parseColor("#0F172A"), R.drawable.bg_glossy_grey));
        apps.add(new AppInfo("Files",     "com.google.android.apps.nbu.files",     R.drawable.ic_app_files,    Color.parseColor("#0EA5E9"), Color.parseColor("#0369A1"), R.drawable.bg_glossy_blue));
        apps.add(new AppInfo("YouTube",   "com.google.android.youtube",            R.drawable.ic_app_youtube,  Color.parseColor("#FF0000"), Color.parseColor("#CC0000"), R.drawable.bg_glossy_red));
        apps.add(new AppInfo("Home App",  "com.zaros.app.homeswitcher",            R.drawable.ic_app_home_switcher, Color.parseColor("#0EA5E9"), Color.parseColor("#0369A1"), R.drawable.bg_glossy_blue));
        return apps;
    }

    public static List<AppInfo> getDockApps() {
        List<AppInfo> dock = new ArrayList<>();
        dock.add(new AppInfo("Phone",    "com.zaros.app.dialer",   R.drawable.ic_app_phone,    Color.parseColor("#22C55E"), Color.parseColor("#15803D"), R.drawable.bg_glossy_green));
        dock.add(new AppInfo("Fynder",   "app.fynder.browser",     R.drawable.ic_app_fynder,   Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"), R.drawable.bg_glossy_white));
        dock.add(new AppInfo("Messages", "com.zaros.app.messages", R.drawable.ic_app_messages, Color.parseColor("#0EA5E9"), Color.parseColor("#0369A1"), R.drawable.bg_glossy_blue));
        dock.add(new AppInfo("Settings", "com.zaros.app.settings", R.drawable.ic_app_settings, Color.parseColor("#3A3A4A"), Color.parseColor("#555568"), R.drawable.bg_glossy_grey));
        return dock;
    }
}
