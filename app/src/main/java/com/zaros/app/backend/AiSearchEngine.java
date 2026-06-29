package com.zaros.app.backend;

import android.content.Context;
import android.util.Log;

import com.zaros.app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ZarOS AiSearchEngine (Java backend)
 *
 * Two-step AI search:
 *   1. Serper.dev  → real web results (title, url, snippet)
 *   2. Groq API    → AI summary of those results
 *
 * Keys come from BuildConfig.SERPER_API_KEY / BuildConfig.GROQ_API_KEY,
 * which build.gradle populates from local.properties at build time.
 * Add these two lines to local.properties (the same file you already
 * recreate after every fresh extract for sdk.dir) and the keys will
 * persist across future ZarOS updates instead of resetting to the
 * placeholder strings every time you pull down a new zip:
 *
 *   SERPER_API_KEY=your_real_serper_key_here
 *   GROQ_API_KEY=your_real_groq_key_here
 *
 * Get a free Serper key at: https://serper.dev
 * Get a free Groq key at:   https://console.groq.com
 */
public class AiSearchEngine {

    private static final String TAG = "AiSearchEngine";

    public static final String SERPER_KEY = BuildConfig.SERPER_API_KEY;
    public static final String GROQ_KEY   = BuildConfig.GROQ_API_KEY;

    private static final String SERPER_URL = "https://google.serper.dev/search";
    private static final String GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama3-8b-8192";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    // ── Result model ──────────────────────────────────────────────────────

    public static class SearchResult {
        public final String title;
        public final String url;
        public final String snippet;
        public SearchResult(String title, String url, String snippet) {
            this.title = title; this.url = url; this.snippet = snippet;
        }
    }

    public static class SearchResponse {
        public final String aiSummary;
        public final List<SearchResult> results;
        public final String error;
        public SearchResponse(String aiSummary, List<SearchResult> results, String error) {
            this.aiSummary = aiSummary; this.results = results; this.error = error;
        }
    }

    // ── Main search method (call from background thread) ─────────────────

    /** True if the keys are still the unset placeholder strings from build.gradle's defaults. */
    private static boolean keysNotConfigured() {
        return "YOUR_SERPER_KEY_HERE".equals(SERPER_KEY) || "YOUR_GROQ_KEY_HERE".equals(GROQ_KEY);
    }

    public static SearchResponse search(String query) {
        if (keysNotConfigured()) {
            // Skip the network calls entirely — they'd just fail auth with
            // a literal placeholder string as the key, and that failure
            // mode previously looked identical to "your real key stopped
            // working," which made this much harder to diagnose than it
            // needed to be. See the class doc comment for the two
            // local.properties lines this needs.
            return new SearchResponse("", new ArrayList<>(),
                    "Fynder AI search isn't set up yet. Add SERPER_API_KEY and " +
                    "GROQ_API_KEY to local.properties, then rebuild — see " +
                    "AiSearchEngine.java for the free signup links.");
        }

        List<SearchResult> results = new ArrayList<>();
        String aiSummary = "";

        // Step 1: Serper web results
        try {
            JSONObject body = new JSONObject();
            body.put("q", query);
            body.put("num", 6);

            Request req = new Request.Builder()
                    .url(SERPER_URL)
                    .addHeader("X-API-KEY", SERPER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response res = client.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONObject json = new JSONObject(res.body().string());
                    JSONArray organic = json.optJSONArray("organic");
                    if (organic != null) {
                        for (int i = 0; i < Math.min(organic.length(), 6); i++) {
                            JSONObject item = organic.getJSONObject(i);
                            results.add(new SearchResult(
                                    item.optString("title", ""),
                                    item.optString("link", ""),
                                    item.optString("snippet", "")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Serper error: " + e.getMessage());
        }

        // Step 2: Groq AI summary
        try {
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < Math.min(results.size(), 4); i++) {
                SearchResult r = results.get(i);
                context.append(i + 1).append(". ").append(r.title)
                        .append(": ").append(r.snippet).append("\n");
            }

            String prompt = "You are Fynder AI, the smart assistant for ZarOS. " +
                    "Based on these search results, give a concise 2-3 sentence summary " +
                    "answering the query: \"" + query + "\"\n\nResults:\n" + context +
                    "\nBe direct, helpful and conversational. No bullet points.";

            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(msg);

            JSONObject groqBody = new JSONObject();
            groqBody.put("model", GROQ_MODEL);
            groqBody.put("messages", messages);
            groqBody.put("max_tokens", 200);
            groqBody.put("temperature", 0.7);

            Request req = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Authorization", "Bearer " + GROQ_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(groqBody.toString(), JSON))
                    .build();

            try (Response res = client.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONObject json = new JSONObject(res.body().string());
                    aiSummary = json
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Groq error: " + e.getMessage());
            aiSummary = "";
        }

        if (results.isEmpty() && aiSummary.isEmpty()) {
            return new SearchResponse("", results, "No results found. Check your API keys.");
        }

        return new SearchResponse(aiSummary, results, null);
    }
}
