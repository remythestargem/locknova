package app.locknova.screens;

import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import app.locknova.MainActivity;
import app.locknova.core.EventLog;
import app.locknova.core.SettingsStore;
import app.locknova.system.ShizukuBridge;
import app.locknova.ui.UiKit;

public final class SettingsScreen {
    private static final int[] ACCENTS = {0xFF7555D9, 0xFF9272EB, 0xFFBB6DD3, 0xFF4E7BCC, 0xFF39947D, 0xFF6A6B78};
    private SettingsScreen() { }

    public static View create(MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        ScrollView scroll = UiKit.scroll(activity);
        LinearLayout content = UiKit.pageContent(activity);
        scroll.addView(content);
        content.addView(UiKit.text(activity, "Settings", 26, p.text, true));
        TextView intro = UiKit.text(activity, "Tune LockNova itself, check access, or keep a local backup.", 13, p.muted, false);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introParams.topMargin = UiKit.dp(activity, 6);
        content.addView(intro, introParams);

        UiKit.section(content, activity, "LockNova Launcher");
        LinearLayout launcher = UiKit.card(activity);
        launcher.addView(UiKit.text(activity, "Optional real home-screen launcher", 14, p.text, true));
        TextView launcherNote = UiKit.text(activity, "Open the searchable app drawer, pin a dock and host real Android widgets in a swipeable stack. These settings are stored locally.", 12, p.muted, false);
        LinearLayout.LayoutParams launcherNoteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        launcherNoteParams.topMargin = UiKit.dp(activity, 6);
        launcher.addView(launcherNote, launcherNoteParams);
        TextView openLauncher = UiKit.button(activity, "Open LockNova launcher", true, v -> activity.openLauncher());
        LinearLayout.LayoutParams openLauncherParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        openLauncherParams.topMargin = UiKit.dp(activity, 11);
        launcher.addView(openLauncher, openLauncherParams);
        TextView setDefault = UiKit.button(activity, "Set as default home app", false, v -> activity.openSystemSettings(Settings.ACTION_HOME_SETTINGS));
        LinearLayout.LayoutParams defaultParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        defaultParams.topMargin = UiKit.dp(activity, 7);
        launcher.addView(setDefault, defaultParams);
        TextView grid = UiKit.button(activity, "App grid · " + SettingsStore.getLauncherColumns(activity) + " columns", false, v -> chooseGrid(activity));
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = UiKit.dp(activity, 7);
        launcher.addView(grid, gridParams);
        TextView size = UiKit.button(activity, "Icon size · " + SettingsStore.getLauncherIconSize(activity), false, v -> chooseIconSize(activity));
        LinearLayout.LayoutParams sizeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sizeParams.topMargin = UiKit.dp(activity, 7);
        launcher.addView(size, sizeParams);
        TextView shape = UiKit.button(activity, "Icon shape · " + SettingsStore.getLauncherIconShape(activity), false, v -> chooseIconShape(activity));
        LinearLayout.LayoutParams shapeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shapeParams.topMargin = UiKit.dp(activity, 7);
        launcher.addView(shape, shapeParams);
        addSwitch(launcher, activity, "App labels", "Show names below launcher icons.", SettingsStore.getLauncherLabels(activity), checked -> SettingsStore.setLauncherLabels(activity, checked));
        UiKit.addCard(content, activity, launcher);

        UiKit.section(content, activity, "Appearance");
        LinearLayout theme = UiKit.card(activity);
        theme.addView(UiKit.text(activity, "Theme", 14, p.text, true));
        String themeLabel = "system".equals(SettingsStore.getTheme(activity)) ? "Follow device" : "dark".equals(SettingsStore.getTheme(activity)) ? "Dark" : "Light";
        TextView themeChoice = UiKit.button(activity, themeLabel + " · Change", false, v -> chooseTheme(activity));
        LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        themeParams.topMargin = UiKit.dp(activity, 9);
        theme.addView(themeChoice, themeParams);
        TextView accentLabel = UiKit.text(activity, "Accent color", 12, p.muted, true);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        accentParams.topMargin = UiKit.dp(activity, 15);
        theme.addView(accentLabel, accentParams);
        LinearLayout swatches = UiKit.row(activity);
        LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(activity, 48));
        swatchParams.topMargin = UiKit.dp(activity, 5);
        theme.addView(swatches, swatchParams);
        for (int color : ACCENTS) {
            TextView swatch = UiKit.text(activity, color == SettingsStore.getAccent(activity) ? "✓" : "", 16, 0xFFFFFFFF, true);
            swatch.setGravity(android.view.Gravity.CENTER);
            swatch.setBackground(UiKit.shape(color, 0, 20, activity));
            swatch.setClickable(true);
            swatch.setOnClickListener(v -> { SettingsStore.setAccent(activity, color); activity.refreshScreen(); });
            LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(0, UiKit.dp(activity, 40), 1f);
            swatchLp.rightMargin = UiKit.dp(activity, 7);
            swatches.addView(swatch, swatchLp);
        }
        UiKit.addCard(content, activity, theme);

