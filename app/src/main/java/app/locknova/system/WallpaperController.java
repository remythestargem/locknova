package app.locknova.system;

import android.app.WallpaperManager;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import app.locknova.core.SettingsStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class WallpaperController {
    public static final class Result {
        public final boolean success;
        public final boolean backupSaved;
        public final String message;
        Result(boolean success, boolean backupSaved, String message) { this.success = success; this.backupSaved = backupSaved; this.message = message; }
    }

    private WallpaperController() { }

    public static Result apply(Context context, Uri image, int which) {
        if (image == null) return new Result(false, false, "Choose an image first.");
        if (which != WallpaperManager.FLAG_SYSTEM && which != WallpaperManager.FLAG_LOCK) return new Result(false, false, "Unsupported wallpaper target.");
        WallpaperManager manager = WallpaperManager.getInstance(context);
        boolean backedUp = backupCurrent(context, manager, which);
        try (InputStream input = context.getContentResolver().openInputStream(image)) {
            if (input == null) return new Result(false, backedUp, "The selected image could not be opened.");
            int before = manager.getWallpaperId(which);
            manager.setStream(input, null, true, which);
            int after = manager.getWallpaperId(which);
            if (after < 0) return new Result(false, backedUp, "Android did not confirm the wallpaper update.");
            if (before == after) return new Result(true, backedUp, "Wallpaper request completed; Android reports the wallpaper ID is unchanged.");
            return new Result(true, backedUp, "Wallpaper applied through Android's WallpaperManager.");
        } catch (Exception failure) {
            String detail = failure.getMessage();
            return new Result(false, backedUp, "Android could not apply this image" + (detail == null ? "." : ": " + detail));
        }
    }

    public static Result restore(Context context, int which) {
        boolean lock = which == WallpaperManager.FLAG_LOCK;
        String path = SettingsStore.getWallpaperBackupPath(context, lock);
        if (path == null) return new Result(false, false, "No saved wallpaper backup is available for this target.");
        File backup = new File(path);
        if (!backup.isFile()) return new Result(false, false, "The saved wallpaper backup is no longer available.");
        try (FileInputStream input = new FileInputStream(backup)) {
            WallpaperManager.getInstance(context).setStream(input, null, true, which);
            if (WallpaperManager.getInstance(context).getWallpaperId(which) < 0) return new Result(false, true, "Android did not confirm restoration; the backup was kept.");
            backup.delete();
            SettingsStore.setWallpaperBackupPath(context, lock, null);
            return new Result(true, true, "Previous wallpaper restored through Android's WallpaperManager.");
        } catch (Exception failure) {
            return new Result(false, true, "Could not restore the saved wallpaper: " + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()));
        }
    }

    public static boolean hasBackup(Context context, boolean lock) {
        String path = SettingsStore.getWallpaperBackupPath(context, lock);
        return path != null && new File(path).isFile();
    }

    private static boolean backupCurrent(Context context, WallpaperManager manager, int which) {
        boolean lock = which == WallpaperManager.FLAG_LOCK;
        if (hasBackup(context, lock)) return true;
        File target = new File(context.getFilesDir(), lock ? "previous_lock_wallpaper.bin" : "previous_home_wallpaper.bin");
        ParcelFileDescriptor descriptor = null;
        try {
            descriptor = manager.getWallpaperFile(which);
            if (descriptor == null) return false;
            try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
                 FileOutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.flush();
            }
            SettingsStore.setWallpaperBackupPath(context, lock, target.getAbsolutePath());
            return true;
        } catch (Exception ignored) {
            if (target.exists()) target.delete();
            return false;
        } finally {
            if (descriptor != null) try { descriptor.close(); } catch (Exception ignored) { }
        }
    }
}
