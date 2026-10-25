package app.locknova.core;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PresetCatalog {
    private static final List<Preset> BUILT_INS = Arrays.asList(
            new Preset("Violet Terminal", 0xFF9879F4, "dark", true),
            new Preset("Midnight", 0xFF9B8AF5, "dark", true),
            new Preset("Soft Lavender", 0xFF7555D9, "light", true),
            new Preset("Monochrome", 0xFF6A6B78, "light", true),
            new Preset("Dark Orchid", 0xFFBE78D3, "dark", true)
    );

    private PresetCatalog() { }
    public static List<Preset> builtIns() { return new ArrayList<>(BUILT_INS); }

    public static List<Preset> all(Context context) {
        ArrayList<Preset> result = new ArrayList<>(BUILT_INS);
        JSONArray custom = SettingsStore.getCustomPresets(context);
        for (int i = 0; i < custom.length(); i++) {
            JSONObject item = custom.optJSONObject(i);
            if (item == null) continue;
            String name = item.optString("name", "").trim();
            if (!name.isEmpty()) result.add(new Preset(name, item.optInt("accent", 0xFF7555D9), item.optString("theme", "system"), false));
        }
        return result;
    }

    public static void saveCustom(Context context, Preset preset) {
        JSONArray data = SettingsStore.getCustomPresets(context);
        try { data.put(new JSONObject().put("name", preset.name.trim()).put("accent", preset.accent).put("theme", preset.theme)); }
        catch (JSONException ignored) { }
        SettingsStore.setCustomPresets(context, data);
    }

    public static boolean renameCustom(Context context, int customIndex, String name) {
        JSONArray data = SettingsStore.getCustomPresets(context);
        JSONObject item = data.optJSONObject(customIndex);
        if (item == null || name == null || name.trim().isEmpty()) return false;
        try { item.put("name", name.trim()); }
        catch (JSONException ignored) { return false; }
        SettingsStore.setCustomPresets(context, data);
        return true;
    }

    public static boolean deleteCustom(Context context, int customIndex) {
        JSONArray old = SettingsStore.getCustomPresets(context);
        if (customIndex < 0 || customIndex >= old.length()) return false;
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) if (i != customIndex) next.put(old.opt(i));
        SettingsStore.setCustomPresets(context, next);
        return true;
    }
}
