package app.locknova;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import app.locknova.core.EventLog;
import app.locknova.core.Preset;
import app.locknova.core.SettingsStore;
import app.locknova.screens.ActivityScreen;
import app.locknova.screens.CustomizeScreen;
import app.locknova.screens.HomeScreen;
import app.locknova.screens.ModuleScreen;
import app.locknova.screens.PresetsScreen;
import app.locknova.screens.SettingsScreen;
import app.locknova.system.AnimationManager;
import app.locknova.system.ShizukuBridge;
import app.locknova.system.WallpaperController;
import app.locknova.ui.UiKit;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements ShizukuBridge.Listener {
    private static final int REQUEST_IMAGE = 9001;
    private static final int REQUEST_EXPORT = 9002;
    private static final int REQUEST_IMPORT = 9003;
    private LinearLayout appRoot;
    private FrameLayout contentHost;
    private String destination = "Home";
    private String moduleId;
    private Uri selectedImage;
    private ShizukuBridge shizuku;
    private ShizukuBridge.State shizukuState;
    private String lastLoggedShizukuState;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            destination = savedInstanceState.getString("destination", "Home");
            moduleId = savedInstanceState.getString("module", null);
            String image = savedInstanceState.getString("selectedImage", null);
            if (image != null) selectedImage = Uri.parse(image);
        } else if (getIntent().getBooleanExtra("openSettings", false)) {
            destination = "Settings";
        }
        shizuku = new ShizukuBridge(this, this);
        shizukuState = shizuku.getState();
        render();
    }

    @Override protected void onStart() {
        super.onStart();
        if (shizuku != null) shizuku.start();
    }
    @Override protected void onResume() {
        super.onResume();
        if (shizuku != null) shizuku.refresh();
    }
    @Override protected void onStop() {
        if (shizuku != null) shizuku.stop();
        super.onStop();
    }
    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("destination", destination);
        out.putString("module", moduleId);
        if (selectedImage != null) out.putString("selectedImage", selectedImage.toString());
        super.onSaveInstanceState(out);
    }

    @Override public void onShizukuStateChanged(ShizukuBridge.State state) {
        runOnUiThread(() -> {
            shizukuState = state;
            String signature = state.binderAvailable + ":" + state.authorized + ":" + state.message;
            if (!signature.equals(lastLoggedShizukuState)) {
                if (lastLoggedShizukuState != null) EventLog.add(this, "shizuku", "Shizuku status changed", state.message);
                lastLoggedShizukuState = signature;
            }
            render();
        });
    }

    public ShizukuBridge.State getShizukuState() { return shizukuState == null ? shizuku.getState() : shizukuState; }
    public Uri getSelectedImage() { return selectedImage; }
    public void executeSystemTask(Runnable task) { io.execute(task); }

    public void openLauncher() {
        try { startActivity(new Intent(this, LauncherActivity.class)); }
        catch (Exception ignored) { toast("LockNova Home could not be opened."); }
    }
    public void navigate(String page) { destination = page; moduleId = null; render(); }
    public void openModule(String id) {
        destination = "Customize";
        moduleId = id;
        EventLog.add(this, "navigation", "Opened " + moduleTitle(id), "Customization module opened.");
        render();
    }
    public void refreshScreen() { render(); }

    private void render() {
        if (isFinishing()) return;
        UiKit.Palette p = UiKit.palette(this);
        getWindow().setStatusBarColor(p.background);
        getWindow().setNavigationBarColor(p.background);
        if (android.os.Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(p.text == 0xFF1D1A27 ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        appRoot = UiKit.column(this);
        appRoot.setBackgroundColor(p.background);
        contentHost = new FrameLayout(this);
        appRoot.addView(contentHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        View page;
        if (moduleId != null) page = ModuleScreen.create(this, moduleId);
        else if ("Customize".equals(destination)) page = CustomizeScreen.create(this);
        else if ("Presets".equals(destination)) page = PresetsScreen.create(this);
        else if ("Activity".equals(destination)) page = ActivityScreen.create(this);
        else if ("Settings".equals(destination)) page = SettingsScreen.create(this);
        else page = HomeScreen.create(this);
        contentHost.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        appRoot.addView(createNavigation(p), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 68)));
        setContentView(appRoot);
    }

    private View createNavigation(UiKit.Palette p) {
        LinearLayout bar = UiKit.row(this);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(UiKit.dp(this, 6), UiKit.dp(this, 7), UiKit.dp(this, 6), UiKit.dp(this, 5));
        bar.setBackground(UiKit.shape(p.surface, p.border, 22, this));
        String[] names = {"Home", "Customize", "Presets", "Activity", "Settings"};
        String[] icons = {"⌂", "◈", "✦", "≋", "⚙"};
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            boolean active = (moduleId != null && "Customize".equals(name)) || (moduleId == null && destination.equals(name));
            LinearLayout item = UiKit.column(this);
            item.setGravity(Gravity.CENTER);
            TextView icon = UiKit.text(this, icons[i], 19, active ? p.accent : p.muted, true);
            icon.setGravity(Gravity.CENTER);
            TextView label = UiKit.text(this, names[i], 10, active ? p.accent : p.muted, active);
            label.setGravity(Gravity.CENTER);
            item.addView(icon);
            item.addView(label);
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener(v -> navigate(name));
            bar.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        LinearLayout wrap = UiKit.column(this);
        wrap.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 4), UiKit.dp(this, 12), UiKit.dp(this, 5));
        wrap.setBackgroundColor(p.background);
        wrap.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return wrap;
    }

    public void requestShizukuAccess() {
        ShizukuBridge.State state = getShizukuState();
        if (state.binderAvailable && !state.authorized) {
            new AlertDialog.Builder(this).setTitle("Allow LockNova to use Shizuku?")
                    .setMessage("This lets LockNova request a small set of documented Android settings operations. It does not grant root, and every system change is confirmed, saved for restoration, and verified.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Continue", (dialog, which) -> shizuku.requestPermission()).show();
            return;
        }
        if (state.authorized) { toast("Shizuku is connected and authorized."); return; }
        new AlertDialog.Builder(this).setTitle("Start Shizuku")
                .setMessage("On Android 11, start Shizuku using Wireless debugging. Shizuku must be started again after a reboot. If ColorOS limits ADB permissions, its Developer options may require Permission monitoring to be disabled.")
                .setNegativeButton("Setup guide", (dialog, which) -> openUrl("https://shizuku.rikka.app/guide/setup/"))
                .setPositiveButton(state.managerInstalled ? "Open Shizuku" : "Get Shizuku", (dialog, which) -> openShizuku())
                .show();
    }

    private void openShizuku() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
            if (launch != null) startActivity(launch);
            else openUrl("https://shizuku.rikka.app/download/");
        } catch (Exception ignored) { openUrl("https://shizuku.rikka.app/download/"); }
    }

    public void confirmAnimationChange(boolean restore) {
        String title = restore ? "Restore previous animations?" : "Reduce system animations?";
        String message = restore
                ? "LockNova will write the three original Android animation-scale values it previously recorded, then read them back to verify restoration."
                : "This changes Android's window, transition and animator scales to 0.5×. LockNova will read and save the original values, apply the change, verify each value and offer restoration. ColorOS may restrict or override these settings."
                + "\n\nAfter you confirm, LockNova will use root if your device grants it; otherwise it will use an active, authorized Shizuku connection. If neither is available, nothing will be changed.";
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(restore ? "Restore" : "Apply", (dialog, which) -> runAnimationOperation(restore)).show();
    }

    private void runAnimationOperation(boolean restore) {
        io.execute(() -> {
            AnimationManager.Result result = restore ? AnimationManager.restore(this) : AnimationManager.reduce(this);
            EventLog.add(this, result.success ? (restore ? "restoration" : "system") : "failure", result.success ? (restore ? "Animations restored" : "Animations reduced") : "Animation operation failed", result.message);
            runOnUiThread(() -> { toast(result.message); render(); });
        });
    }

    public void openSystemSettings(String action) {
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(intent); }
        catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) { toast("Android Settings could not be opened."); }
        }
    }
    public void openConnectivityPanel() {
        try { startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)); }
        catch (Exception e) { openSystemSettings(Settings.ACTION_SETTINGS); }
    }
    public void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception ignored) { toast("No browser is available to open this link."); }
    }
    public void chooseWallpaper() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try { startActivityForResult(intent, REQUEST_IMAGE); }
        catch (Exception ignored) { toast("The system image picker could not be opened."); }
    }
    public void confirmWallpaperChange(int which) {
        if (selectedImage == null) { toast("Choose an image first."); return; }
        String target = which == WallpaperManager.FLAG_LOCK ? "lock-screen" : "home-screen";
        new AlertDialog.Builder(this).setTitle("Change " + target + " wallpaper?")
                .setMessage("LockNova will use Android's WallpaperManager to apply the selected image. It will try to save a local copy of the current wallpaper first; if Android does not expose a readable backup, the new wallpaper can still be applied but restoration may not be available.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply wallpaper", (dialog, button) -> applyWallpaper(which)).show();
    }
    public void applyWallpaper(int which) {
        Uri image = selectedImage;
        if (image == null) { toast("Choose an image first."); return; }
        io.execute(() -> {
            WallpaperController.Result result = WallpaperController.apply(this, image, which);
            EventLog.add(this, result.success ? "system" : "failure", result.success ? "Wallpaper applied" : "Wallpaper failed", result.message + (result.backupSaved ? " Previous wallpaper backup saved." : " No readable previous-wallpaper backup was created."));
            runOnUiThread(() -> { toast(result.message); render(); });
        });
    }
    public void confirmWallpaperRestore(int which) {
        new AlertDialog.Builder(this).setTitle("Restore previous wallpaper?")
                .setMessage("LockNova will restore the private local backup, if it is still available, through Android's WallpaperManager.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restore", (dialog, button) -> io.execute(() -> {
                    WallpaperController.Result result = WallpaperController.restore(this, which);
                    EventLog.add(this, result.success ? "restoration" : "failure", result.success ? "Wallpaper restored" : "Wallpaper restore failed", result.message);
                    runOnUiThread(() -> { toast(result.message); render(); });
                })).show();
    }

    public void startBackupExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "LockNova-config.json");
        try { startActivityForResult(intent, REQUEST_EXPORT); }
        catch (Exception ignored) { toast("The system document picker could not be opened."); }
    }
    public void startBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try { startActivityForResult(intent, REQUEST_IMPORT); }
        catch (Exception ignored) { toast("The system document picker could not be opened."); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_IMAGE) {
            selectedImage = uri;
            try { getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); }
            catch (Exception ignored) { }
            EventLog.add(this, "permission", "Image selected", "A local image was selected for a wallpaper preview.");
            render();
        } else if (requestCode == REQUEST_EXPORT) {
            io.execute(() -> exportConfig(uri));
        } else if (requestCode == REQUEST_IMPORT) {
            io.execute(() -> importConfig(uri));
        }
    }

    private void exportConfig(Uri uri) {
        try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) throw new IllegalStateException("Document could not be created.");
            output.write(SettingsStore.exportConfig(this).toString(2).getBytes(StandardCharsets.UTF_8));
            EventLog.add(this, "backup", "Configuration exported", "LockNova settings and custom presets were backed up locally.");
            runOnUiThread(() -> toast("LockNova configuration exported."));
        } catch (Exception e) { runOnUiThread(() -> toast("Export failed: " + (e.getMessage() == null ? "document unavailable" : e.getMessage()))); }
    }
    private void importConfig(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && text.length() < 500_000) text.append(line);
            JSONObject json = new JSONObject(text.toString());
            if (!SettingsStore.importConfig(this, json)) throw new IllegalArgumentException("This is not a LockNova configuration backup.");
            EventLog.add(this, "backup", "Configuration imported", "LockNova settings and custom presets were restored.");
            runOnUiThread(() -> { toast("LockNova configuration restored."); render(); });
        } catch (Exception e) { runOnUiThread(() -> toast("Import failed: " + (e.getMessage() == null ? "invalid backup" : e.getMessage()))); }
    }

    public void applyPreset(Preset preset) {
        SettingsStore.setAccent(this, preset.accent);
        SettingsStore.setTheme(this, preset.theme);
        EventLog.add(this, "preset", "Preset applied: " + preset.name, "Applied LockNova app appearance and preview accent only; no device system settings were changed.");
        toast("Applied “" + preset.name + "” to LockNova.");
        render();
    }
    public void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    private static String moduleTitle(String id) {
        switch (id) {
            case "status": return "Status Bar";
            case "quick": return "Quick Settings";
            case "lock": return "Lock Screen";
            case "home": return "Home Screen";
            case "gestures": return "Gestures";
            case "system": return "System UI";
            case "appearance": return "Appearance";
            case "navigation": return "Navigation & Display";
            default: return "Customization";
        }
    }
}
