package com.zaros.app.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ZarOS FynderSearchEngine (Java backend)
 * Handles URL/query resolution and persists search history.
 */
public class FynderSearchEngine {

    private static final String PREFS_NAME    = "fynder_prefs";
    private static final String KEY_HISTORY   = "search_history";
    private static final int    MAX_HISTORY   = 20;

    // Default search base — uses Fynder's Netlify deployment
    private static final String FYNDER_BASE =
            "https://sparkling-banoffee-9ca11d.netlify.app/?q=";

    // Fallback to DuckDuckGo if Fynder unreachable
    private static final String DDG_BASE =
            "https://duckduckgo.com/?q=";

    /**
     * Resolve a user-typed string to a full URL.
     * - Already a URL  → returned as-is (with https:// if missing)
     * - Otherwise      → treated as a Fynder search query
     */
    public static String resolve(String input) {
        if (input == null || input.trim().isEmpty()) return FYNDER_BASE;

        input = input.trim();

        // Looks like a URL (has a dot and no spaces, or starts with http)
        if (isUrl(input)) {
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                return "https://" + input;
            }
            return input;
        }

        // Search query
        return FYNDER_BASE + Uri.encode(input);
    }

    /** Returns the Fynder home page URL */
    public static String homeUrl() {
        return "https://sparkling-banoffee-9ca11d.netlify.app/";
    }

    private static boolean isUrl(String s) {
        return (s.contains(".") && !s.contains(" ")) ||
                s.startsWith("http://") ||
                s.startsWith("https://");
    }

    // ── History ────────────────────────────────────────────────────────────

    public static void addToHistory(Context ctx, String query) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet(KEY_HISTORY, new LinkedHashSet<>());
        LinkedHashSet<String> history = new LinkedHashSet<>(raw);
        history.remove(query);          // remove duplicate
        history.add(query);             // add to end (most recent)
        // Trim to max
        while (history.size() > MAX_HISTORY) {
            history.remove(history.iterator().next());
        }
        prefs.edit().putStringSet(KEY_HISTORY, history).apply();
    }

    /** Returns history newest-first */
    public static List<String> getHistory(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet(KEY_HISTORY, new LinkedHashSet<>());
        List<String> list = new ArrayList<>(raw);
        java.util.Collections.reverse(list);
        return list;
    }

    public static void clearHistory(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_HISTORY).apply();
    }
}
