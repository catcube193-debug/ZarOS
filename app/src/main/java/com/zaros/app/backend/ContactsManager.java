package com.zaros.app.backend;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ZarOS ContactsManager (Java backend)
 * Reads contacts from the device ContactsContract provider.
 */
public class ContactsManager {

    public static class Contact {
        public final String id;
        public final String name;
        public final String phone;
        public final String initials;

        public Contact(String id, String name, String phone) {
            this.id    = id;
            this.name  = name;
            this.phone = phone;
            // Build initials from name
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                this.initials = String.valueOf(parts[0].charAt(0)) +
                                String.valueOf(parts[1].charAt(0));
            } else if (parts.length == 1 && !parts[0].isEmpty()) {
                this.initials = String.valueOf(parts[0].charAt(0));
            } else {
                this.initials = "?";
            }
        }
    }

    public static List<Contact> getContacts(Context ctx) {
        List<Contact> list = new ArrayList<>();
        try {
            ContentResolver cr = ctx.getContentResolver();

            Cursor cur = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cur != null) {
                while (cur.moveToNext()) {
                    String id    = cur.getString(0);
                    String name  = cur.getString(1);
                    String phone = cur.getString(2);
                    if (name != null && phone != null) {
                        list.add(new Contact(id, name, phone));
                    }
                }
                cur.close();
            }
        } catch (SecurityException | IllegalArgumentException e) {
            // Permission not granted yet, or provider rejected the query.
        }
        return list;
    }

    /** Look up a display name for a phone number. Falls back to the raw number on any failure. */
    public static String getNameForNumber(Context ctx, String number) {
        if (number == null || number.isEmpty()) return number;
        try {
            ContentResolver cr = ctx.getContentResolver();
            android.net.Uri uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(number));
            Cursor cur = cr.query(uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null);
            if (cur != null) {
                try {
                    if (cur.moveToFirst()) return cur.getString(0);
                } finally { cur.close(); }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            // Permission not granted yet, or provider rejected the query.
        }
        return number;
    }
}
