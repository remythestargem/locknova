package app.locknova.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SettingsStore {
    private static final String FILE = "locknova_settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_QUICK_ROUND = "quick_round";
    private static final String KEY_HAPTICS = "haptics";
    private static final String KEY_MOTION = "motion";
    private static final String KEY_STACK = "widget_stack";
    private static final String KEY_CUSTOM_PRESETS = "custom_presets";
    private static final String KEY_LAUNCHER_COLUMNS = "launcher_columns";
    private static final String KEY_LAUNCHER_ICON_SIZE = "launcher_icon_size";
    private static final String KEY_LAUNCHER_LABELS = "launcher_labels";
    private static final String KEY_LAUNCHER_ICON_SHAPE = "launcher_icon_shape";
    private static final String KEY_LAUNCHER_DOCK = "launcher_dock";
    private static final String KEY_WIDGET_IDS = "launcher_widget_ids";
    private static final String KEY_ANIMATION_BACKUP = "animation_backup";
    private static final String KEY_WALLPAPER_BACKUP_SYSTEM = "wallpaper_backup_system";
    private static final String KEY_WALLPAPER_BACKUP_LOCK = "wallpaper_backup_lock";

    private SettingsStore() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getTheme(Context context) { return prefs(context).getString(KEY_THEME, "system"); }
    public static void setTheme(Context context, String value) {
        String safe = "dark".equals(value) || "light".equals(value) ? value : "system";
        prefs(context).edit().putString(KEY_THEME, safe).apply();
    }
    public static boolean isDark(Context context) {
        String theme = getTheme(context);
        if ("dark".equals(theme)) return true;
        if ("light".equals(theme)) return false;
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }
    public static int getAccent(Context context) { return prefs(context).getInt(KEY_ACCENT, 0xFF7555D9); }
    public static void setAccent(Context context, int color) { prefs(context).edit().putInt(KEY_ACCENT, color).apply(); }
    public static int getQuickRound(Context context) { return prefs(context).getInt(KEY_QUICK_ROUND, 22); }
    public static void setQuickRound(Context context, int value) { prefs(context).edit().putInt(KEY_QUICK_ROUND, Math.max(10, Math.min(32, value))).apply(); }
    public static boolean getHaptics(Context context) { return prefs(context).getBoolean(KEY_HAPTICS, true); }
    public static void setHaptics(Context context, boolean value) { prefs(context).edit().putBoolean(KEY_HAPTICS, value).apply(); }
    public static boolean getMotion(Context context) { return prefs(context).getBoolean(KEY_MOTION, true); }
    public static void setMotion(Context context, boolean value) { prefs(context).edit().putBoolean(KEY_MOTION, value).apply(); }

    public static int getLauncherColumns(Context context) { return Math.max(3, Math.min(6, prefs(context).getInt(KEY_LAUNCHER_COLUMNS, 4))); }
    public static void setLauncherColumns(Context context, int value) { prefs(context).edit().putInt(KEY_LAUNCHER_COLUMNS, Math.max(3, Math.min(6, value))).apply(); }
    public static String getLauncherIconSize(Context context) { return prefs(context).getString(KEY_LAUNCHER_ICON_SIZE, "medium"); }
    public static void setLauncherIconSize(Context context, String value) {
        String safe = "small".equals(value) || "large".equals(value) ? value : "medium";
        prefs(context).edit().putString(KEY_LAUNCHER_ICON_SIZE, safe).apply();
    }
    public static boolean getLauncherLabels(Context context) { return prefs(context).getBoolean(KEY_LAUNCHER_LABELS, true); }
    public static void setLauncherLabels(Context context, boolean value) { prefs(context).edit().putBoolean(KEY_LAUNCHER_LABELS, value).apply(); }
    public static String getLauncherIconShape(Context context) { return prefs(context).getString(KEY_LAUNCHER_ICON_SHAPE, "rounded"); }
    public static void setLauncherIconShape(Context context, String value) {
        String safe = "circle".equals(value) || "square".equals(value) ? value : "rounded";
        prefs(context).edit().putString(KEY_LAUNCHER_ICON_SHAPE, safe).apply();
    }
    public static List<String> getLauncherDock(Context context) { return parseList(prefs(context).getString(KEY_LAUNCHER_DOCK, "")); }
    public static void setLauncherDock(Context context, List<String> values) {
        ArrayList<String> safe = new ArrayList<>();
        for (String item : values) if (item != null && item.matches("[A-Za-z0-9_.]+") && !safe.contains(item) && safe.size() < 8) safe.add(item);
        prefs(context).edit().putString(KEY_LAUNCHER_DOCK, android.text.TextUtils.join(",", safe)).apply();
    }
    public static List<Integer> getWidgetIds(Context context) {
        ArrayList<Integer> result = new ArrayList<>();
        for (String item : parseList(prefs(context).getString(KEY_WIDGET_IDS, ""))) {
            try { int id = Integer.parseInt(item); if (id >= 0 && !result.contains(id)) result.add(id); } catch (NumberFormatException ignored) { }
        }
        return result;
    }
    public static void setWidgetIds(Context context, List<Integer> ids) {
        ArrayList<String> safe = new ArrayList<>();
        for (Integer id : ids) if (id != null && id >= 0 && !safe.contains(String.valueOf(id))) safe.add(String.valueOf(id));
        prefs(context).edit().putString(KEY_WIDGET_IDS, android.text.TextUtils.join(",", safe)).apply();
    }
    private static List<String> parseList(String raw) {
        ArrayList<String> result = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) for (String item : raw.split(",")) { String clean = item.trim(); if (!clean.isEmpty() && !result.contains(clean)) result.add(clean); }
        return result;
    }

    public static List<String> getWidgetStack(Context context) {
        String raw = prefs(context).getString(KEY_STACK, "Clock,Calendar,Weather");
        ArrayList<String> result = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            for (String item : raw.split(",")) {
                String clean = item.trim();
                if (Arrays.asList("Clock", "Calendar", "Weather", "Music", "Notes").contains(clean) && !result.contains(clean)) result.add(clean);
            }
        }
        if (result.isEmpty()) result.add("Clock");
        return result;
    }
    public static void setWidgetStack(Context context, List<String> stack) {
        ArrayList<String> safe = new ArrayList<>();
        for (String item : stack) {
            if (Arrays.asList("Clock", "Calendar", "Weather", "Music", "Notes").contains(item) && !safe.contains(item)) safe.add(item);
        }
        if (safe.isEmpty()) safe.add("Clock");
        prefs(context).edit().putString(KEY_STACK, android.text.TextUtils.join(",", safe)).apply();
    }

    public static JSONArray getCustomPresets(Context context) {
        String raw = prefs(context).getString(KEY_CUSTOM_PRESETS, "[]");
        try { return new JSONArray(raw); } catch (JSONException ignored) { return new JSONArray(); }
    }
    public static void setCustomPresets(Context context, JSONArray presets) {
        prefs(context).edit().putString(KEY_CUSTOM_PRESETS, presets.toString()).apply();
    }

    public static JSONObject getAnimationBackup(Context context) {
        String raw = prefs(context).getString(KEY_ANIMATION_BACKUP, "{}");
        try { return new JSONObject(raw); } catch (JSONException ignored) { return new JSONObject(); }
    }
    public static void setAnimationBackup(Context context, JSONObject backup) {
        prefs(context).edit().putString(KEY_ANIMATION_BACKUP, backup.toString()).apply();
    }
    public static void clearAnimationBackup(Context context) { prefs(context).edit().remove(KEY_ANIMATION_BACKUP).apply(); }
    public static boolean hasAnimationBackup(Context context) { return getAnimationBackup(context).length() > 0; }

    public static String getWallpaperBackupPath(Context context, boolean lock) {
        return prefs(context).getString(lock ? KEY_WALLPAPER_BACKUP_LOCK : KEY_WALLPAPER_BACKUP_SYSTEM, null);
    }
    public static void setWallpaperBackupPath(Context context, boolean lock, String path) {
        prefs(context).edit().putString(lock ? KEY_WALLPAPER_BACKUP_LOCK : KEY_WALLPAPER_BACKUP_SYSTEM, path).apply();
    }

    public static JSONObject exportConfig(Context context) {
        JSONObject data = new JSONObject();
        try {
            data.put("format", "locknova-config-v1");
            data.put("theme", getTheme(context));
            data.put("accent", getAccent(context));
            data.put("quickRound", getQuickRound(context));
            data.put("haptics", getHaptics(context));
            data.put("motion", getMotion(context));
            data.put("launcherColumns", getLauncherColumns(context));
            data.put("launcherIconSize", getLauncherIconSize(context));
            data.put("launcherLabels", getLauncherLabels(context));
            data.put("launcherIconShape", getLauncherIconShape(context));
            JSONArray dock = new JSONArray();
            for (String packageName : getLauncherDock(context)) dock.put(packageName);
            data.put("launcherDock", dock);
            JSONArray stack = new JSONArray();
            for (String item : getWidgetStack(context)) stack.put(item);
            data.put("widgetStack", stack);
            data.put("customPresets", getCustomPresets(context));
        } catch (JSONException ignored) { }
        return data;
    }

    public static boolean importConfig(Context context, JSONObject data) {
        if (data == null || !"locknova-config-v1".equals(data.optString("format"))) return false;
        setTheme(context, data.optString("theme", "system"));
        setAccent(context, data.optInt("accent", 0xFF7555D9));
        setQuickRound(context, data.optInt("quickRound", 22));
        setHaptics(context, data.optBoolean("haptics", true));
        setMotion(context, data.optBoolean("motion", true));
        setLauncherColumns(context, data.optInt("launcherColumns", 4));
        setLauncherIconSize(context, data.optString("launcherIconSize", "medium"));
        setLauncherLabels(context, data.optBoolean("launcherLabels", true));
        setLauncherIconShape(context, data.optString("launcherIconShape", "rounded"));
        JSONArray dock = data.optJSONArray("launcherDock");
        if (dock != null) { ArrayList<String> pinned = new ArrayList<>(); for (int i = 0; i < dock.length(); i++) pinned.add(dock.optString(i, "")); setLauncherDock(context, pinned); }
        JSONArray rawStack = data.optJSONArray("widgetStack");
        if (rawStack != null) {
            ArrayList<String> stack = new ArrayList<>();
            for (int i = 0; i < rawStack.length(); i++) stack.add(rawStack.optString(i, ""));
            setWidgetStack(context, stack);
        }
        JSONArray presets = data.optJSONArray("customPresets");
        if (presets != null) setCustomPresets(context, presets);
        return true;
    }

    public static void reset(Context context) {
        prefs(context).edit()
                .remove(KEY_THEME).remove(KEY_ACCENT).remove(KEY_QUICK_ROUND)
                .remove(KEY_HAPTICS).remove(KEY_MOTION).remove(KEY_STACK)
                .remove(KEY_CUSTOM_PRESETS).remove(KEY_LAUNCHER_COLUMNS)
                .remove(KEY_LAUNCHER_ICON_SIZE).remove(KEY_LAUNCHER_LABELS)
                .remove(KEY_LAUNCHER_ICON_SHAPE).remove(KEY_LAUNCHER_DOCK).apply();
    }
}
