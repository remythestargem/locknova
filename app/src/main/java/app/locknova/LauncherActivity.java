package app.locknova;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import app.locknova.core.EventLog;
import app.locknova.core.SettingsStore;
import app.locknova.ui.UiKit;
import app.locknova.ui.WidgetStackFrame;
import app.locknova.core.WidgetStackLogic;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LauncherActivity extends Activity {
    private static final int APP_WIDGET_HOST_ID = 0x4C4E;
    private static final int REQUEST_BIND_WIDGET = 9201;
    private static final int REQUEST_CONFIGURE_WIDGET = 9202;
    private final Handler clockHandler = new Handler();
    private AppWidgetHost widgetHost;
    private AppWidgetManager widgetManager;
    private boolean drawerOpen;
    private String drawerQuery = "";
    private WidgetStackFrame widgetDeck;
    private final Map<Integer, AppWidgetHostView> widgetViews = new HashMap<>();
    private int widgetIndex;
    private int activeWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private int lastCycleStep = 1;
    private int pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private ComponentName pendingProvider;
    private TextView clockText;
    private TextView dateText;
    private TextView widgetPosition;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (clockText != null) clockText.setText(android.text.format.DateFormat.getTimeFormat(LauncherActivity.this).format(new Date()));
            if (dateText != null) dateText.setText(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()));
            clockHandler.postDelayed(this, 30_000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            drawerOpen = savedInstanceState.getBoolean("drawerOpen", false);
            drawerQuery = savedInstanceState.getString("drawerQuery", "");
            widgetIndex = savedInstanceState.getInt("widgetIndex", 0);
            pendingWidgetId = savedInstanceState.getInt("pendingWidgetId", AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        widgetManager = AppWidgetManager.getInstance(this);
        widgetHost = new AppWidgetHost(this, APP_WIDGET_HOST_ID);
        render();
    }

    @Override protected void onStart() {
        super.onStart();
        try { widgetHost.startListening(); } catch (RuntimeException ignored) { }
        clockHandler.post(clockTick);
        if (!drawerOpen) render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (!drawerOpen) render();
    }

    @Override protected void onStop() {
        clockHandler.removeCallbacks(clockTick);
        try { widgetHost.stopListening(); } catch (RuntimeException ignored) { }
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putBoolean("drawerOpen", drawerOpen);
        out.putString("drawerQuery", drawerQuery);
        out.putInt("widgetIndex", widgetIndex);
        out.putInt("pendingWidgetId", pendingWidgetId);
        super.onSaveInstanceState(out);
    }

    @Override public void onBackPressed() {
        if (drawerOpen) { drawerOpen = false; drawerQuery = ""; render(); }
        else super.onBackPressed();
    }

    private UiKit.Palette palette() { return UiKit.palette(this); }

    private void render() {
        UiKit.Palette p = palette();
        LinearLayout outer = UiKit.column(this);
        outer.setBackgroundColor(drawerOpen ? p.background : Color.TRANSPARENT);
        if (drawerOpen) renderDrawer(outer, p); else renderHome(outer, p);
        setContentView(outer);
    }

    private void renderHome(LinearLayout outer, UiKit.Palette p) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout content = UiKit.column(this);
        content.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 46), UiKit.dp(this, 20), UiKit.dp(this, 16));
        scroll.addView(content);
        outer.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout header = UiKit.row(this);
        LinearLayout identity = UiKit.column(this);
        identity.addView(UiKit.text(this, "LockNova", 24, p.text, true));
        TextView subtitle = UiKit.text(this, "Make your home feel like yours", 11, p.muted, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = UiKit.dp(this, 3);
        identity.addView(subtitle, subLp);
        header.addView(identity, UiKit.weightParams());
        header.addView(actionPill("✦  Style", () -> openHub(true)));
        content.addView(header);

        LinearLayout intro = UiKit.column(this);
        TextView greeting = UiKit.text(this, "Your phone,\nyour rules.", 31, p.text, true);
        greeting.setLineSpacing(0, 0.96f);
        intro.addView(greeting);
        TextView introCopy = UiKit.text(this, "A calmer home screen, made to fit your style.", 12, p.muted, false);
        LinearLayout.LayoutParams introCopyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introCopyParams.topMargin = UiKit.dp(this, 7);
        intro.addView(introCopy, introCopyParams);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introParams.topMargin = UiKit.dp(this, 22);
        content.addView(intro, introParams);

        LinearLayout clockCard = UiKit.card(this);
        clockCard.setBackground(UiKit.shape(withAlpha(p.surface, 225), withAlpha(p.border, 160), 26, this));
        clockText = UiKit.text(this, android.text.format.DateFormat.getTimeFormat(this).format(new Date()), 43, p.text, true);
        dateText = UiKit.text(this, DateFormat.getDateInstance(DateFormat.FULL).format(new Date()), 12, p.muted, false);
        clockCard.addView(clockText);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = UiKit.dp(this, 3);
        clockCard.addView(dateText, dateLp);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockLp.topMargin = UiKit.dp(this, 16);
        content.addView(clockCard, clockLp);

        LinearLayout widgetHeading = UiKit.row(this);
        LinearLayout widgetTitleBlock = UiKit.column(this);
        widgetTitleBlock.addView(UiKit.text(this, "Your widget stack", 16, p.text, true));
        TextView stackHint = UiKit.text(this, "One shared space · swipe to switch", 10, p.muted, false);
        LinearLayout.LayoutParams stackHintParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stackHintParams.topMargin = UiKit.dp(this, 3);
        widgetTitleBlock.addView(stackHint, stackHintParams);
        widgetHeading.addView(widgetTitleBlock, UiKit.weightParams());
        widgetPosition = UiKit.pill(this, "0 / 0", withAlpha(p.accentSoft, 240), p.accent);
        widgetHeading.addView(widgetPosition);
        LinearLayout.LayoutParams wh = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wh.topMargin = UiKit.dp(this, 18);
        content.addView(widgetHeading, wh);

        widgetDeck = new WidgetStackFrame(this);
        widgetDeck.setBackground(UiKit.shape(withAlpha(p.surface, 238), withAlpha(p.border, 180), 26, this));
        widgetDeck.setClipToOutline(true);
        widgetDeck.setOnCycleListener(this::cycleWidget);
        LinearLayout.LayoutParams deckParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 232));
        deckParams.topMargin = UiKit.dp(this, 9);
        content.addView(widgetDeck, deckParams);
        renderWidget(false);

        LinearLayout widgetActions = UiKit.row(this);
        widgetActions.addView(actionPill("‹", () -> cycleWidget(-1)));
        TextView addWidget = UiKit.button(this, "+  Add widget", true, v -> showWidgetProviders());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        addParams.leftMargin = UiKit.dp(this, 8);
        addParams.rightMargin = UiKit.dp(this, 8);
        widgetActions.addView(addWidget, addParams);
        widgetActions.addView(actionPill("Manage", this::showWidgetStackManager));
        widgetActions.addView(actionPill("›", () -> cycleWidget(1)));
        LinearLayout.LayoutParams wa = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wa.topMargin = UiKit.dp(this, 7);
        content.addView(widgetActions, wa);

        LinearLayout shortcuts = UiKit.row(this);
        TextView apps = UiKit.button(this, "All apps", true, v -> { drawerOpen = true; render(); });
        shortcuts.addView(apps, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView search = UiKit.button(this, "Search apps", false, v -> { drawerOpen = true; render(); });
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        searchLp.leftMargin = UiKit.dp(this, 9);
        shortcuts.addView(search, searchLp);
        LinearLayout.LayoutParams shortcutLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shortcutLp.topMargin = UiKit.dp(this, 14);
        content.addView(shortcuts, shortcutLp);
        TextView hint = UiKit.text(this, "Long-press an app to pin it to the dock or open app info.", 10, p.muted, false);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = UiKit.dp(this, 8);
        content.addView(hint, hintLp);

        addDock(outer, p);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private void renderDrawer(LinearLayout outer, UiKit.Palette p) {
        LinearLayout top = UiKit.row(this);
        top.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 40), UiKit.dp(this, 18), UiKit.dp(this, 10));
        TextView back = UiKit.text(this, "‹", 28, p.text, true);
        back.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 44)));
        back.setOnClickListener(v -> { drawerOpen = false; drawerQuery = ""; render(); });
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setHint("Search apps");
        search.setTextColor(p.text);
        search.setHintTextColor(p.muted);
        search.setBackground(UiKit.shape(p.surface, p.border, 18, this));
        search.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 8));
        search.setText(drawerQuery);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 46), 1f);
        searchParams.leftMargin = UiKit.dp(this, 8);
        top.addView(search, searchParams);
        outer.addView(top);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = UiKit.column(this);
        content.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 8), UiKit.dp(this, 18), UiKit.dp(this, 22));
        scroll.addView(content);
        outer.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        TextView resultCount = UiKit.text(this, "", 12, p.muted, false);
        content.addView(resultCount);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(SettingsStore.getLauncherColumns(this));
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = UiKit.dp(this, 10);
        content.addView(grid, gridParams);
        populateAppGrid(grid, resultCount, p, drawerQuery);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int before, int count, int after) {
                drawerQuery = s.toString();
                populateAppGrid(grid, resultCount, p, drawerQuery);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        search.setSelection(search.getText().length());
        search.requestFocus();
    }

    private void populateAppGrid(GridLayout grid, TextView count, UiKit.Palette p, String query) {
        grid.removeAllViews();
        List<ResolveInfo> apps = launcherApps();
        ArrayList<ResolveInfo> filtered = new ArrayList<>();
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        for (ResolveInfo info : apps) {
            String label = info.loadLabel(getPackageManager()).toString();
            if (cleanQuery.isEmpty() || label.toLowerCase(Locale.getDefault()).contains(cleanQuery)) filtered.add(info);
        }
        count.setText(filtered.size() + (cleanQuery.isEmpty() ? " apps" : " results"));
        int columns = SettingsStore.getLauncherColumns(this);
        grid.setColumnCount(columns);
        int row = 0;
        int col = 0;
        for (ResolveInfo info : filtered) {
            grid.addView(createAppItem(info, p, true), gridLayoutParams(row, col, columns, p));
            col++;
            if (col == columns) { col = 0; row++; }
        }
        if (filtered.isEmpty()) {
            TextView empty = UiKit.text(this, "No matching apps", 14, p.muted, false);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0, columns));
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = UiKit.dp(this, 100);
            grid.addView(empty, lp);
        }
    }

    private List<ResolveInfo> launcherApps() {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return String.CASE_INSENSITIVE_ORDER.compare(a.loadLabel(getPackageManager()).toString(), b.loadLabel(getPackageManager()).toString());
            }
        });
        return apps;
    }

    private View createAppItem(ResolveInfo info, UiKit.Palette p, boolean allowLongPress) {
        LinearLayout item = UiKit.column(this);
        item.setGravity(Gravity.CENTER);
        int iconSize = "small".equals(SettingsStore.getLauncherIconSize(this)) ? 42 : "large".equals(SettingsStore.getLauncherIconSize(this)) ? 62 : 52;
        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(info.loadIcon(getPackageManager())); }
        catch (Exception ignored) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
        String shape = SettingsStore.getLauncherIconShape(this);
        float radius = "circle".equals(shape) ? iconSize : "square".equals(shape) ? 8 : 16;
        icon.setBackground(UiKit.shape(p.surface, p.border, radius, this));
        icon.setPadding(UiKit.dp(this, 5), UiKit.dp(this, 5), UiKit.dp(this, 5), UiKit.dp(this, 5));
        item.addView(icon, new LinearLayout.LayoutParams(UiKit.dp(this, iconSize), UiKit.dp(this, iconSize)));
        if (SettingsStore.getLauncherLabels(this)) {
            TextView label = UiKit.text(this, info.loadLabel(getPackageManager()).toString(), 10, p.text, false);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = UiKit.dp(this, 5);
            item.addView(label, labelParams);
        }
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> launchApp(info));
        if (allowLongPress) item.setOnLongClickListener(v -> { showAppActions(info); return true; });
        return item;
    }

    private GridLayout.LayoutParams gridLayoutParams(int row, int col, int columns, UiKit.Palette p) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(row, 1f), GridLayout.spec(col, 1f));
        params.width = 0;
        params.height = UiKit.dp(this, "large".equals(SettingsStore.getLauncherIconSize(this)) ? 102 : 91);
        params.setMargins(UiKit.dp(this, 3), UiKit.dp(this, 3), UiKit.dp(this, 3), UiKit.dp(this, 7));
        return params;
    }

    private void launchApp(ResolveInfo info) {
        try {
            Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launch);
        } catch (Exception e) { toast("This app could not be launched."); }
    }

    private void showAppActions(ResolveInfo info) {
        String packageName = info.activityInfo.packageName;
        String label = info.loadLabel(getPackageManager()).toString();
        List<String> pinned = SettingsStore.getLauncherDock(this);
        boolean isPinned = pinned.contains(packageName);
        String pinText = isPinned ? "Remove from dock" : "Pin to dock";
        new AlertDialog.Builder(this).setTitle(label)
                .setItems(new String[]{pinText, "App info", "Open"}, (dialog, which) -> {
                    if (which == 0) {
                        if (isPinned) pinned.remove(packageName);
                        else { if (pinned.size() >= 8) { toast("The dock can hold up to eight apps."); return; } pinned.add(packageName); }
                        SettingsStore.setLauncherDock(this, pinned);
                        render();
                    } else if (which == 1) {
                        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + packageName))); }
                        catch (Exception ignored) { toast("App info could not be opened."); }
                    } else launchApp(info);
                }).show();
    }

    private void addDock(LinearLayout outer, UiKit.Palette p) {
        LinearLayout dock = UiKit.row(this);
        dock.setPadding(UiKit.dp(this, 10), UiKit.dp(this, 8), UiKit.dp(this, 10), UiKit.dp(this, 8));
        dock.setBackground(UiKit.shape(withAlpha(p.surface, 222), withAlpha(p.border, 165), 22, this));
        List<String> pinned = SettingsStore.getLauncherDock(this);
        if (pinned.isEmpty()) {
            TextView empty = UiKit.text(this, "Dock · long-press apps to pin", 11, p.muted, false);
            empty.setGravity(Gravity.CENTER);
            dock.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 36)));
        } else {
            for (String packageName : pinned) {
                ResolveInfo info = findApp(packageName);
                if (info == null) continue;
                ImageView icon = new ImageView(this);
                try { icon.setImageDrawable(info.loadIcon(getPackageManager())); } catch (Exception ignored) { continue; }
                icon.setPadding(UiKit.dp(this, 3), UiKit.dp(this, 3), UiKit.dp(this, 3), UiKit.dp(this, 3));
                icon.setBackground(UiKit.shape(p.surfaceAlt, p.border, 16, this));
                icon.setOnClickListener(v -> launchApp(info));
                icon.setOnLongClickListener(v -> { showAppActions(info); return true; });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 44), 1f);
                lp.leftMargin = UiKit.dp(this, 4);
                lp.rightMargin = UiKit.dp(this, 4);
                dock.addView(icon, lp);
            }
        }
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dockParams.setMargins(UiKit.dp(this, 14), UiKit.dp(this, 5), UiKit.dp(this, 14), UiKit.dp(this, 12));
        outer.addView(dock, dockParams);
    }

    private ResolveInfo findApp(String packageName) {
        for (ResolveInfo info : launcherApps()) if (info.activityInfo.packageName.equals(packageName)) return info;
        return null;
    }

    private TextView actionText(String label, Runnable action) {
        TextView text = UiKit.text(this, label, 12, palette().accent, true);
        text.setPadding(UiKit.dp(this, 5), UiKit.dp(this, 8), UiKit.dp(this, 8), UiKit.dp(this, 8));
        text.setOnClickListener(v -> action.run());
        return text;
    }

    private TextView actionPill(String label, Runnable action) {
        TextView text = UiKit.pill(this, label, palette().accentSoft, palette().accent);
        text.setOnClickListener(v -> action.run());
        return text;
    }

    private void openHub(boolean launcherSettings) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("openSettings", launcherSettings);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void renderWidget(boolean animate) {
        if (widgetDeck == null) return;
        List<Integer> ids = SettingsStore.getWidgetIds(this);
        if (ids.isEmpty()) {
            for (AppWidgetHostView view : new ArrayList<>(widgetViews.values())) {
                view.animate().cancel();
                if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
            }
            widgetDeck.removeAllViews();
            widgetViews.clear();
            activeWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            widgetIndex = 0;
            if (widgetPosition != null) widgetPosition.setText("0 / 0");
            widgetDeck.setContentDescription("Empty widget stack. Add Android widgets here.");
            LinearLayout empty = UiKit.column(this);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 18), UiKit.dp(this, 20), UiKit.dp(this, 18));
            UiKit.Palette p = palette();
            empty.addView(UiKit.text(this, "Stack your Android widgets", 17, p.text, true));
            TextView body = UiKit.text(this, "Add real app widgets. They share this one space; swipe or use the arrows to switch.", 12, p.muted, false);
            body.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.topMargin = UiKit.dp(this, 8);
            empty.addView(body, bp);
            TextView add = UiKit.button(this, "Add first widget", true, v -> showWidgetProviders());
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ap.topMargin = UiKit.dp(this, 13);
            empty.addView(add, ap);
            widgetDeck.addView(empty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            updateDeckHeight(232);
            return;
        }

        boolean removedMissingProvider = false;
        int sharedHeightDp = 232;
        float density = getResources().getDisplayMetrics().density;
        for (int id : new ArrayList<>(ids)) {
            AppWidgetProviderInfo providerInfo = null;
            try { providerInfo = widgetManager.getAppWidgetInfo(id); } catch (RuntimeException ignored) { }
            if (providerInfo == null) {
                ids.remove(Integer.valueOf(id));
                AppWidgetHostView orphan = widgetViews.remove(id);
                if (orphan != null && orphan.getParent() instanceof ViewGroup) ((ViewGroup) orphan.getParent()).removeView(orphan);
                try { widgetHost.deleteAppWidgetId(id); } catch (RuntimeException ignored) { }
                removedMissingProvider = true;
                continue;
            }
            if (providerInfo.minHeight > 0) sharedHeightDp = Math.max(sharedHeightDp, Math.round(providerInfo.minHeight / density));
        }
        if (removedMissingProvider) {
            SettingsStore.setWidgetIds(this, ids);
            widgetIndex = 0;
            renderWidget(false);
            return;
        }
        sharedHeightDp = Math.max(210, Math.min(340, sharedHeightDp));
        updateDeckHeight(sharedHeightDp);
        widgetIndex = ((widgetIndex % ids.size()) + ids.size()) % ids.size();
        int nextId = ids.get(widgetIndex);
        int oldId = activeWidgetId;

        for (Integer knownId : new ArrayList<>(widgetViews.keySet())) {
            if (ids.contains(knownId)) continue;
            AppWidgetHostView stale = widgetViews.remove(knownId);
            if (stale != null && stale.getParent() instanceof ViewGroup) ((ViewGroup) stale.getParent()).removeView(stale);
        }
        for (int child = widgetDeck.getChildCount() - 1; child >= 0; child--) {
            View view = widgetDeck.getChildAt(child);
            if (!(view instanceof AppWidgetHostView)) widgetDeck.removeViewAt(child);
        }

        int widthDp = Math.max(180, Math.round(getResources().getDisplayMetrics().widthPixels / density) - 40);
        for (int id : ids) {
            AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
            AppWidgetHostView hostView = widgetViews.get(id);
            if (hostView == null) {
                try {
                    hostView = widgetHost.createView(this, id, info);
                    widgetViews.put(id, hostView);
                } catch (RuntimeException failure) {
                    TextView failed = UiKit.text(this, "This Android widget could not be loaded. Remove it in Manage stack and add it again.", 13, palette().muted, false);
                    failed.setGravity(Gravity.CENTER);
                    widgetDeck.removeAllViews();
                    widgetDeck.addView(failed, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    if (widgetPosition != null) widgetPosition.setText(WidgetStackLogic.positionLabel(widgetIndex, ids.size()));
                    return;
                }
            }
            if (hostView.getParent() instanceof ViewGroup && hostView.getParent() != widgetDeck) {
                ((ViewGroup) hostView.getParent()).removeView(hostView);
            }
            if (hostView.getParent() == null) {
                widgetDeck.addView(hostView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                Bundle options = new Bundle();
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, sharedHeightDp);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, sharedHeightDp);
                try { hostView.updateAppWidgetSize(options, widthDp, sharedHeightDp, widthDp, sharedHeightDp); } catch (RuntimeException ignored) { }
            }
        }

        AppWidgetProviderInfo activeInfo = widgetManager.getAppWidgetInfo(nextId);
        AppWidgetHostView nextView = widgetViews.get(nextId);
        AppWidgetHostView previousView = widgetViews.get(oldId);
        if (nextView == null) return;
        widgetDeck.setBackground(UiKit.shape(withAlpha(palette().surface, 238), withAlpha(palette().border, 180), 26, this));
        widgetDeck.setContentDescription((activeInfo == null ? "Android widget" : activeInfo.loadLabel(getPackageManager())) + ", " + WidgetStackLogic.positionLabel(widgetIndex, ids.size()) + ". Swipe left or right to switch widgets.");
        if (widgetPosition != null) widgetPosition.setText(WidgetStackLogic.positionLabel(widgetIndex, ids.size()));

        boolean transition = animate && SettingsStore.getMotion(this) && previousView != null && previousView != nextView;
        for (AppWidgetHostView view : widgetViews.values()) {
            view.animate().cancel();
            boolean keepVisible = view == nextView || (transition && view == previousView);
            view.setVisibility(keepVisible ? View.VISIBLE : View.INVISIBLE);
            view.setAlpha(view == nextView && transition ? 0f : 1f);
            view.setTranslationX(0f);
        }
        nextView.bringToFront();
        if (transition) {
            int direction = lastCycleStep > 0 ? 1 : -1;
            float distance = UiKit.dp(this, 24);
            nextView.setTranslationX(direction * distance);
            nextView.animate().alpha(1f).translationX(0f).setDuration(220).start();
            previousView.animate().alpha(0f).translationX(-direction * distance).setDuration(175).withEndAction(() -> {
                previousView.setVisibility(View.INVISIBLE);
                previousView.setAlpha(1f);
                previousView.setTranslationX(0f);
            }).start();
        }
        activeWidgetId = nextId;
    }

    private void updateDeckHeight(int heightDp) {
        if (widgetDeck == null) return;
        ViewGroup.LayoutParams params = widgetDeck.getLayoutParams();
        if (params != null && params.height != UiKit.dp(this, heightDp)) {
            params.height = UiKit.dp(this, heightDp);
            widgetDeck.setLayoutParams(params);
        }
    }

    private void cycleWidget(int step) {
        List<Integer> ids = SettingsStore.getWidgetIds(this);
        if (ids.size() < 2) {
            if (!ids.isEmpty()) toast("Add another Android widget to this shared space to cycle the stack.");
            return;
        }
        lastCycleStep = step;
        widgetIndex = WidgetStackLogic.nextIndex(widgetIndex, step, ids.size());
        renderWidget(true);
    }

    private void showWidgetProviders() {
        List<AppWidgetProviderInfo> providers;
        try { providers = widgetManager.getInstalledProviders(); }
        catch (RuntimeException failure) { toast("Android could not list available widgets."); return; }
        if (providers == null || providers.isEmpty()) { toast("No app widgets are installed on this device."); return; }
        ArrayList<AppWidgetProviderInfo> usable = new ArrayList<>();
        for (AppWidgetProviderInfo info : providers) if (info != null && info.provider != null) usable.add(info);
        Collections.sort(usable, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.loadLabel(getPackageManager()).toString(), b.loadLabel(getPackageManager()).toString()));
        String[] labels = new String[usable.size()];
        for (int i = 0; i < usable.size(); i++) labels[i] = usable.get(i).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle("Choose an app widget")
                .setItems(labels, (dialog, which) -> bindWidget(usable.get(which)))
                .setNegativeButton("Cancel", null).show();
    }

    private void bindWidget(AppWidgetProviderInfo info) {
        if (SettingsStore.getWidgetIds(this).size() >= 8) { toast("The stack can hold up to eight widgets."); return; }
        pendingWidgetId = widgetHost.allocateAppWidgetId();
        pendingProvider = info.provider;
        boolean bound;
        try { bound = widgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, info.provider); }
        catch (RuntimeException failure) { bound = false; }
        if (!bound) {
            Intent bind = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId);
            bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
            try { startActivityForResult(bind, REQUEST_BIND_WIDGET); }
            catch (Exception failure) { discardPendingWidget(); toast("Android did not open the widget permission screen."); }
            return;
        }
        configureOrAdd(pendingWidgetId);
    }

    private void configureOrAdd(int appWidgetId) {
        AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(appWidgetId);
        if (info == null && pendingProvider != null) {
            for (AppWidgetProviderInfo candidate : widgetManager.getInstalledProviders()) if (pendingProvider.equals(candidate.provider)) { info = candidate; break; }
        }
        if (info == null) { discardPendingWidget(); toast("Android did not confirm the selected widget."); return; }
        if (info.configure != null) {
            Intent configure = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(info.configure);
            configure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            try { startActivityForResult(configure, REQUEST_CONFIGURE_WIDGET); }
            catch (Exception failure) { discardPendingWidget(); toast("This widget's setup screen could not be opened."); }
        } else addWidgetToStack(appWidgetId);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BIND_WIDGET) {
            if (resultCode == RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) configureOrAdd(pendingWidgetId);
            else { discardPendingWidget(); toast("Widget permission was not granted."); }
        } else if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            if (resultCode == RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) addWidgetToStack(pendingWidgetId);
            else { discardPendingWidget(); toast("Widget setup was cancelled; it was not added."); }
        }
    }

    private void addWidgetToStack(int id) {
        List<Integer> ids = SettingsStore.getWidgetIds(this);
        if (!ids.contains(id)) ids.add(id);
        SettingsStore.setWidgetIds(this, ids);
        widgetIndex = ids.size() - 1;
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        pendingProvider = null;
        EventLog.add(this, "launcher", "Widget added to stack", "Added an Android app widget to the LockNova launcher stack.");
        toast("Widget added to the LockNova stack.");
        render();
    }

    private void discardPendingWidget() {
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try { widgetHost.deleteAppWidgetId(pendingWidgetId); } catch (RuntimeException ignored) { }
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        pendingProvider = null;
    }

    private void showWidgetStackManager() {
        List<Integer> ids = SettingsStore.getWidgetIds(this);
        if (ids.isEmpty()) { showWidgetProviders(); return; }
        ArrayList<String> labels = new ArrayList<>();
        for (int id : ids) {
            AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
            labels.add(info == null ? "Unavailable widget" : info.loadLabel(getPackageManager()).toString());
        }
        String[] options = labels.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Manage widget stack")
                .setItems(options, (dialog, which) -> {
                    int widgetId = ids.get(which);
                    new AlertDialog.Builder(this).setTitle(options[which])
                            .setItems(new String[]{"Move to top", "Remove from stack"}, (d, action) -> {
                                List<Integer> current = SettingsStore.getWidgetIds(this);
                                if (action == 0) { current.remove(Integer.valueOf(widgetId)); current.add(0, widgetId); widgetIndex = 0; SettingsStore.setWidgetIds(this, current); render(); }
                                else new AlertDialog.Builder(this).setTitle("Remove this widget?")
                                        .setMessage("This removes the widget from LockNova and deletes its Android widget ID. It does not uninstall the app that provides it.")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Remove", (confirm, b) -> { current.remove(Integer.valueOf(widgetId)); SettingsStore.setWidgetIds(this, current); try { widgetHost.deleteAppWidgetId(widgetId); } catch (RuntimeException ignored) { } widgetIndex = 0; EventLog.add(this, "launcher", "Widget removed from stack", "Removed an app widget instance from the LockNova launcher."); render(); })
                                        .show();
                            }).show();
                }).setPositiveButton("Add widget", (dialog, which) -> showWidgetProviders())
                .setNegativeButton("Done", null).show();
    }

    private List<Integer> removeId(List<Integer> old, int id) {
        ArrayList<Integer> result = new ArrayList<>(old);
        result.remove(Integer.valueOf(id));
        return result;
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
