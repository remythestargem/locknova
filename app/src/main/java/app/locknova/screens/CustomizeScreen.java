package app.locknova.screens;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.locknova.MainActivity;
import app.locknova.ui.UiKit;

public final class CustomizeScreen {
    private CustomizeScreen() { }

    private static final String[][] MODULES = {
            {"status", "Status Bar", "Clock, date and icon appearance preview", "PREVIEW ONLY", "◉"},
            {"quick", "Quick Settings", "Tile layout preview and Android connectivity shortcut", "PREVIEW + SHORTCUT", "▦"},
            {"lock", "Lock Screen", "Wallpaper API, preview clock styles and native settings", "WALLPAPER API", "▣"},
            {"home", "Home Screen", "Real launcher, searchable app drawer and stacked Android widgets", "OPTIONAL LAUNCHER", "⌂"},
            {"gestures", "Gestures", "Test gestures in LockNova and open Android settings", "IN-APP TEST", "↔"},
            {"system", "System UI", "Verified animation-scale operation through Shizuku", "SHIZUKU-DEPENDENT", "⌘"},
            {"appearance", "Appearance", "LockNova theme, accent and preview styling", "APP SETTINGS", "✦"},
            {"navigation", "Navigation & Display", "Shortcuts to Android display and launcher settings", "SYSTEM SHORTCUTS", "◫"}
    };

    public static View create(MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        ScrollView scroll = UiKit.scroll(activity);
        LinearLayout content = UiKit.pageContent(activity);
        scroll.addView(content);
        content.addView(UiKit.text(activity, "Customize", 26, p.text, true));
        TextView subtitle = UiKit.text(activity, "Tools are marked by what they actually change.", 13, p.muted, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = UiKit.dp(activity, 6);
        content.addView(subtitle, subtitleParams);
        UiKit.section(content, activity, "Status & panels");
        addModule(content, activity, MODULES[0]);
        addModule(content, activity, MODULES[1]);
        UiKit.section(content, activity, "Personalize");
        addModule(content, activity, MODULES[2]);
        addModule(content, activity, MODULES[3]);
        addModule(content, activity, MODULES[6]);
        UiKit.section(content, activity, "Interaction & system");
        addModule(content, activity, MODULES[4]);
        addModule(content, activity, MODULES[5]);
        addModule(content, activity, MODULES[7]);
        LinearLayout note = UiKit.card(activity);
        note.addView(UiKit.text(activity, "A preview never changes the system interface. Supported Android operations ask first, preserve previous values where possible, and verify the result.", 12, p.muted, false));
        UiKit.addCard(content, activity, note);
        return scroll;
    }

    private static void addModule(LinearLayout parent, MainActivity activity, String[] module) {
        UiKit.Palette p = UiKit.palette(activity);
        LinearLayout card = UiKit.card(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        TextView icon = UiKit.text(activity, module[4], 21, p.accent, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(UiKit.shape(p.accentSoft, 0, 15, activity));
        card.addView(icon, new LinearLayout.LayoutParams(UiKit.dp(activity, 46), UiKit.dp(activity, 46)));
        LinearLayout center = UiKit.column(activity);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        centerParams.leftMargin = UiKit.dp(activity, 12);
        card.addView(center, centerParams);
        center.addView(UiKit.text(activity, module[1], 14, p.text, true));
        TextView desc = UiKit.text(activity, module[2], 11, p.muted, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = UiKit.dp(activity, 4);
        center.addView(desc, descParams);
        TextView status = UiKit.text(activity, module[3], 9, p.accent, true);
        status.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = UiKit.dp(activity, 7);
        center.addView(status, statusParams);
        TextView arrow = UiKit.text(activity, "›", 22, p.muted, false);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(UiKit.dp(activity, 24), UiKit.dp(activity, 40)));
        card.setClickable(true);
        card.setOnClickListener(v -> activity.openModule(module[0]));
        UiKit.addCard(parent, activity, card);
    }
}
