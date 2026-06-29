package com.zaros.app.backend;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ZarOS CallLogManager (Java backend)
 * Reads recent calls from the device CallLog provider.
 */
public class CallLogManager {

    public static class CallEntry {
        public final String number;
        public final String name;
        public final int    type;     // INCOMING=1, OUTGOING=2, MISSED=3
        public final long   date;
        public final long   duration;

        public CallEntry(String number, String name, int type, long date, long duration) {
            this.number   = number;
            this.name     = name;
            this.type     = type;
            this.date     = date;
            this.duration = duration;
        }

        public String typeEmoji() {
            switch (type) {
                case CallLog.Calls.INCOMING_TYPE: return "📞";
                case CallLog.Calls.OUTGOING_TYPE: return "📲";
                case CallLog.Calls.MISSED_TYPE:   return "📵";
                default:                          return "📱";
            }
        }

        public String formattedDate() {
            return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                .format(new Date(date));
        }

        public String formattedDuration() {
            long s = duration % 60, m = duration / 60;
            return m > 0 ? m + "m " + s + "s" : s + "s";
        }
    }

    public static List<CallEntry> getRecentCalls(Context ctx, int limit) {
        List<CallEntry> list = new ArrayList<>();
        try {
            Cursor cur = ctx.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                },
                null, null,
                CallLog.Calls.DATE + " DESC"
            );
            if (cur != null) {
                while (cur.moveToNext() && list.size() < limit) {
                    String number   = cur.getString(0);
                    String name     = cur.getString(1);
                    int    type     = cur.getInt(2);
                    long   date     = cur.getLong(3);
                    long   duration = cur.getLong(4);
                    if (name == null || name.isEmpty()) name = number;
                    list.add(new CallEntry(number, name, type, date, duration));
                }
                cur.close();
            }
        } catch (SecurityException | IllegalArgumentException e) {
            // Permission not yet granted, or provider rejected the query —
            // return what we have (empty list) instead of crashing the app.
        }
        return list;
    }
}
