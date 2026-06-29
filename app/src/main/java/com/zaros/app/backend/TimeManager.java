package com.zaros.app.backend;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ZarOS TimeManager (Java backend)
 * Provides formatted time/date strings for the UI layer.
 */
public class TimeManager {

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("h:mm", Locale.getDefault());
    private static final SimpleDateFormat AMPM_FMT =
            new SimpleDateFormat("a", Locale.getDefault());
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());

    /** Returns e.g. "10:42" */
    public static String getTime() {
        return TIME_FMT.format(new Date());
    }

    /** Returns "AM" or "PM" */
    public static String getAmPm() {
        return AMPM_FMT.format(new Date()).toUpperCase(Locale.getDefault());
    }

    /** Returns e.g. "Friday, June 6" */
    public static String getDate() {
        return DATE_FMT.format(new Date());
    }
}
