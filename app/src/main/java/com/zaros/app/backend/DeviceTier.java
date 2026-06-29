package com.zaros.app.backend;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

/**
 * ZarOS DeviceTier (Java backend)
 *
 * Lightweight heuristic for whether the current device is "low-end" —
 * used to pick safer defaults (e.g. animated wallpaper off) on devices
 * like the Galaxy A11 (2020 budget phone, 2-3GB RAM) where continuous
 * custom-View redraw loops visibly stutter.
 *
 * This is intentionally simple: total RAM and API level are cheap to
 * read, don't require any permissions, and are reliable signals for
 * "this device will struggle with extra animation" without needing a
 * full GPU/benchmark profiling pass.
 */
public class DeviceTier {

    // Devices reporting at or below this much total RAM are treated as
    // low-end. The Galaxy A11 ships with 2GB on most variants (3GB on
    // some), so 3GB is a safe cutoff that catches it and similar budget
    // hardware from the same era without flagging mid-range+ devices.
    private static final long LOW_RAM_THRESHOLD_MB = 3072L;

    public static boolean isLowEndDevice(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;

            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            long totalRamMb = info.totalMem / (1024L * 1024L);

            // ActivityManager also exposes an explicit low-RAM flag on
            // many OEM builds (commonly true on budget devices) — trust
            // it if present, otherwise fall back to the RAM threshold.
            boolean reportedLowRam = am.isLowRamDevice();

            return reportedLowRam || totalRamMb <= LOW_RAM_THRESHOLD_MB;
        } catch (Exception e) {
            // If anything goes wrong reading device info, don't assume
            // low-end — keep the richer default experience rather than
            // silently disabling animation on a perfectly capable device.
            return false;
        }
    }
}
