package app.locknova.screens;

import android.app.Activity;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.locknova.MainActivity;
import app.locknova.core.EventLog;
import app.locknova.core.SettingsStore;
import app.locknova.system.ShizukuBridge;
import app.locknova.system.WallpaperController;
import app.locknova.ui.UiKit;
import app.locknova.ui.WidgetStackView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class HomeScreen {
    private HomeScreen() { }

    public static View create(MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        ScrollView scroll = UiKit.scroll(activity);
        LinearLayout content = UiKit.pageContent(activity);
        scroll.addView(content);

        LinearLayout top = UiKit.row(activity);
        LinearLayout brand = UiKit.column(activity);
        TextView name = UiKit.text(activity, "LockNova", 25, p.text, true);
        TextView subtitle = UiKit.text(activity, "ColorOS, with room to make it yours", 12, p.muted, false);
        brand.addView(name);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = UiKit.dp(activity, 4);
        brand.addView(subtitle, subParams);
        top.addView(brand, UiKit.weightParams());
        TextView date = UiKit.text(activity, DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()), 11, p.muted, true);
        date.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(date);
        content.addView(top);

        UiKit.addGap(content, activity, 18);
        LinearLayout hero = UiKit.card(activity);
        hero.setBackground(UiKit.shape(p.accentSoft, p.border, 26, activity));
        TextView eyebrow = UiKit.text(activity, "PERSONALIZATION, WITH HONEST LIMITS", 10, p.accent, true);
        eyebrow.setLetterSpacing(0.08f);
        hero.addView(eyebrow);
        UiKit.addGap(hero, activity, 9);
        hero.addView(UiKit.text(activity, "Your phone,\nyour rules.", 26, p.text, true));
        TextView heroBody = UiKit.text(activity, "Preview freely. Apply only what Android and ColorOS allow.", 13, p.muted, false);
        LinearLayout.LayoutParams heroBodyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroBodyParams.topMargin = UiKit.dp(activity, 9);
        hero.addView(heroBody, heroBodyParams);
        UiKit.addCard(content, activity, hero);

        UiKit.section(content, activity, "LockNova Home");
        LinearLayout launcherCard = UiKit.card(activity);
        launcherCard.addView(UiKit.text(activity, "A real, optional home-screen launcher", 15, p.text, true));
        TextView launcherCopy = UiKit.text(activity, "Open apps from a searchable drawer, pin a dock, customize the grid, and stack real Android widgets.", 12, p.muted, false);
        LinearLayout.LayoutParams launcherCopyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        launcherCopyParams.topMargin = UiKit.dp(activity, 6);
        launcherCard.addView(launcherCopy, launcherCopyParams);
        TextView openLauncher = UiKit.button(activity, "Open LockNova Home", true, v -> activity.openLauncher());
        LinearLayout.LayoutParams launcherButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        launcherButtonParams.topMargin = UiKit.dp(activity, 11);
        launcherCard.addView(openLauncher, launcherButtonParams);
        TextView defaultLauncher = UiKit.button(activity, "Choose default home app", false, v -> activity.openSystemSettings(Settings.ACTION_HOME_SETTINGS));
        LinearLayout.LayoutParams defaultButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        defaultButtonParams.topMargin = UiKit.dp(activity, 7);
        launcherCard.addView(defaultLauncher, defaultButtonParams);
        UiKit.addCard(content, activity, launcherCard);

        UiKit.section(content, activity, "System bridge");
        ShizukuBridge.State state = activity.getShizukuState();
        LinearLayout shizukuCard = UiKit.card(activity);
        LinearLayout stateRow = UiKit.row(activity);
        TextView dot = UiKit.text(activity, state.authorized ? "●" : state.binderAvailable ? "●" : "○", 16, state.authorized ? 0xFF319B73 : state.binderAvailable ? 0xFFE69B36 : p.muted, true);
        stateRow.addView(dot);
        LinearLayout stateCopy = UiKit.column(activity);
        LinearLayout.LayoutParams stateCopyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        stateCopyParams.leftMargin = UiKit.dp(activity, 10);
        stateRow.addView(stateCopy, stateCopyParams);
        stateCopy.addView(UiKit.text(activity, "Shizuku", 15, p.text, true));
        TextView stateMessage = UiKit.text(activity, state.message, 12, p.muted, false);
        LinearLayout.LayoutParams stateMsgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stateMsgParams.topMargin = UiKit.dp(activity, 4);
        stateCopy.addView(stateMessage, stateMsgParams);
        shizukuCard.addView(stateRow);
        TextView shizukuExplain = UiKit.text(activity, "ADB/Shizuku access is not root. Each supported operation is checked and verified separately.", 12, p.muted, false);
        LinearLayout.LayoutParams explainParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        explainParams.topMargin = UiKit.dp(activity, 11);
        shizukuCard.addView(shizukuExplain, explainParams);
        TextView action = UiKit.button(activity, state.authorized ? "Review connection" : state.binderAvailable ? "Request LockNova permission" : "Set up Shizuku", true, v -> activity.requestShizukuAccess());
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionParams.topMargin = UiKit.dp(activity, 13);
        shizukuCard.addView(action, actionParams);
        UiKit.addCard(content, activity, shizukuCard);

        UiKit.section(content, activity, "This device");
        LinearLayout device = UiKit.card(activity);
        String maker = Build.MANUFACTURER == null ? "Android device" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "Model unavailable" : Build.MODEL;
        device.addView(UiKit.text(activity, maker + " · " + model, 15, p.text, true));
        TextView androidVersion = UiKit.text(activity, "Android " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT, 12, p.muted, false);
        LinearLayout.LayoutParams androidParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        androidParams.topMargin = UiKit.dp(activity, 6);
        device.addView(androidVersion, androidParams);
        TextView build = UiKit.text(activity, "ColorOS behavior depends on the device build; LockNova does not assume a specific firmware capability.", 12, p.muted, false);
        LinearLayout.LayoutParams buildParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buildParams.topMargin = UiKit.dp(activity, 8);
        device.addView(build, buildParams);
        UiKit.addCard(content, activity, device);

        UiKit.section(content, activity, "Restoration");
        boolean animationBackup = SettingsStore.hasAnimationBackup(activity);
        boolean homeWallpaperBackup = WallpaperController.hasBackup(activity, false);
        boolean lockWallpaperBackup = WallpaperController.hasBackup(activity, true);
        LinearLayout restore = UiKit.card(activity);
        if (animationBackup) {
            restore.addView(UiKit.text(activity, "System animation values saved", 14, p.text, true));
            TextView detail = UiKit.text(activity, "A previous LockNova operation recorded original values. They may have changed since then.", 12, p.muted, false);
            LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            d.topMargin = UiKit.dp(activity, 5);
            restore.addView(detail, d);
            TextView restoreButton = UiKit.button(activity, "Restore animations", false, v -> activity.confirmAnimationChange(true));
            LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            b.topMargin = UiKit.dp(activity, 10);
            restore.addView(restoreButton, b);
        } else {
            restore.addView(UiKit.text(activity, "No saved system changes", 14, p.text, true));
            TextView detail = UiKit.text(activity, "Only supported system operations appear here. App appearance previews do not count as device changes.", 12, p.muted, false);
            LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            d.topMargin = UiKit.dp(activity, 5);
            restore.addView(detail, d);
        }
        if (homeWallpaperBackup || lockWallpaperBackup) {
            TextView wallpaper = UiKit.text(activity, "A local previous-wallpaper backup is available in the Lock Screen or Home Screen module.", 12, p.muted, false);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wp.topMargin = UiKit.dp(activity, 9);
            restore.addView(wallpaper, wp);
        }
        UiKit.addCard(content, activity, restore);

        UiKit.section(content, activity, "In-app widget layout preview");
        LinearLayout stackCard = UiKit.card(activity);
        stackCard.addView(new WidgetStackView(activity));
        UiKit.addCard(content, activity, stackCard);

        UiKit.section(content, activity, "Explore modules");
        addShortcut(content, activity, "Status Bar", "Clock and icon layout preview", "status", "◉");
        addShortcut(content, activity, "Quick Settings", "Tile layout preview + Android connectivity controls", "quick", "▦");
        addShortcut(content, activity, "Home Screen", "Real launcher, widget stack, wallpaper and launcher settings", "home", "⌂");

        List<EventLog.Event> events = EventLog.list(activity);
        if (!events.isEmpty()) {
            UiKit.section(content, activity, "Recent activity");
            for (int i = 0; i < events.size() && i < 3; i++) {
                EventLog.Event event = events.get(i);
                LinearLayout item = UiKit.card(activity);
                item.addView(UiKit.text(activity, event.title, 13, p.text, true));
                TextView detail = UiKit.text(activity, event.detail, 11, p.muted, false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = UiKit.dp(activity, 5);
                item.addView(detail, lp);
                UiKit.addCard(content, activity, item);
            }
        }
        return scroll;
    }

    private static void addShortcut(LinearLayout parent, MainActivity activity, String title, String detail, String id, String icon) {
        UiKit.Palette p = UiKit.palette(activity);
        LinearLayout card = UiKit.card(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        TextView glyph = UiKit.text(activity, icon, 20, p.accent, true);
        glyph.setGravity(Gravity.CENTER);
        glyph.setBackground(UiKit.shape(p.accentSoft, 0, 16, activity));
        card.addView(glyph, new LinearLayout.LayoutParams(UiKit.dp(activity, 44), UiKit.dp(activity, 44)));
        LinearLayout labels = UiKit.column(activity);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.leftMargin = UiKit.dp(activity, 12);
        card.addView(labels, labelsParams);
        labels.addView(UiKit.text(activity, title, 14, p.text, true));
        TextView description = UiKit.text(activity, detail, 11, p.muted, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = UiKit.dp(activity, 3);
        labels.addView(description, dp);
        card.setClickable(true);
        card.setOnClickListener(v -> activity.openModule(id));
        UiKit.addCard(parent, activity, card);
    }
}
