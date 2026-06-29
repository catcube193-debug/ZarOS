package com.zaros.app.backend;

/**
 * ZarOS AppInfo (Java backend)
 * Immutable data model representing an app entry on the home screen.
 * glossBgRes points to one of the bg_glossy_* drawables for the iOS-style icon look.
 */
public class AppInfo {

    private final String label;
    private final String packageName;
    private final int iconResId;
    private final int backgroundColorStart;
    private final int backgroundColorEnd;
    private final int glossBgRes;

    public AppInfo(String label, String packageName, int iconResId,
                   int backgroundColorStart, int backgroundColorEnd, int glossBgRes) {
        this.label = label;
        this.packageName = packageName;
        this.iconResId = iconResId;
        this.backgroundColorStart = backgroundColorStart;
        this.backgroundColorEnd = backgroundColorEnd;
        this.glossBgRes = glossBgRes;
    }

    // Backward-compatible constructor (defaults to no gloss override)
    public AppInfo(String label, String packageName, int iconResId,
                   int backgroundColorStart, int backgroundColorEnd) {
        this(label, packageName, iconResId, backgroundColorStart, backgroundColorEnd, 0);
    }

    public String getLabel()             { return label; }
    public String getPackageName()       { return packageName; }
    public int getIconResId()            { return iconResId; }
    public int getBackgroundColorStart() { return backgroundColorStart; }
    public int getBackgroundColorEnd()   { return backgroundColorEnd; }
    public int getGlossBgRes()           { return glossBgRes; }
}
