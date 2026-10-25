package app.locknova.screens;

import android.app.WallpaperManager;
import android.app.AlertDialog;
import android.content.Context;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import app.locknova.MainActivity;
import app.locknova.core.EventLog;
import app.locknova.core.SettingsStore;
import app.locknova.system.AnimationManager;
import app.locknova.system.RootSettingsController;
import app.locknova.system.ShizukuBridge;
import app.locknova.system.WallpaperController;
import app.locknova.ui.UiKit;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ModuleScreen {
    private static final int[] ACCENTS = {0xFF7555D9, 0xFF9272EB, 0xFFBB6DD3, 0xFF4E7BCC, 0xFF39947D, 0xFF6A6B78};
    private ModuleScreen() { }

    public static View create(MainActivity activity, String id) {
        UiKit.Palette p = UiKit.palette(activity);
        ScrollView scroll = UiKit.scroll(activity);
        LinearLayout content = UiKit.pageContent(activity);
        scroll.addView(content);
        LinearLayout toolbar = UiKit.row(activity);
        TextView back = UiKit.text(activity, "‹", 28, p.text, false);
        back.setGravity(Gravity.CENTER);
        toolbar.addView(back, new LinearLayout.LayoutParams(UiKit.dp(activity, 34), UiKit.dp(activity, 42)));
        back.setClickable(true);
        back.setOnClickListener(v -> activity.navigate("Customize"));
        LinearLayout titleBlock = UiKit.column(activity);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = UiKit.dp(activity, 7);
        toolbar.addView(titleBlock, titleParams);
        titleBlock.addView(UiKit.text(activity, titleFor(id), 20, p.text, true));
        TextView subtitle = UiKit.text(activity, subtitleFor(id), 11, p.muted, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = UiKit.dp(activity, 3);
        titleBlock.addView(subtitle, subtitleParams);
        content.addView(toolbar);
        UiKit.addGap(content, activity, 10);
        switch (id) {
            case "status": statusPage(content, activity); break;
            case "quick": quickPage(content, activity); break;
            case "lock": wallpaperPage(content, activity, true); break;
            case "home": homePage(content, activity); break;
            case "gestures": gesturesPage(content, activity); break;
            case "system": systemPage(content, activity); break;
            case "appearance": appearancePage(content, activity); break;
            case "navigation": navigationPage(content, activity); break;
            default: addInfo(content, activity, "Unsupported module", "This module is not available in this build."); break;
        }
        return scroll;
    }

    private static void statusPage(LinearLayout content, MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "PREVIEW + OPTIONAL ROOT");
        addInfo(content, activity, "Your status bar, with honest limits", "The sample controls are previews. If root is granted, LockNova can also try Android's stored status-icon visibility setting; ColorOS may ignore or override it.");
        UiKit.section(content, activity, "Live preview");
        LinearLayout preview = UiKit.card(activity);
        preview.setBackground(UiKit.shape(p.surfaceAlt, p.border, 22, activity));
        LinearLayout statusRow = UiKit.row(activity);
        TextView time = UiKit.text(activity, android.text.format.DateFormat.getTimeFormat(activity).format(new Date()), 14, p.text, true);
        statusRow.addView(time, UiKit.weightParams());
        TextView icons = UiKit.text(activity, "▮  ◔  92%", 12, p.text, true);
        statusRow.addView(icons);
        preview.addView(statusRow);
        TextView demo = UiKit.text(activity, "LockNova preview", 21, p.text, true);
        demo.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams demoParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(activity, 100));
        demoParams.topMargin = UiKit.dp(activity, 16);
        preview.addView(demo, demoParams);
        UiKit.addCard(content, activity, preview);
        UiKit.section(content, activity, "Preview controls");
        LinearLayout controls = UiKit.card(activity);
        final String[] position = {"Center"};
        TextView positionButton = UiKit.button(activity, "Clock position · Center", false, v -> {
            position[0] = "Center".equals(position[0]) ? "Left" : "Center".equals(position[0]) ? "Right" : "Center";
            positionButtonText(positionButtonRef[0], position[0]);
            statusRow.removeView(time);
            statusRow.addView(time, "Left".equals(position[0]) ? 0 : statusRow.getChildCount());
        });
        positionButtonRef[0] = positionButton;
        controls.addView(positionButton);
        addGap(controls, activity, 8);
        addSwitch(controls, activity, "Show preview icons", "Changes the sample row only.", true, checked -> icons.setVisibility(checked ? View.VISIBLE : View.INVISIBLE));
        TextView supported = UiKit.text(activity, "Clock relocation, arbitrary icon replacement and global status-bar theming are not exposed as a verified ColorOS operation here.", 12, p.muted, false);
        LinearLayout.LayoutParams supportedParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        supportedParams.topMargin = UiKit.dp(activity, 8);
        controls.addView(supported, supportedParams);
        UiKit.addCard(content, activity, controls);
        TextView system = UiKit.button(activity, "Open Android display settings", false, v -> activity.openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS));
        UiKit.addCard(content, activity, system);
        UiKit.section(content, activity, "Optional root-backed Android setting");
        LinearLayout rootCard = UiKit.card(activity);
        rootCard.addView(UiKit.text(activity, "Hide selected status icons", 14, p.text, true));
        TextView rootNote = UiKit.text(activity, "On Android SystemUI builds that honor it, this writes the secure icon_blacklist setting. LockNova saves the prior value and reads the new value back. It cannot guarantee ColorOS will render the change.", 12, p.muted, false);
        LinearLayout.LayoutParams rootNoteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootNoteParams.topMargin = UiKit.dp(activity, 6);
        rootCard.addView(rootNote, rootNoteParams);
        TextView editIcons = UiKit.button(activity, "Check root & choose icons", true, v -> editStatusIcons(activity));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editParams.topMargin = UiKit.dp(activity, 11);
        rootCard.addView(editIcons, editParams);
        if (SettingsStore.hasRootSettingsBackup(activity)) addRestoreRootButton(rootCard, activity);
        UiKit.addCard(content, activity, rootCard);
    }

    private static final TextView[] positionButtonRef = new TextView[1];
    private static void positionButtonText(TextView button, String value) { if (button != null) button.setText("Clock position · " + value); }

    private static void quickPage(LinearLayout content, MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "PREVIEW + OPTIONAL ROOT");
        addInfo(content, activity, "Quick Settings, with a real system option", "The tile cards below are a preview. On Android builds that honor it, optional root access can move existing connectivity tiles to the front of the stored panel order; ColorOS may ignore or override that setting.");
        UiKit.section(content, activity, "Interactive tile layout preview");
        LinearLayout panel = UiKit.card(activity);
        panel.setBackground(UiKit.shape(p.surfaceAlt, p.border, SettingsStore.getQuickRound(activity), activity));
        GridLayout grid = buildTileGrid(activity, p, 2);
        panel.addView(grid);
        UiKit.addCard(content, activity, panel);
        TextView layoutButton = UiKit.button(activity, "Tile layout · 2 columns", false, v -> {
            int columns = grid.getColumnCount() == 2 ? 3 : 2;
            panel.removeView(grid);
            GridLayout next = buildTileGrid(activity, p, columns);
            panel.addView(next, 0);
            gridHolder[0] = next;
            layoutButtonRef[0].setText("Tile layout · " + columns + " columns");
        });
        gridHolder[0] = grid;
        layoutButtonRef[0] = layoutButton;
        UiKit.addCard(content, activity, layoutButton);
        UiKit.section(content, activity, "Preview style");
        LinearLayout radiusCard = UiKit.card(activity);
        TextView radiusTitle = UiKit.text(activity, "Tile corner radius", 13, p.text, true);
        radiusCard.addView(radiusTitle);
        SeekBar radius = new SeekBar(activity);
        radius.setMax(22);
        radius.setProgress(Math.max(0, Math.min(22, SettingsStore.getQuickRound(activity) - 10)));
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int value = progress + 10;
                SettingsStore.setQuickRound(activity, value);
                panel.setBackground(UiKit.shape(p.surfaceAlt, p.border, value, activity));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekParams.topMargin = UiKit.dp(activity, 7);
        radiusCard.addView(radius, seekParams);
        UiKit.addCard(content, activity, radiusCard);
        LinearLayout limits = UiKit.card(activity);
        limits.addView(UiKit.text(activity, "This preview stores its own layout in LockNova. The separate optional root action may change Android's stored tile order if ColorOS honors it; panel colors and grid dimensions remain preview-only.", 12, p.muted, false));
        UiKit.addCard(content, activity, limits);
        UiKit.section(content, activity, "System shortcut");
        TextView open = UiKit.button(activity, "Open Android connectivity controls", true, v -> activity.openConnectivityPanel());
        UiKit.addCard(content, activity, open);
        TextView openNote = UiKit.text(activity, "This opens Android's internet connectivity panel when available; it does not open or modify the ColorOS tile editor.", 11, p.muted, false);
        content.addView(openNote);
        UiKit.section(content, activity, "Optional root-backed tile order");
        LinearLayout rootTiles = UiKit.card(activity);
        rootTiles.addView(UiKit.text(activity, "Put connectivity tiles first", 14, p.text, true));
        TextView rootTileNote = UiKit.text(activity, "This changes only Android's stored sysui_qs_tiles order: existing Wi-Fi, mobile, Bluetooth and airplane entries move first; every other tile remains in its current relative order. The previous value is saved and verified.", 12, p.muted, false);
        LinearLayout.LayoutParams rootTileNoteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootTileNoteParams.topMargin = UiKit.dp(activity, 6);
        rootTiles.addView(rootTileNote, rootTileNoteParams);
        TextView reorder = UiKit.button(activity, "Review tile reorder", true, v -> confirmQuickTileOrder(activity));
        LinearLayout.LayoutParams reorderParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        reorderParams.topMargin = UiKit.dp(activity, 11);
        rootTiles.addView(reorder, reorderParams);
        if (SettingsStore.hasRootSettingsBackup(activity)) addRestoreRootButton(rootTiles, activity);
        UiKit.addCard(content, activity, rootTiles);
    }

    private static void editStatusIcons(MainActivity activity) {
        activity.executeSystemTask(() -> {
            RootSettingsController.Result current = RootSettingsController.readStatusIcons();
            activity.runOnUiThread(() -> {
                if (!current.success) { Toast.makeText(activity, current.message, Toast.LENGTH_LONG).show(); return; }
                String[] keys = RootSettingsController.STATUS_ICON_KEYS;
                String[] labels = {"Alarm", "Bluetooth", "Mobile signal", "Clock", "Location", "Mute", "Rotation", "Volume", "Wi-Fi"};
                boolean[] checked = new boolean[keys.length];
                for (int i = 0; i < keys.length; i++) checked[i] = hasCsvValue(current.value, keys[i]);
                new AlertDialog.Builder(activity).setTitle("Choose status icons to hide")
                        .setMultiChoiceItems(labels, checked, (dialog, which, selected) -> checked[which] = selected)
                        .setPositiveButton("Review changes", (dialog, which) -> {
                            LinkedHashSet<String> selected = new LinkedHashSet<>();
                            StringBuilder exact = new StringBuilder();
                            for (int i = 0; i < keys.length; i++) if (checked[i]) {
                                selected.add(keys[i]);
                                if (exact.length() > 0) exact.append(", ");
                                exact.append(labels[i]);
                            }
                            String chosen = exact.length() == 0 ? "None of the listed icons (show all supported icons)" : exact.toString();
                            new AlertDialog.Builder(activity).setTitle("Apply status-icon change?")
                                    .setMessage("Root will update Android's secure icon_blacklist setting. Selected to hide: " + chosen + ". The original value is saved before the write and read back after it. ColorOS may ignore or override it.")
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Apply with root", (confirm, button) -> runRootOperation(activity, "Status icons", () -> RootSettingsController.setHiddenStatusIcons(activity, selected)))
                                    .show();
                        }).setNegativeButton("Cancel", null).show();
            });
        });
    }

    private static boolean hasCsvValue(String csv, String key) {
        if (csv == null || csv.trim().isEmpty() || "null".equals(csv.trim())) return false;
        for (String item : csv.split(",")) if (key.equals(item.trim())) return true;
        return false;
    }

    private static void confirmQuickTileOrder(MainActivity activity) {
        new AlertDialog.Builder(activity).setTitle("Reorder Quick Settings tiles?")
                .setMessage("After you confirm, LockNova will use root to move only existing Wi-Fi, mobile-data, Bluetooth and airplane tiles to the front of Android's stored tile order. All other entries keep their relative order. The original setting is saved first and the result is read back. ColorOS may ignore or override the change.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply with root", (dialog, which) -> runRootOperation(activity, "Quick Settings order", () -> RootSettingsController.prioritizeConnectivityTiles(activity)))
                .show();
    }

    private static void addRestoreRootButton(LinearLayout parent, MainActivity activity) {
        TextView restore = UiKit.button(activity, "Review & restore saved SystemUI settings", false, v -> confirmRestoreRootSettings(activity));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = UiKit.dp(activity, 8);
        parent.addView(restore, params);
    }

    private static void confirmRestoreRootSettings(MainActivity activity) {
        org.json.JSONObject backup = SettingsStore.getRootSettingsBackup(activity);
        StringBuilder exact = new StringBuilder();
        for (String key : new String[]{"icon_blacklist", "sysui_qs_tiles"}) {
            if (!backup.has(key)) continue;
            if (exact.length() > 0) exact.append("\n");
            exact.append("secure ").append(key).append(" = ").append(backup.optString(key, "null"));
        }
        new AlertDialog.Builder(activity).setTitle("Restore original SystemUI values?")
                .setMessage("Root will restore these saved values and verify them:\n" + exact)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restore with root", (dialog, which) -> runRootOperation(activity, "SystemUI settings restored", () -> RootSettingsController.restore(activity)))
                .show();
    }

    private interface RootOperation { RootSettingsController.Result run(); }
    private static void runRootOperation(MainActivity activity, String title, RootOperation operation) {
        activity.executeSystemTask(() -> {
            RootSettingsController.Result result = operation.run();
            EventLog.add(activity, result.success ? "system" : "failure", result.success ? title : title + " failed", result.message);
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, result.message, Toast.LENGTH_LONG).show();
                activity.refreshScreen();
            });
        });
    }

    private static final GridLayout[] gridHolder = new GridLayout[1];
    private static final TextView[] layoutButtonRef = new TextView[1];
    private static GridLayout buildTileGrid(MainActivity activity, UiKit.Palette p, int columns) {
        String[] tiles = {"Wi-Fi\nOn", "Bluetooth\nOff", "Flashlight", "Airplane mode", "Location", "Battery saver"};
        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(columns);
        grid.setRowCount((tiles.length + columns - 1) / columns);
        for (int i = 0; i < tiles.length; i++) {
            TextView tile = UiKit.text(activity, tiles[i], 12, i == 0 ? 0xFFFFFFFF : p.text, true);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(UiKit.shape(i == 0 ? p.accent : p.surface, p.border, SettingsStore.getQuickRound(activity), activity));
            tile.setPadding(UiKit.dp(activity, 6), UiKit.dp(activity, 13), UiKit.dp(activity, 6), UiKit.dp(activity, 13));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(i / columns, 1f), GridLayout.spec(i % columns, 1f));
            params.width = 0;
            params.height = UiKit.dp(activity, 58);
            params.setMargins(UiKit.dp(activity, 4), UiKit.dp(activity, 4), UiKit.dp(activity, 4), UiKit.dp(activity, 4));
            grid.addView(tile, params);
        }
        return grid;
    }

    private static void wallpaperPage(LinearLayout content, MainActivity activity, boolean lock) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "ANDROID WALLPAPER API");
        addInfo(content, activity, lock ? "Lock-screen personalization" : "Home-screen personalization", "Wallpaper can be applied through Android. Clock styles, icon styles and launcher layouts shown here are previews, not replacements for ColorOS components.");
        UiKit.section(content, activity, "Wallpaper preview");
        FrameLayout preview = new FrameLayout(activity);
        preview.setBackground(UiKit.shape(p.accentSoft, p.border, 24, activity));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(activity, 270));
        LinearLayout imageStack = UiKit.column(activity);
        imageStack.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        imageStack.setPadding(UiKit.dp(activity, 16), UiKit.dp(activity, 16), UiKit.dp(activity, 16), UiKit.dp(activity, 20));
        if (activity.getSelectedImage() != null) {
            ImageView image = new ImageView(activity);
            image.setImageURI(activity.getSelectedImage());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            View wash = new View(activity);
            wash.setBackground(UiKit.shape(p.accentSoft, 0, 24, activity));
            preview.addView(wash, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        TextView clock = UiKit.text(activity, android.text.format.DateFormat.getTimeFormat(activity).format(new Date()), 36, p.text, true);
        clock.setGravity(Gravity.CENTER);
        TextView date = UiKit.text(activity, DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()), 12, p.text, true);
        date.setGravity(Gravity.CENTER);
        imageStack.addView(clock);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = UiKit.dp(activity, 7);
        imageStack.addView(date, dateParams);
        preview.addView(imageStack, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(preview, previewParams);
        UiKit.addGap(content, activity, 10);
        TextView choose = UiKit.button(activity, "Choose image", true, v -> activity.chooseWallpaper());
        UiKit.addCard(content, activity, choose);
        UiKit.section(content, activity, "Clock style");
        LinearLayout clockCard = UiKit.card(activity);
        clockCard.addView(UiKit.text(activity, "Clock and type controls below affect this LockNova preview only.", 12, p.muted, false));
        final int[] size = {36};
        TextView sizeButton = UiKit.button(activity, "Preview clock size · Medium", false, v -> {
            size[0] = size[0] == 36 ? 28 : size[0] == 28 ? 42 : 36;
            clock.setTextSize(size[0]);
            sizeButtonRef[0].setText("Preview clock size · " + (size[0] == 28 ? "Small" : size[0] == 42 ? "Large" : "Medium"));
        });
        sizeButtonRef[0] = sizeButton;
        LinearLayout.LayoutParams sizeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sizeParams.topMargin = UiKit.dp(activity, 10);
        clockCard.addView(sizeButton, sizeParams);
        UiKit.addCard(content, activity, clockCard);
        int target = lock ? WallpaperManager.FLAG_LOCK : WallpaperManager.FLAG_SYSTEM;
        TextView apply = UiKit.button(activity, lock ? "Apply to lock screen" : "Apply to home screen", true, v -> activity.confirmWallpaperChange(target));
        UiKit.addCard(content, activity, apply);
        if (WallpaperController.hasBackup(activity, lock)) {
            TextView restore = UiKit.button(activity, "Restore previous wallpaper", false, v -> activity.confirmWallpaperRestore(target));
            UiKit.addCard(content, activity, restore);
        }
        TextView systemSettings = UiKit.button(activity, lock ? "Open Android security settings" : "Open launcher settings", false, v -> activity.openSystemSettings(lock ? Settings.ACTION_SECURITY_SETTINGS : Settings.ACTION_HOME_SETTINGS));
        UiKit.addCard(content, activity, systemSettings);
        LinearLayout limits = UiKit.card(activity);
        limits.addView(UiKit.text(activity, lock ? "Android keeps the native lock screen and device security in control. LockNova does not bypass security or claim a global clock replacement." : "LockNova Home hosts real Android widgets. They share one footprint and cycle by swipe; the settings hub does not use mock cards as widgets.", 12, p.muted, false));
        UiKit.addCard(content, activity, limits);
    }

    private static final TextView[] sizeButtonRef = new TextView[1];

    private static void homePage(LinearLayout content, MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "OPTIONAL REAL LAUNCHER");
        addInfo(content, activity, "A customizable Android Home app", "LockNova Home has a searchable app drawer, a user-pinned dock, adjustable grid and icons, and a swipeable stack of real Android app widgets. Choose it in Android Home settings only if you want it as your default.");
        TextView openLauncher = UiKit.button(activity, "Open LockNova Home", true, v -> activity.openLauncher());
        UiKit.addCard(content, activity, openLauncher);
        TextView chooseDefault = UiKit.button(activity, "Choose default home app", false, v -> activity.openSystemSettings(Settings.ACTION_HOME_SETTINGS));
        UiKit.addCard(content, activity, chooseDefault);
        TextView launcherOptions = UiKit.button(activity, "Customize launcher grid and icons", false, v -> activity.navigate("Settings"));
        UiKit.addCard(content, activity, launcherOptions);
        UiKit.section(content, activity, "Real Android widget stack");
        LinearLayout stack = UiKit.card(activity);
        int widgetCount = SettingsStore.getWidgetIds(activity).size();
        stack.addView(UiKit.text(activity, widgetCount == 0 ? "No widgets added yet" : widgetCount + " real widgets in one shared slot", 15, p.text, true));
        TextView stackNote = UiKit.text(activity, "Widgets are bound through Android's AppWidgetHost and retain their providers' live content and tap actions. The selected widget is the only one shown in this shared space.", 12, p.muted, false);
        LinearLayout.LayoutParams stackNoteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stackNoteParams.topMargin = UiKit.dp(activity, 7);
        stack.addView(stackNote, stackNoteParams);
        TextView openStack = UiKit.button(activity, "Open LockNova Home · manage widgets", true, v -> activity.openLauncher());
        LinearLayout.LayoutParams openStackParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        openStackParams.topMargin = UiKit.dp(activity, 11);
        stack.addView(openStack, openStackParams);
        UiKit.addCard(content, activity, stack);
        UiKit.section(content, activity, "Wallpaper");
        addAction(content, activity, "Choose an image", "Select a local image for the home-screen wallpaper.", () -> activity.chooseWallpaper());
        TextView apply = UiKit.button(activity, "Apply selected image to home screen", true, v -> activity.confirmWallpaperChange(WallpaperManager.FLAG_SYSTEM));
        UiKit.addCard(content, activity, apply);
        if (WallpaperController.hasBackup(activity, false)) {
            TextView restore = UiKit.button(activity, "Restore previous home wallpaper", false, v -> activity.confirmWallpaperRestore(WallpaperManager.FLAG_SYSTEM));
            UiKit.addCard(content, activity, restore);
        }
        UiKit.section(content, activity, "App icon style preview");
        LinearLayout icons = UiKit.card(activity);
        LinearLayout iconRow = UiKit.row(activity);
        for (int color : new int[]{p.accent, 0xFF8D73DE, 0xFFB4A8E4, 0xFF6C6A83}) {
            TextView icon = UiKit.text(activity, "✦", 19, 0xFFFFFFFF, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(UiKit.shape(color, 0, 15, activity));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiKit.dp(activity, 48), 1f);
            lp.rightMargin = UiKit.dp(activity, 8);
            iconRow.addView(icon, lp);
        }
        icons.addView(iconRow);
        TextView iconNote = UiKit.text(activity, "The LockNova launcher uses the selected icon size and shape. Other launchers keep their own icon settings.", 11, p.muted, false);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = UiKit.dp(activity, 9);
        icons.addView(iconNote, noteParams);
        UiKit.addCard(content, activity, icons);
        TextView launcher = UiKit.button(activity, "Open launcher settings", false, v -> activity.openSystemSettings(Settings.ACTION_HOME_SETTINGS));
        UiKit.addCard(content, activity, launcher);
    }

    private static void gesturesPage(LinearLayout content, MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "IN-APP GESTURE TEST");
        addInfo(content, activity, "Test a gesture here", "This pad detects touch movement only while LockNova is open. It does not monitor other apps or intercept system navigation.");
        UiKit.section(content, activity, "Gesture pad");
        FrameLayout pad = new FrameLayout(activity);
        pad.setBackground(UiKit.shape(p.accentSoft, p.border, 22, activity));
        TextView message = UiKit.text(activity, "Swipe in any direction", 16, p.text, true);
        message.setGravity(Gravity.CENTER);
        pad.addView(message, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(activity, 170)));
        final float[] start = new float[2];
        pad.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { start[0] = event.getX(); start[1] = event.getY(); return true; }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - start[0];
                float dy = event.getY() - start[1];
                String result;
                if (Math.max(Math.abs(dx), Math.abs(dy)) < UiKit.dp(activity, 28)) result = "Tap detected inside LockNova";
                else if (Math.abs(dx) > Math.abs(dy)) result = dx > 0 ? "Right swipe detected" : "Left swipe detected";
                else result = dy > 0 ? "Down swipe detected" : "Up swipe detected";
                message.setText(result);
                return true;
            }
            return true;
        });
        content.addView(pad);
        UiKit.section(content, activity, "Native settings");
        TextView settings = UiKit.button(activity, "Open Android navigation settings", true, v -> activity.openSystemSettings(Settings.ACTION_SETTINGS));
        UiKit.addCard(content, activity, settings);
        LinearLayout limits = UiKit.card(activity);
        limits.addView(UiKit.text(activity, "ColorOS gesture options vary by firmware. Global gesture changes may require the launcher, a privileged component or accessibility access; LockNova requests none of those for this test.", 12, p.muted, false));
        UiKit.addCard(content, activity, limits);
    }

    private static void systemPage(LinearLayout content, MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "PERMISSION-DEPENDENT");
        addInfo(content, activity, "Root-aware system controls", "LockNova can use approved root access for a small allowlist of Android settings, or Shizuku when root is unavailable. Each system write asks first, saves the original value, reads it back and offers restoration.");
        UiKit.section(content, activity, "Optional root access");
        LinearLayout rootStatus = UiKit.card(activity);
        rootStatus.addView(UiKit.text(activity, "Root is checked only when you ask LockNova to apply or inspect a root-backed setting.", 12, p.muted, false));
        TextView checkRoot = UiKit.button(activity, "Check root access", false, v -> activity.executeSystemTask(() -> {
            app.locknova.system.RootBridge.Result result = app.locknova.system.RootBridge.checkRoot();
            activity.runOnUiThread(() -> Toast.makeText(activity, result.message + (result.success ? " " + result.output : ""), Toast.LENGTH_LONG).show());
        }));
        LinearLayout.LayoutParams rootCheckParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootCheckParams.topMargin = UiKit.dp(activity, 9);
        rootStatus.addView(checkRoot, rootCheckParams);
        UiKit.addCard(content, activity, rootStatus);
        UiKit.section(content, activity, "Shizuku fallback status");
        ShizukuBridge.State state = activity.getShizukuState();
        LinearLayout status = UiKit.card(activity);
        status.addView(UiKit.text(activity, state.message, 14, p.text, true));
        TextView stateDetail = UiKit.text(activity, "Binder: " + (state.binderAvailable ? "available" : "unavailable") + " · Authorization: " + (state.authorized ? "granted" : "required"), 12, p.muted, false);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stateParams.topMargin = UiKit.dp(activity, 6);
        status.addView(stateDetail, stateParams);
        TextView shizukuAction = UiKit.button(activity, state.binderAvailable && !state.authorized ? "Request permission" : "Set up Shizuku", false, v -> activity.requestShizukuAccess());
        LinearLayout.LayoutParams requestParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        requestParams.topMargin = UiKit.dp(activity, 10);
        status.addView(shizukuAction, requestParams);
        UiKit.addCard(content, activity, status);
        UiKit.section(content, activity, "Animation scale");
        LinearLayout operation = UiKit.card(activity);
        operation.addView(UiKit.text(activity, "Reduce window, transition and animator scales to 0.5×", 14, p.text, true));
        TextView explain = UiKit.text(activity, "Before applying, LockNova reads and records each original value. It then applies only these three fixed keys, reads them back and offers restoration. If one fails, it attempts rollback and reports the result.", 12, p.muted, false);
        LinearLayout.LayoutParams explainParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        explainParams.topMargin = UiKit.dp(activity, 7);
        operation.addView(explain, explainParams);
        TextView apply = UiKit.button(activity, "Review and reduce animations", true, v -> activity.confirmAnimationChange(false));
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        applyParams.topMargin = UiKit.dp(activity, 12);
        operation.addView(apply, applyParams);
        if (SettingsStore.hasAnimationBackup(activity)) {
            TextView restore = UiKit.button(activity, "Review and restore saved values", false, v -> activity.confirmAnimationChange(true));
            LinearLayout.LayoutParams restoreParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            restoreParams.topMargin = UiKit.dp(activity, 8);
            operation.addView(restore, restoreParams);
        }
        UiKit.addCard(content, activity, operation);
        TextView unsupported = UiKit.text(activity, "SystemUI icon visibility and existing Quick Settings tile order have optional root-backed Android setting paths on supported builds. ColorOS-specific clock styles, panel recoloring, arbitrary tile creation, immersive mode and global gestures are not claimed as verified here.", 12, p.muted, false);
        content.addView(unsupported);
    }

    private static void appearancePage(LinearLayout content, MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "LOCKNOVA ONLY");
        addInfo(content, activity, "Appearance", "These controls affect LockNova and its previews. They do not change ColorOS themes or other apps.");
        UiKit.section(content, activity, "Theme");
        LinearLayout theme = UiKit.card(activity);
        addThemeButton(theme, activity, "Follow device", "system");
        addThemeButton(theme, activity, "Light", "light");
        addThemeButton(theme, activity, "Dark", "dark");
        UiKit.addCard(content, activity, theme);
        UiKit.section(content, activity, "Accent");
        LinearLayout colors = UiKit.card(activity);
        LinearLayout row = UiKit.row(activity);
        for (int color : ACCENTS) {
            TextView swatch = UiKit.text(activity, color == SettingsStore.getAccent(activity) ? "✓" : "", 15, 0xFFFFFFFF, true);
            swatch.setGravity(Gravity.CENTER);
            swatch.setBackground(UiKit.shape(color, 0, 18, activity));
            swatch.setOnClickListener(v -> { SettingsStore.setAccent(activity, color); EventLog.add(activity, "appearance", "Accent updated", "Changed the LockNova app accent only."); activity.refreshScreen(); });
            swatch.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiKit.dp(activity, 40), 1f);
            lp.rightMargin = UiKit.dp(activity, 6);
            row.addView(swatch, lp);
        }
        colors.addView(row);
        UiKit.addCard(content, activity, colors);
    }

    private static void addThemeButton(LinearLayout parent, MainActivity activity, String label, String value) {
        UiKit.Palette p = UiKit.palette(activity);
        TextView button = UiKit.button(activity, label + (SettingsStore.getTheme(activity).equals(value) ? " · Selected" : ""), SettingsStore.getTheme(activity).equals(value), v -> {
            SettingsStore.setTheme(activity, value);
            EventLog.add(activity, "appearance", "Theme updated", "Changed LockNova's app theme to " + label + ".");
            activity.refreshScreen();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = UiKit.dp(activity, 7);
        parent.addView(button, params);
    }

    private static void navigationPage(LinearLayout content, MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        addPill(content, activity, "ANDROID SETTINGS SHORTCUTS");
        addInfo(content, activity, "Navigation & display", "These buttons open Android-owned settings. They do not claim to switch ColorOS system modes from inside LockNova.");
        UiKit.section(content, activity, "Display");
        addAction(content, activity, "Display settings", "Brightness, display size and screen timeout where offered.", () -> activity.openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS));
        addAction(content, activity, "Sound settings", "Android-owned sound and volume settings.", () -> activity.openSystemSettings(Settings.ACTION_SOUND_SETTINGS));
        addAction(content, activity, "Home app settings", "Choose or configure a launcher if Android exposes the activity.", () -> activity.openSystemSettings(Settings.ACTION_HOME_SETTINGS));
        UiKit.section(content, activity, "Availability");
        LinearLayout note = UiKit.card(activity);
        note.addView(UiKit.text(activity, "Some ColorOS builds route generic settings actions to a broader page or omit a destination. LockNova falls back to Settings and never silently changes a protected option.", 12, p.muted, false));
        UiKit.addCard(content, activity, note);
    }

    private static void addPill(LinearLayout parent, Context c, String text) {
        UiKit.Palette p = UiKit.palette(c);
        TextView pill = UiKit.pill(c, text, p.accentSoft, p.accent);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = UiKit.dp(c, 7);
        parent.addView(pill, params);
    }
    private static void addInfo(LinearLayout parent, Context c, String title, String text) {
        UiKit.Palette p = UiKit.palette(c);
        LinearLayout card = UiKit.card(c);
        card.addView(UiKit.text(c, title, 15, p.text, true));
        TextView body = UiKit.text(c, text, 12, p.muted, false);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = UiKit.dp(c, 6);
        card.addView(body, bodyParams);
        UiKit.addCard(parent, c, card);
    }
    private static void addSwitch(LinearLayout parent, MainActivity activity, String title, String detail, boolean initial, SwitchListener listener) {
        UiKit.Palette p = UiKit.palette(activity);
        LinearLayout row = UiKit.row(activity);
        LinearLayout labels = UiKit.column(activity);
        row.addView(labels, UiKit.weightParams());
        labels.addView(UiKit.text(activity, title, 13, p.text, true));
        TextView subtitle = UiKit.text(activity, detail, 11, p.muted, false);
        LinearLayout.LayoutParams sub = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sub.topMargin = UiKit.dp(activity, 4);
        labels.addView(subtitle, sub);
        Switch toggle = new Switch(activity);
        toggle.setChecked(initial);
        toggle.setOnCheckedChangeListener((button, checked) -> listener.changed(checked));
        row.addView(toggle);
        parent.addView(row);
    }
    private interface SwitchListener { void changed(boolean value); }
    private static void addGap(LinearLayout parent, Context context, int dp) { View gap = new View(context); parent.addView(gap, new LinearLayout.LayoutParams(1, UiKit.dp(context, dp))); }
    private static void addAction(LinearLayout parent, MainActivity activity, String title, String detail, Runnable action) {
        UiKit.Palette p = UiKit.palette(activity);
        LinearLayout card = UiKit.card(activity);
        LinearLayout row = UiKit.row(activity);
        LinearLayout labels = UiKit.column(activity);
        row.addView(labels, UiKit.weightParams());
        labels.addView(UiKit.text(activity, title, 14, p.text, true));
        TextView summary = UiKit.text(activity, detail, 11, p.muted, false);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = UiKit.dp(activity, 4);
        labels.addView(summary, summaryParams);
        row.addView(UiKit.text(activity, "›", 22, p.muted, false));
        card.addView(row);
        card.setClickable(true);
        card.setOnClickListener(v -> action.run());
        UiKit.addCard(parent, activity, card);
    }

    private static String titleFor(String id) {
        switch (id) {
            case "status": return "Status Bar";
            case "quick": return "Quick Settings";
            case "lock": return "Lock Screen";
            case "home": return "Home Screen";
            case "gestures": return "Gestures";
            case "system": return "System UI";
            case "appearance": return "Appearance";
            default: return "Navigation & Display";
        }
    }
    private static String subtitleFor(String id) {
        switch (id) {
            case "status": return "Preview the details · don't fake system access";
            case "quick": return "Concept layout and native controls";
            case "lock": return "Wallpaper API · native lock screen remains in control";
            case "home": return "Stack preview, wallpapers and launcher limits";
            case "gestures": return "Test locally · no background monitoring";
            case "system": return "One verified, reversible Shizuku operation";
            case "appearance": return "LockNova's own look";
            default: return "Safe shortcuts to Android settings";
        }
    }
}