        UiKit.section(content, activity, "App behavior");
        LinearLayout behavior = UiKit.card(activity);
        addSwitch(behavior, activity, "Smooth preview transitions", "Controls LockNova preview animation only.", SettingsStore.getMotion(activity), checked -> { SettingsStore.setMotion(activity, checked); activity.refreshScreen(); });
        addDivider(behavior, activity);
        addSwitch(behavior, activity, "Haptic feedback", "Enables haptics on supported LockNova buttons.", SettingsStore.getHaptics(activity), checked -> SettingsStore.setHaptics(activity, checked));
        UiKit.addCard(content, activity, behavior);

        UiKit.section(content, activity, "Shizuku diagnostics");
        ShizukuBridge.State state = activity.getShizukuState();
        LinearLayout diagnostics = UiKit.card(activity);
        diagnostics.addView(UiKit.text(activity, state.message, 14, p.text, true));
        TextView details = UiKit.text(activity, "Manager app: " + (state.managerInstalled ? "detected" : "not detected") + "\nBinder: " + (state.binderAvailable ? "available" : "not available") + "\nLockNova authorization: " + (state.authorized ? "granted" : "not granted"), 12, p.muted, false);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailsParams.topMargin = UiKit.dp(activity, 8);
        diagnostics.addView(details, detailsParams);
        TextView permissionAction = UiKit.button(activity, state.binderAvailable && !state.authorized ? "Request permission" : "Check / set up Shizuku", true, v -> activity.requestShizukuAccess());
        LinearLayout.LayoutParams permissionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        permissionParams.topMargin = UiKit.dp(activity, 12);
        diagnostics.addView(permissionAction, permissionParams);
        TextView coloros = UiKit.text(activity, "On OPPO ColorOS, Shizuku's official guide notes that Developer options → Permission monitoring may need to be disabled if ADB permissions are limited. Shizuku is not root, and may need restarting after reboot.", 12, p.muted, false);
        LinearLayout.LayoutParams colorosParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colorosParams.topMargin = UiKit.dp(activity, 10);
        diagnostics.addView(coloros, colorosParams);
        UiKit.addCard(content, activity, diagnostics);

        UiKit.section(content, activity, "Backup & restore");
        LinearLayout backup = UiKit.card(activity);
        backup.addView(UiKit.text(activity, "Configuration stays on your device unless you choose a document location.", 12, p.muted, false));
        addAction(backup, activity, "Export configuration", "JSON includes theme, accent, launcher grid/icons/dock and custom previews. Android widget bindings stay on this device.", () -> activity.startBackupExport());
        addAction(backup, activity, "Import configuration", "Select a LockNova JSON backup.", () -> confirmImport(activity));
        UiKit.addCard(content, activity, backup);

