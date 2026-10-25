package app.locknova.screens;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.locknova.MainActivity;
import app.locknova.core.Preset;
import app.locknova.core.PresetCatalog;
import app.locknova.core.SettingsStore;
import app.locknova.ui.UiKit;

import java.util.List;

public final class PresetsScreen {
    private PresetsScreen() { }

    public static View create(MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        ScrollView scroll = UiKit.scroll(activity);
        LinearLayout content = UiKit.pageContent(activity);
        scroll.addView(content);
        content.addView(UiKit.text(activity, "Presets", 26, p.text, true));
        TextView intro = UiKit.text(activity, "Save and apply LockNova's look. Presets change app appearance and preview accents only; they do not modify Android system settings.", 13, p.muted, false);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introParams.topMargin = UiKit.dp(activity, 6);
        content.addView(intro, introParams);
        TextView save = UiKit.button(activity, "Save current look as preset", true, v -> promptSave(activity));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.topMargin = UiKit.dp(activity, 16);
        content.addView(save, saveParams);
        UiKit.section(content, activity, "Original LockNova presets");
        List<Preset> all = PresetCatalog.all(activity);
        int builtinCount = PresetCatalog.builtIns().size();
        for (int i = 0; i < all.size(); i++) addPreset(content, activity, all.get(i), i, builtinCount);
        return scroll;
    }

    private static void addPreset(LinearLayout parent, MainActivity activity, Preset preset, int position, int builtinCount) {
        UiKit.Palette p = UiKit.palette(activity);
        LinearLayout card = UiKit.card(activity);
        LinearLayout heading = UiKit.row(activity);
        TextView swatch = UiKit.text(activity, "●", 28, preset.accent, true);
        heading.addView(swatch, new LinearLayout.LayoutParams(UiKit.dp(activity, 36), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout titles = UiKit.column(activity);
        LinearLayout.LayoutParams titlesParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titlesParams.leftMargin = UiKit.dp(activity, 8);
        heading.addView(titles, titlesParams);
        titles.addView(UiKit.text(activity, preset.name, 15, p.text, true));
        TextView desc = UiKit.text(activity, ("dark".equals(preset.theme) ? "Deep charcoal" : "Warm light") + " · LockNova accent", 11, p.muted, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = UiKit.dp(activity, 4);
        titles.addView(desc, descParams);
        heading.addView(UiKit.pill(activity, preset.builtIn ? "BUILT-IN" : "CUSTOM", p.accentSoft, p.accent));
        card.addView(heading);
        TextView apply = UiKit.button(activity, "Apply to LockNova", true, v -> activity.applyPreset(preset));
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        applyParams.topMargin = UiKit.dp(activity, 12);
        card.addView(apply, applyParams);
        LinearLayout actions = UiKit.row(activity);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = UiKit.dp(activity, 8);
        card.addView(actions, actionsParams);
        TextView duplicate = UiKit.button(activity, "Duplicate", false, v -> duplicate(activity, preset));
        actions.addView(duplicate, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (position >= builtinCount) {
            TextView edit = UiKit.button(activity, "Edit", false, v -> editName(activity, position - builtinCount, preset.name));
            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            editParams.leftMargin = UiKit.dp(activity, 7);
            actions.addView(edit, editParams);
            TextView delete = UiKit.button(activity, "Delete", false, v -> delete(activity, position - builtinCount, preset.name));
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            deleteParams.leftMargin = UiKit.dp(activity, 7);
            actions.addView(delete, deleteParams);
        }
        UiKit.addCard(parent, activity, card);
    }

    private static void promptSave(MainActivity activity) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint("e.g. My evening look");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(activity).setTitle("Save current look")
                .setMessage("This saves the current LockNova theme and accent only.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) { activity.toast("Enter a preset name."); return; }
                    PresetCatalog.saveCustom(activity, new Preset(name, SettingsStore.getAccent(activity), SettingsStore.getTheme(activity), false));
                    activity.toast("Preset saved locally.");
                    activity.refreshScreen();
                }).show();
    }

    private static void duplicate(MainActivity activity, Preset source) {
        String name = source.name + " copy";
        PresetCatalog.saveCustom(activity, new Preset(name, source.accent, source.theme, false));
        activity.toast("Preset duplicated.");
        activity.refreshScreen();
    }

    private static void editName(MainActivity activity, int customIndex, String existing) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setText(existing);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(activity).setTitle("Edit preset name").setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (!PresetCatalog.renameCustom(activity, customIndex, input.getText().toString())) activity.toast("Preset name cannot be empty.");
                    activity.refreshScreen();
                }).show();
    }

    private static void delete(MainActivity activity, int customIndex, String name) {
        new AlertDialog.Builder(activity).setTitle("Delete “" + name + "”?")
                .setMessage("This removes the saved preset from LockNova. It does not change your current appearance.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> { PresetCatalog.deleteCustom(activity, customIndex); activity.refreshScreen(); })
                .show();
    }
}
