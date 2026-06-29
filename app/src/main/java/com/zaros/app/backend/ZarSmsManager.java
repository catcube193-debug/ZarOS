package com.zaros.app.backend;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ZarSmsManager {

    public static class SmsThread {
        public final String address;
        public final String name;
        public final String snippet;
        public final long   date;
        public final int    unread;
        public final String initials;

        public SmsThread(String address, String name, String snippet, long date, int unread) {
            this.address  = address;
            this.name     = name;
            this.snippet  = snippet;
            this.date     = date;
            this.unread   = unread;
            String[] p    = name.trim().split("\\s+");
            this.initials = p.length >= 2
                ? String.valueOf(p[0].charAt(0)) + String.valueOf(p[1].charAt(0))
                : (p.length == 1 && !p[0].isEmpty() ? String.valueOf(p[0].charAt(0)) : "?");
        }

        public String formattedDate() {
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(date));
        }
    }

    public static class SmsMessage {
        public final String  body;
        public final long    date;
        public final boolean outgoing;
        public final boolean isMms;

        public SmsMessage(String body, long date, boolean outgoing, boolean isMms) {
            this.body     = body;
            this.date     = date;
            this.outgoing = outgoing;
            this.isMms    = isMms;
        }

        public String formattedTime() {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(date));
        }

        public boolean containsUrl() {
            return body != null && (
                body.contains("http://") ||
                body.contains("https://") ||
                body.contains("www.")
            );
        }
    }

    // ── Threads ───────────────────────────────────────────────────────────

    public static List<SmsThread> getThreads(Context ctx) {
        Map<String, SmsThread> seen = new LinkedHashMap<>();
        // Cache contact name lookups across this whole call so each phone
        // number only ever hits the Contacts provider once, instead of once
        // per message row. This is the single biggest win for load time.
        Map<String, String> nameCache = new java.util.HashMap<>();

        // SMS inbox + sent
        querySmsByUri(ctx, Uri.parse("content://sms/inbox"), seen, nameCache);
        querySmsByUri(ctx, Uri.parse("content://sms/sent"),  seen, nameCache);
        // MMS threads
        queryMmsThreads(ctx, seen, nameCache);

        List<SmsThread> list = new ArrayList<>(seen.values());
        Collections.sort(list, (a, b) -> Long.compare(b.date, a.date));
        return list;
    }

    private static String cachedName(Context ctx, String address, Map<String, String> nameCache) {
        String cached = nameCache.get(address);
        if (cached != null) return cached;
        String name = ContactsManager.getNameForNumber(ctx, address);
        nameCache.put(address, name);
        return name;
    }

    private static void querySmsByUri(Context ctx, Uri uri,
                                       Map<String, SmsThread> seen,
                                       Map<String, String> nameCache) {
        try {
            Cursor cur = ctx.getContentResolver().query(uri,
                new String[]{"address", "body", "date", "read"},
                null, null, "date DESC");
            if (cur == null) return;
            while (cur.moveToNext()) {
                String address = cur.getString(0);
                String body    = cur.getString(1);
                long   date    = cur.getLong(2);
                int    read    = cur.getInt(3);
                if (address == null || address.isEmpty()) continue;
                // Cursor is sorted DESC, so the first row per address is
                // already the most recent — skip the contact lookup
                // entirely for every row after the thread is established.
                if (!seen.containsKey(address)) {
                    String name = cachedName(ctx, address, nameCache);
                    seen.put(address, new SmsThread(address, name,
                        body != null ? body : "", date, read == 0 ? 1 : 0));
                }
            }
            cur.close();
        } catch (Exception e) { /* ignore */ }
    }

    private static void queryMmsThreads(Context ctx, Map<String, SmsThread> seen,
                                         Map<String, String> nameCache) {
        try {
            // Get MMS messages
            Cursor mmsCur = ctx.getContentResolver().query(
                Uri.parse("content://mms/"),
                new String[]{"_id", "date", "msg_box"},
                null, null, "date DESC");
            if (mmsCur == null) return;

            while (mmsCur.moveToNext()) {
                String mmsId   = mmsCur.getString(0);
                long   date    = mmsCur.getLong(1) * 1000L; // MMS uses seconds

                // Get address for this MMS — this is itself 1 extra query,
                // so only pay that cost for addresses we might actually add.
                String address = getMmsAddress(ctx, mmsId);
                if (address == null || address.isEmpty()) continue;
                if (seen.containsKey(address)) continue; // already have a newer thread entry

                // Get text body
                String body = getMmsBody(ctx, mmsId);
                if (body == null || body.isEmpty()) body = "📎 MMS";

                String name = cachedName(ctx, address, nameCache);
                seen.put(address, new SmsThread(address, name, body, date, 0));
            }
            mmsCur.close();
        } catch (Exception e) { /* MMS not available */ }
    }

    // ── Messages for a conversation ───────────────────────────────────────

    public static List<SmsMessage> getMessages(Context ctx, String address) {
        List<SmsMessage> messages = new ArrayList<>();

        // SMS messages
        fetchSms(ctx, Uri.parse("content://sms/inbox"), address, false, messages);
        fetchSms(ctx, Uri.parse("content://sms/sent"),  address, true,  messages);

        // MMS messages
        fetchMms(ctx, address, messages);

        Collections.sort(messages, (a, b) -> Long.compare(a.date, b.date));
        return messages;
    }

    private static void fetchSms(Context ctx, Uri uri, String address,
                                  boolean outgoing, List<SmsMessage> out) {
        try {
            Cursor cur = ctx.getContentResolver().query(uri,
                new String[]{"body", "date"}, "address=?",
                new String[]{address}, "date ASC");
            if (cur == null) return;
            while (cur.moveToNext()) {
                String body = cur.getString(0);
                long   date = cur.getLong(1);
                if (body != null && !body.isEmpty())
                    out.add(new SmsMessage(body, date, outgoing, false));
            }
            cur.close();
        } catch (Exception e) { /* ignore */ }
    }

    private static void fetchMms(Context ctx, String address, List<SmsMessage> out) {
        try {
            Cursor mmsCur = ctx.getContentResolver().query(
                Uri.parse("content://mms/"),
                new String[]{"_id", "date", "msg_box"},
                null, null, "date ASC");
            if (mmsCur == null) return;

            // The conversation's own address never changes across rows —
            // normalize it once instead of on every iteration.
            String normalAddr = normalizeNumber(address);

            while (mmsCur.moveToNext()) {
                String mmsId  = mmsCur.getString(0);
                long   date   = mmsCur.getLong(1) * 1000L;
                int    msgBox = mmsCur.getInt(2);

                String mmsAddr = getMmsAddress(ctx, mmsId);
                if (mmsAddr == null) continue;

                String normalMmsAddr = normalizeNumber(mmsAddr);
                if (!normalAddr.equals(normalMmsAddr) &&
                    !mmsAddr.contains(address) &&
                    !address.contains(mmsAddr)) continue;

                String body = getMmsBody(ctx, mmsId);
                if (body == null || body.isEmpty()) body = "📎 MMS message";

                boolean outgoing = (msgBox == 2);
                out.add(new SmsMessage(body, date, outgoing, true));
            }
            mmsCur.close();
        } catch (Exception e) { /* MMS not available */ }
    }

    // ── MMS helpers ───────────────────────────────────────────────────────

    private static String getMmsAddress(Context ctx, String mmsId) {
        try {
            Uri addrUri = Uri.parse("content://mms/" + mmsId + "/addr");
            Cursor cur = ctx.getContentResolver().query(addrUri,
                new String[]{"address", "type"}, null, null, null);
            if (cur == null) return null;
            String address = null;
            while (cur.moveToNext()) {
                String addr = cur.getString(0);
                int    type = cur.getInt(1);
                // type 137 = FROM, type 151 = TO
                if (addr != null && !addr.equals("insert-address-token")) {
                    address = addr;
                    if (type == 137) break; // prefer FROM address
                }
            }
            cur.close();
            return address;
        } catch (Exception e) { return null; }
    }

    private static String getMmsBody(Context ctx, String mmsId) {
        try {
            Uri partUri = Uri.parse("content://mms/part");
            Cursor cur = ctx.getContentResolver().query(partUri,
                new String[]{"_id", "ct", "_data", "text"},
                "mid=?", new String[]{mmsId}, null);
            if (cur == null) return null;
            StringBuilder sb = new StringBuilder();
            while (cur.moveToNext()) {
                String ct   = cur.getString(1); // content type
                String text = cur.getString(3);
                if ("text/plain".equals(ct) && text != null && !text.isEmpty()) {
                    sb.append(text);
                }
            }
            cur.close();
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) { return null; }
    }

    private static String normalizeNumber(String number) {
        if (number == null) return "";
        // Strip everything except digits
        String digits = number.replaceAll("[^0-9]", "");
        // Use last 10 digits for comparison
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        return digits;
    }

    // ── Send ──────────────────────────────────────────────────────────────

    public static void sendSms(String number, String body) {
        try {
            SmsManager.getDefault().sendTextMessage(number, null, body, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS: " + e.getMessage());
        }
    }
}