        UiKit.section(content, activity, "About & help");
        LinearLayout about = UiKit.card(activity);
        String version = "Version unavailable";
        try { PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0); version = "Version " + info.versionName + " (" + info.versionCode + ")"; }
        catch (Exception ignored) { }
        about.addView(UiKit.text(activity, "LockNova", 15, p.text, true));
        TextView versionView = UiKit.text(activity, version + "\nJava · Android views · local storage · no account or backend", 12, p.muted, false);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionParams.topMargin = UiKit.dp(activity, 6);
        about.addView(versionView, versionParams);
        addAction(about, activity, "Troubleshooting", "Open official Shizuku setup and ColorOS notes.", () -> activity.openUrl("https://shizuku.rikka.app/guide/setup/"));
        addAction(about, activity, "Open-source licenses", "Shizuku API and provider license information.", () -> activity.openUrl("https://github.com/RikkaApps/Shizuku-API"));
        UiKit.addCard(content, activity, about);

        UiKit.section(content, activity, "Reset");
        TextView reset = UiKit.button(activity, "Reset LockNova preferences", false, v -> confirmReset(activity));
        content.addView(reset);
        TextView resetNote = UiKit.text(activity, "This does not change Android settings or remove saved restoration records.", 11, p.muted, false);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = UiKit.dp(activity, 7);
        content.addView(resetNote, noteParams);
        return scroll;
    }

    private interface Change { void onChanged(boolean value); }
    private static void addSwitch(LinearLayout parent, MainActivity activity, String title, String summary, boolean initial, Change change) {
        UiKit.Palette p = UiKit.palette(activity);
        LinearLayout row = UiKit.row(activity);
        LinearLayout labels = UiKit.column(activity);
        row.addView(labels, UiKit.weightParams());
        labels.addView(UiKit.text(activity, title, 13, p.text, true));
        TextView detail = UiKit.text(activity, summary, 11, p.muted, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = UiKit.dp(activity, 4);
        labels.addView(detail, detailParams);
        Switch control = new Switch(activity);
        control.setChecked(initial);
        control.setOnCheckedChangeListener((button, checked) -> change.onChanged(checked));
        row.addView(control);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = UiKit.dp(activity, 5);
        rowParams.bottomMargin = UiKit.dp(activity, 5);
        parent.addView(row, rowParams);
    }
    private static void addDivider(LinearLayout parent, MainActivity activity) {
        View divider = new View(activity);
        divider.setBackgroundColor(UiKit.palette(activity).border);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(activity, 1));
        params.topMargin = UiKit.dp(activity, 5);
        params.bottomMargin = UiKit.dp(activity, 8);
        parent.addView(divider, params);
    }
    private static void addAction(LinearLayout parent, MainActivity activity, String title, String detail, Runnable action) {
        UiKit.Palette p = UiKit.palette(activity);
        LinearLayout row = UiKit.row(activity);
        LinearLayout labels = UiKit.column(activity);
        row.addView(labels, UiKit.weightParams());
        labels.addView(UiKit.text(activity, title, 13, p.text, true));
        TextView desc = UiKit.text(activity, detail, 11, p.muted, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = UiKit.dp(activity, 4);
        labels.addView(desc, descParams);
        TextView arrow = UiKit.text(activity, "›", 22, p.muted, false);
        row.addView(arrow);
        row.setClickable(true);
        row.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = UiKit.dp(activity, 14);
        parent.addView(row, rowParams);
    }
    private static void chooseTheme(MainActivity activity) {
        String[] values = {"Follow device", "Light", "Dark"};
        String[] keys = {"system", "light", "dark"};
        int checked = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(SettingsStore.getTheme(activity))) checked = i;
        new AlertDialog.Builder(activity).setTitle("Choose theme").setSingleChoiceItems(values, checked, (dialog, which) -> {
            SettingsStore.setTheme(activity, keys[which]);
            dialog.dismiss();
            activity.refreshScreen();
        }).setNegativeButton("Cancel", null).show();
    }
    private static void chooseGrid(MainActivity activity) {
        String[] labels = {"3 columns", "4 columns", "5 columns", "6 columns"};
        int[] values = {3, 4, 5, 6};
        int selected = SettingsStore.getLauncherColumns(activity) - 3;
        new AlertDialog.Builder(activity).setTitle("Launcher grid").setSingleChoiceItems(labels, selected, (dialog, which) -> {
            SettingsStore.setLauncherColumns(activity, values[which]);
            dialog.dismiss();
            activity.refreshScreen();
        }).setNegativeButton("Cancel", null).show();
    }
    private static void chooseIconSize(MainActivity activity) {
        String[] labels = {"Small", "Medium", "Large"};
        String[] values = {"small", "medium", "large"};
        int selected = "small".equals(SettingsStore.getLauncherIconSize(activity)) ? 0 : "large".equals(SettingsStore.getLauncherIconSize(activity)) ? 2 : 1;
        new AlertDialog.Builder(activity).setTitle("Launcher icon size").setSingleChoiceItems(labels, selected, (dialog, which) -> {
            SettingsStore.setLauncherIconSize(activity, values[which]);
            dialog.dismiss();
            activity.refreshScreen();
        }).setNegativeButton("Cancel", null).show();
    }
    private static void chooseIconShape(MainActivity activity) {
        String[] labels = {"Rounded", "Circle", "Square"};
        String[] values = {"rounded", "circle", "square"};
        int selected = "circle".equals(SettingsStore.getLauncherIconShape(activity)) ? 1 : "square".equals(SettingsStore.getLauncherIconShape(activity)) ? 2 : 0;
        new AlertDialog.Builder(activity).setTitle("Launcher icon shape").setSingleChoiceItems(labels, selected, (dialog, which) -> {
            SettingsStore.setLauncherIconShape(activity, values[which]);
            dialog.dismiss();
            activity.refreshScreen();
        }).setNegativeButton("Cancel", null).show();
    }
    private static void confirmImport(MainActivity activity) {
        new AlertDialog.Builder(activity).setTitle("Import LockNova configuration?")
                .setMessage("Import replaces the current LockNova theme, accent, launcher preferences, preview stack and custom presets. It does not change wallpapers, Android settings or Android widget bindings.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose backup", (dialog, which) -> activity.startBackupImport()).show();
    }
    private static void confirmReset(MainActivity activity) {
        new AlertDialog.Builder(activity).setTitle("Reset LockNova preferences?")
                .setMessage("This resets the app theme, accent, launcher grid/icon/dock preferences, preview settings and custom presets. It does not modify Android settings, wallpapers, or saved Android widget bindings.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (dialog, which) -> {
                    SettingsStore.reset(activity);
                    EventLog.add(activity, "system", "LockNova preferences reset", "App preferences were reset; system settings and restoration records were preserved.");
                    activity.toast("LockNova preferences reset.");
                    activity.refreshScreen();
                }).show();
    }
}
