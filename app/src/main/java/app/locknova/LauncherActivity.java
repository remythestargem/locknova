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
import android.view.MotionEvent;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LauncherActivity extends Activity {
    private static final int APP_WIDGET_HOST_ID = 0x4C4E;
    private static final int REQUEST_BIND_WIDGET = 9201;
    private static final int REQUEST_CONFIGURE_WIDGET = 9202;
    private final Handler clockHandler = new Handler();
    private AppWidgetHost widgetHost;
    private AppWidgetManager widgetManager;
    private boolean drawerOpen;
    private String drawerQuery = "";
    private FrameLayout widgetDeck;
    private int widgetIndex;
    private float touchX;
    private float touchY;
    private int pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private ComponentName pendingProvider;
    private TextView clockText;
    private TextView dateText;

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
        outer.setBackgroundColor(Color.TRANSPARENT);
        if (drawerOpen) renderDrawer(outer, p); else renderHome(outer, p);
        setContentView(outer);
    }

    private void renderHome(LinearLayout outer, UiKit.Palette p) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout content = UiKit.column(this);
        content.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 40), UiKit.dp(this, 18), UiKit.dp(this, 16));
        scroll.addView(content);
        outer.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout header = UiKit.row(this);
        LinearLayout identity = UiKit.column(this);
        identity.addView(UiKit.text(this, "LockNova", 22, p.text, true));
        TextView subtitle = UiKit.text(this, "Your launcher, your layout", 11, p.muted, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = UiKit.dp(this, 3);
        identity.addView(subtitle, subLp);
        header.addView(identity, UiKit.weightParams());
        TextView settings = actionPill("Customize", () -> openHub(true));
        header.addView(settings);
        content.addView(header);

        LinearLayout clockCard = UiKit.card(this);
        clockCard.setBackground(UiKit.shape(p.surface, p.border, 24, this));
        clockText = UiKit.text(this, android.text.format.DateFormat.getTimeFormat(this).format(new Date()), 37, p.text, true);
        dateText = UiKit.text(this, DateFormat.getDateInstance(DateFormat.FULL).format(new Date()), 12, p.muted, false);
        clockCard.addView(clockText);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = UiKit.dp(this, 4);
        clockCard.addView(dateText, dateLp);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockLp.topMargin = UiKit.dp(this, 18);
        content.addView(clockCard, clockLp);

        LinearLayout widgetHeading = UiKit.row(this);
        TextView widgetTitle = UiKit.text(this, "Widget stack", 15, p.text, true);
        widgetHeading.addView(widgetTitle, UiKit.weightParams());
        widgetHeading.addView(UiKit.pill(this, "ANDROID WIDGETS", p.accentSoft, p.accent));
        LinearLayout.LayoutParams wh = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wh.topMargin = UiKit.dp(this, 17);
        content.addView(widgetHeading, wh);
        widgetDeck = new FrameLayout(this);
        widgetDeck.setBackground(UiKit.shape(p.surface, p.border, 22, this));
        LinearLayout.LayoutParams deckParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 240));
        deckParams.topMargin = UiKit.dp(this, 8);
        content.addView(widgetDeck, deckParams);
        widgetDeck.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { touchX = event.getX(); touchY = event.getY(); return true; }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - touchX;
                float dy = event.getY() - touchY;
                if (Math.abs(dx) > UiKit.dp(this, 30) && Math.abs(dx) > Math.abs(dy)) cycleWidget(dx < 0 ? 1 : -1);
                return true;
            }
            return true;
        });
        renderWidget(false);
        LinearLayout widgetActions = UiKit.row(this);
        TextView addWidget = actionText("Add widget", this::showWidgetProviders);
        widgetActions.addView(addWidget, UiKit.weightParams());
        TextView manageWidgets = actionText("Manage stack", this::showWidgetStackManager);
        widgetActions.addView(manageWidgets);
        LinearLayout.LayoutParams wa = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        content.addView(widgetActions, wa);

        LinearLayout shortcuts = UiKit.row(this);
        TextView apps = UiKit.button(this, "All apps", true, v -> { drawerOpen = true; render(); });
        shortcuts.addView(apps, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView search = UiKit.button(this, "Search apps", false, v -> { drawerOpen = true; render(); });
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        searchLp.leftMargin = UiKit.dp(this, 8);
        shortcuts.addView(search, searchLp);
        LinearLayout.LayoutParams shortcutLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shortcutLp.topMargin = UiKit.dp(this, 15);
        content.addView(shortcuts, shortcutLp);
        TextView hint = UiKit.text(this, "Long-press an app to pin it to the dock or open app info.", 11, p.muted, false);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = UiKit.dp(this, 8);
        content.addView(hint, hintLp);

        addDock(outer, p);
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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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
        dock.setBackground(UiKit.shape(p.surface, p.border, 22, this));
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
            widgetDeck.removeAllViews();
            LinearLayout empty = UiKit.column(this);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 15), UiKit.dp(this, 18), UiKit.dp(this, 15));
            UiKit.Palette p = palette();
            empty.addView(UiKit.text(this, "Stack your Android widgets", 16, p.text, true));
            TextView body = UiKit.text(this, "Add app widgets, then swipe left or right to cycle them in this launcher stack.", 12, p.muted, false);
            body.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.topMargin = UiKit.dp(this, 7);
            empty.addView(body, bp);
            TextView add = UiKit.button(this, "Add first widget", true, v -> showWidgetProviders());
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ap.topMargin = UiKit.dp(this, 12);
            empty.addView(add, ap);
            widgetDeck.addView(empty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return;
        }
        widgetIndex = (widgetIndex % ids.size() + ids.size()) % ids.size();
        int id = ids.get(widgetIndex);
        AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
        if (info == null) {
            SettingsStore.setWidgetIds(this, removeId(ids, id));
            renderWidget(false);
            return;
        }
        AppWidgetHostView hostView;
        try { hostView = widgetHost.createView(this, id, info); }
        catch (RuntimeException failure) {
            TextView failed = UiKit.text(this, "This widget is currently unavailable. Manage the stack to remove it.", 13, palette().muted, false);
            failed.setGravity(Gravity.CENTER);
            widgetDeck.removeAllViews();
            widgetDeck.addView(failed, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return;
        }
        widgetDeck.setBackground(UiKit.shape(palette().surface, palette().border, 22, this));
        if (animate && widgetDeck.getChildCount() > 0 && SettingsStore.getMotion(this)) {
            View old = widgetDeck.getChildAt(0);
            old.animate().alpha(0f).translationX(-UiKit.dp(this, 15)).setDuration(100).withEndAction(() -> {
                widgetDeck.removeAllViews();
                hostView.setAlpha(0f);
                hostView.setTranslationX(UiKit.dp(this, 15));
                widgetDeck.addView(hostView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                hostView.animate().alpha(1f).translationX(0f).setDuration(220).start();
            }).start();
        } else {
            widgetDeck.removeAllViews();
            widgetDeck.addView(hostView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private void cycleWidget(int step) {
        List<Integer> ids = SettingsStore.getWidgetIds(this);
        if (ids.size() < 2) { if (!ids.isEmpty()) toast("Add another widget to cycle the stack."); return; }
        widgetIndex = (widgetIndex + step + ids.size()) % ids.size();
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
