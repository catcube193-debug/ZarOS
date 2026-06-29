package com.zaros.app.backend;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * ZarOS ZarOSPrefs (Java backend)
 * Single source of truth for all user-configurable settings.
 * Stored in SharedPreferences; read by both Java and Kotlin layers.
 */
public class ZarOSPrefs {

    private static final String PREFS_NAME        = "zaros_settings";

    // Keys
    public static final String KEY_WALLPAPER      = "wallpaper_index";
    public static final String KEY_DARK_MODE      = "dark_mode";
    public static final String KEY_BRIGHTNESS     = "brightness";
    public static final String KEY_VOLUME         = "volume";
    public static final String KEY_VIBRATION      = "vibration";
    public static final String KEY_NOTIFICATIONS  = "notifications";
    public static final String KEY_WIFI           = "wifi";
    public static final String KEY_BLUETOOTH      = "bluetooth";
    public static final String KEY_24H_CLOCK      = "clock_24h";
    public static final String KEY_DEFAULT_BROWSER= "default_browser";
    public static final String KEY_ANIMATED_WALLPAPER = "animated_wallpaper";

    // Defaults
    private static final int    DEFAULT_WALLPAPER     = 0;
    private static final boolean DEFAULT_DARK_MODE    = true;
    private static final int    DEFAULT_BRIGHTNESS    = 80;
    private static final int    DEFAULT_VOLUME        = 65;
    private static final boolean DEFAULT_VIBRATION    = true;
    private static final boolean DEFAULT_NOTIFS       = true;
    private static final boolean DEFAULT_WIFI         = true;
    private static final boolean DEFAULT_BT           = false;
    private static final boolean DEFAULT_24H          = false;
    private static final String  DEFAULT_BROWSER      = "Fynder";
    private static final boolean DEFAULT_ANIMATED_WALL = true;

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public static int  getWallpaper(Context ctx)    { return prefs(ctx).getInt(KEY_WALLPAPER, DEFAULT_WALLPAPER); }
    public static boolean isDarkMode(Context ctx)   { return prefs(ctx).getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE); }
    public static int  getBrightness(Context ctx)   { return prefs(ctx).getInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS); }
    public static int  getVolume(Context ctx)       { return prefs(ctx).getInt(KEY_VOLUME, DEFAULT_VOLUME); }
    public static boolean isVibration(Context ctx)  { return prefs(ctx).getBoolean(KEY_VIBRATION, DEFAULT_VIBRATION); }
    public static boolean isNotifications(Context ctx){ return prefs(ctx).getBoolean(KEY_NOTIFICATIONS, DEFAULT_NOTIFS); }
    public static boolean isWifi(Context ctx)       { return prefs(ctx).getBoolean(KEY_WIFI, DEFAULT_WIFI); }
    public static boolean isBluetooth(Context ctx)  { return prefs(ctx).getBoolean(KEY_BLUETOOTH, DEFAULT_BT); }
    public static boolean is24HClock(Context ctx)   { return prefs(ctx).getBoolean(KEY_24H_CLOCK, DEFAULT_24H); }
    public static String  getDefaultBrowser(Context ctx){ return prefs(ctx).getString(KEY_DEFAULT_BROWSER, DEFAULT_BROWSER); }
    public static boolean isAnimatedWallpaper(Context ctx) {
        // If the user has never touched this setting, default it based on
        // device capability — e.g. the animated dual-gradient redraw loop
        // visibly stutters on budget/older hardware like a 2020 Galaxy A11.
        // Once a user explicitly sets the toggle (in either direction),
        // that choice is stored and always respected from then on —
        // this only affects what happens before they've ever touched it.
        if (!prefs(ctx).contains(KEY_ANIMATED_WALLPAPER)) {
            return !DeviceTier.isLowEndDevice(ctx);
        }
        return prefs(ctx).getBoolean(KEY_ANIMATED_WALLPAPER, DEFAULT_ANIMATED_WALL);
    }

    // ── Setters ──────────────────────────────────────────────────────────

    public static void setWallpaper(Context ctx, int idx)       { prefs(ctx).edit().putInt(KEY_WALLPAPER, idx).apply(); }
    public static void setDarkMode(Context ctx, boolean v)      { prefs(ctx).edit().putBoolean(KEY_DARK_MODE, v).apply(); }
    public static void setBrightness(Context ctx, int v)        { prefs(ctx).edit().putInt(KEY_BRIGHTNESS, v).apply(); }
    public static void setVolume(Context ctx, int v)            { prefs(ctx).edit().putInt(KEY_VOLUME, v).apply(); }
    public static void setVibration(Context ctx, boolean v)     { prefs(ctx).edit().putBoolean(KEY_VIBRATION, v).apply(); }
    public static void setNotifications(Context ctx, boolean v) { prefs(ctx).edit().putBoolean(KEY_NOTIFICATIONS, v).apply(); }
    public static void setWifi(Context ctx, boolean v)          { prefs(ctx).edit().putBoolean(KEY_WIFI, v).apply(); }
    public static void setBluetooth(Context ctx, boolean v)     { prefs(ctx).edit().putBoolean(KEY_BLUETOOTH, v).apply(); }
    public static void set24HClock(Context ctx, boolean v)      { prefs(ctx).edit().putBoolean(KEY_24H_CLOCK, v).apply(); }
    public static void setAnimatedWallpaper(Context ctx, boolean v) { prefs(ctx).edit().putBoolean(KEY_ANIMATED_WALLPAPER, v).apply(); }

    /** Wallpaper gradient strings indexed by wallpaper option number */
    public static final String[] WALLPAPER_STARTS = {
        "#FF38BDF8", "#FF7C3AED", "#FF059669",
        "#FFDC2626", "#FFEA580C", "#FF0F172A",
        "#FFBE185D", "#FF854D0E"
    };
    public static final String[] WALLPAPER_ENDS = {
        "#FF0C4A6E", "#FF1E1B4B", "#FF064E3B",
        "#FF7F1D1D", "#FF431407", "#FF334155",
        "#FF500724", "#FF1C0A00"
    };
}
