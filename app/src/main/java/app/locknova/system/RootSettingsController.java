package app.locknova.system;

import android.content.Context;

import app.locknova.core.SettingsStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Root-gated Android 11 SystemUI tuner settings; ColorOS may store or ignore these keys differently. */
public final class RootSettingsController {
    public static final String[] STATUS_ICON_KEYS = {"alarm_clock", "bluetooth", "cell", "clock", "location", "mute", "rotate", "volume", "wifi"};
    private static final String ICON_BLACKLIST = "icon_blacklist";
    private static final String QS_TILES = "sysui_qs_tiles";
    private static final String[] TILE_PRIORITY = {"wifi", "cell", "bt", "airplane"};
    private static final Set<String> ICON_ALLOWLIST = new LinkedHashSet<>(Arrays.asList(STATUS_ICON_KEYS));

    public static final class Result {
        public final boolean success;
        public final String message;
        public final String value;
        Result(boolean success, String message, String value) { this.success = success; this.message = message; this.value = value; }
    }

    private RootSettingsController() { }
    public static Result readStatusIcons() { return read("secure", ICON_BLACKLIST); }
    public static Result readQuickTiles() { return read("secure", QS_TILES); }

    private static Result read(String namespace, String key) {
        RootBridge.Result result = RootBridge.read(namespace, key);
        return new Result(result.success, result.success ? "Read Android's stored " + key + " value." : result.message, result.output);
    }

    public static Result setHiddenStatusIcons(Context context, Set<String> selected) {
        if (selected == null) return new Result(false, "No icon selection was provided.", null);
        for (String key : selected) if (!ICON_ALLOWLIST.contains(key)) return new Result(false, "An icon is not on the supported allowlist.", null);
        RootBridge.Result read = RootBridge.read("secure", ICON_BLACKLIST);
        if (!read.success) return new Result(false, read.message, null);
        List<String> original = split(read.output);
        LinkedHashSet<String> next = new LinkedHashSet<>();
        for (String value : original) if (!ICON_ALLOWLIST.contains(value)) next.add(value);
        for (String key : STATUS_ICON_KEYS) if (selected.contains(key)) next.add(key);
        String target = join(next);
        return change(context, "secure", ICON_BLACKLIST, originalValue(read.output), target.isEmpty() ? "null" : target, "Status-icon visibility");
    }

    public static Result prioritizeConnectivityTiles(Context context) {
        RootBridge.Result read = RootBridge.read("secure", QS_TILES);
        if (!read.success) return new Result(false, read.message, null);
        List<String> original = split(read.output);
        if (original.isEmpty()) return new Result(false, "Android did not expose a current Quick Settings tile order; nothing was changed.", null);
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String priority : TILE_PRIORITY) if (original.contains(priority)) ordered.add(priority);
        if (ordered.isEmpty()) return new Result(false, "No recognized Wi-Fi, mobile-data, Bluetooth or airplane tile is present; nothing was changed.", null);
        ordered.addAll(original);
        String target = join(ordered);
        if (target.equals(join(original))) return new Result(true, "Connectivity tiles are already first in the stored order.", target);
        return change(context, "secure", QS_TILES, originalValue(read.output), target, "Quick Settings tile order");
    }

    private static Result change(Context context, String namespace, String key, String oldValue, String newValue, String label) {
        if (!saveBackup(context, key, oldValue)) return new Result(false, "Could not save the original Android value. Nothing was changed.", null);
        RootBridge.Result write = RootBridge.write(namespace, key, newValue);
        if (!write.success) return new Result(false, write.message + " The original value remains saved for recovery.", null);
        RootBridge.Result verified = RootBridge.read(namespace, key);
        if (!verified.success || !newValue.equals(verified.output)) {
            RootBridge.write(namespace, key, oldValue);
            return new Result(false, "Android did not verify the new " + label + " value. LockNova attempted to restore the previous value; the recovery record was kept.", verified.output);
        }
        return new Result(true, label + " was saved and read back. ColorOS may still ignore or override this Android SystemUI setting.", newValue);
    }

    public static Result restore(Context context) {
        JSONObject backup = SettingsStore.getRootSettingsBackup(context);
        if (backup.length() == 0) return new Result(false, "No saved root-backed SystemUI values are available to restore.", null);
        RootBridge.Result root = RootBridge.checkRoot();
        if (!root.success) return new Result(false, root.message + " The recovery record was kept.", null);
        String[] keys = {ICON_BLACKLIST, QS_TILES};
        for (String key : keys) {
            if (!backup.has(key)) continue;
            String value = backup.optString(key, "null");
            String toWrite = "null".equals(value) ? null : value;
            RootBridge.Result write = RootBridge.write("secure", key, toWrite);
            if (!write.success) return new Result(false, "Could not restore " + key + ": " + write.message + " The recovery record was kept.", null);
            RootBridge.Result read = RootBridge.read("secure", key);
            boolean restored = "null".equals(value) ? read.success && (read.output.isEmpty() || "null".equals(read.output)) : read.success && value.equals(read.output);
            if (!restored) return new Result(false, "Android did not verify restoration of " + key + ". The recovery record was kept.", read.output);
        }
        SettingsStore.clearRootSettingsBackup(context);
        return new Result(true, "Restored and verified the saved SystemUI settings.", null);
    }

    private static boolean saveBackup(Context context, String key, String value) {
        JSONObject backup = SettingsStore.getRootSettingsBackup(context);
        if (backup.has(key)) return true;
        try {
            backup.put(key, value == null || "null".equals(value) ? JSONObject.NULL : value);
            SettingsStore.setRootSettingsBackup(context, backup);
            return true;
        } catch (JSONException failure) { return false; }
    }

    private static String originalValue(String value) { return value == null || value.trim().isEmpty() ? "null" : value.trim(); }
    private static List<String> split(String value) {
        ArrayList<String> result = new ArrayList<>();
        if (value == null || value.trim().isEmpty() || "null".equals(value.trim())) return result;
        for (String item : value.split(",")) {
            String clean = item.trim();
            if (!clean.isEmpty() && !result.contains(clean)) result.add(clean);
        }
        return result;
    }
    private static String join(Iterable<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) { if (out.length() > 0) out.append(','); out.append(value); }
        return out.toString();
    }
}
