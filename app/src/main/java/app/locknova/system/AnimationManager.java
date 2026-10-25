package app.locknova.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import app.locknova.core.SettingsStore;
import app.locknova.system.privileged.ILockNovaUserService;
import app.locknova.system.privileged.LockNovaUserService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

public final class AnimationManager {
    private static final String[] KEYS = {"window_animation_scale", "transition_animation_scale", "animator_duration_scale"};

    public static final class Result {
        public final boolean success;
        public final String message;
        public Result(boolean success, String message) { this.success = success; this.message = message; }
    }

    private AnimationManager() { }

    public static Result reduce(Context context) {
        if (RootBridge.checkRoot().success) return reduceWithRoot(context);
        if (!isAuthorized()) return new Result(false, "Root access was not available and Shizuku is not running with LockNova authorization. No system setting was changed.");
        try (RemoteSession session = connect(context)) {
            LinkedHashMap<String, String> current = new LinkedHashMap<>();
            for (String key : KEYS) {
                String value = session.read(key);
                if (!isScale(value)) return new Result(false, "Android did not return a readable animation value for " + key + ". Nothing was changed.");
                current.put(key, value);
            }
            if (!SettingsStore.hasAnimationBackup(context)) {
                JSONObject backup = new JSONObject();
                try { for (Map.Entry<String, String> item : current.entrySet()) backup.put(item.getKey(), item.getValue()); }
                catch (JSONException ignored) { return new Result(false, "Could not prepare a restoration record. Nothing was changed."); }
                SettingsStore.setAnimationBackup(context, backup);
            }
            for (String key : KEYS) {
                try {
                    session.write(key, "0.5");
                    String verified = session.read(key);
                    if (!sameScale(verified, 0.5f)) throw new IllegalStateException("Android returned " + verified + " after the change");
                } catch (Exception failure) {
                    boolean rolledBack = rollback(session, current);
                    return new Result(false, "Animation change failed at " + key + ". " + (rolledBack ? "Earlier values were restored." : "Some values may need manual restoration; use Restore previous values."));
                }
            }
            return new Result(true, "Reduced animation scales to 0.5× and verified all three Android settings. The original values are saved for restoration.");
        } catch (Exception failure) {
            return new Result(false, "Could not connect to the Shizuku UserService: " + safeMessage(failure) + ". No unverified change was reported as successful.");
        }
    }

    private static Result reduceWithRoot(Context context) {
        LinkedHashMap<String, String> current = new LinkedHashMap<>();
        for (String key : KEYS) {
            RootBridge.Result read = RootBridge.read("global", key);
            if (!read.success || !isScale(read.output)) return new Result(false, "Root could not read a valid animation value for " + key + ". Nothing was changed.");
            current.put(key, read.output);
        }
        if (!SettingsStore.hasAnimationBackup(context)) {
            JSONObject backup = new JSONObject();
            try { for (Map.Entry<String, String> item : current.entrySet()) backup.put(item.getKey(), item.getValue()); }
            catch (JSONException ignored) { return new Result(false, "Could not prepare a restoration record. Nothing was changed."); }
            SettingsStore.setAnimationBackup(context, backup);
        }
        for (String key : KEYS) {
            RootBridge.Result write = RootBridge.write("global", key, "0.5");
            RootBridge.Result readback = RootBridge.read("global", key);
            if (!write.success || !readback.success || !sameScale(readback.output, 0.5f)) {
                boolean restored = rollbackRoot(current);
                return new Result(false, "The root animation-scale change failed at " + key + ". " + (restored ? "Original values were restored." : "Use Restore previous values to retry recovery."));
            }
        }
        return new Result(true, "Root set all three animation scales to 0.5× and Android read them back. Original values are saved for restoration; ColorOS may override them later.");
    }

    public static Result restore(Context context) {
        JSONObject backup = SettingsStore.getAnimationBackup(context);
        if (backup.length() == 0) return new Result(false, "No saved original animation values are available to restore.");
        if (RootBridge.checkRoot().success) return restoreWithRoot(context, backup);
        if (!isAuthorized()) return new Result(false, "Root access was not available and Shizuku is not running with LockNova authorization. No system setting was changed.");
        try (RemoteSession session = connect(context)) {
            for (String key : KEYS) {
                String value = backup.optString(key, "");
                if (!isScale(value)) return new Result(false, "The saved value for " + key + " is missing or invalid; it was not written.");
                try {
                    session.write(key, value);
                    String verified = session.read(key);
                    if (!sameScale(verified, Float.parseFloat(value))) return new Result(false, "Android did not verify the restored value for " + key + ". The recovery record was kept.");
                } catch (Exception failure) {
                    return new Result(false, "Could not restore " + key + ": " + safeMessage(failure) + ". The recovery record was kept.");
                }
            }
            SettingsStore.clearAnimationBackup(context);
            return new Result(true, "Restored and verified the three saved animation values.");
        } catch (Exception failure) {
            return new Result(false, "Could not connect to the Shizuku UserService: " + safeMessage(failure) + ". The recovery record was kept.");
        }
    }

    private static Result restoreWithRoot(Context context, JSONObject backup) {
        for (String key : KEYS) {
            String value = backup.optString(key, "");
            if (!isScale(value)) return new Result(false, "The saved value for " + key + " is missing or invalid; it was not written.");
            RootBridge.Result write = RootBridge.write("global", key, value);
            RootBridge.Result read = RootBridge.read("global", key);
            if (!write.success || !read.success || !sameScale(read.output, Float.parseFloat(value))) {
                return new Result(false, "Could not verify root restoration of " + key + ". The recovery record was kept.");
            }
        }
        SettingsStore.clearAnimationBackup(context);
        return new Result(true, "Root restored and verified the three saved animation values.");
    }

    private static boolean rollbackRoot(Map<String, String> values) {
        boolean okay = true;
        for (Map.Entry<String, String> item : values.entrySet()) {
            RootBridge.Result write = RootBridge.write("global", item.getKey(), item.getValue());
            RootBridge.Result read = RootBridge.read("global", item.getKey());
            okay &= write.success && read.success && sameScale(read.output, Float.parseFloat(item.getValue()));
        }
        return okay;
    }

    private static RemoteSession connect(Context context) throws Exception {
        if (!isAuthorized()) throw new SecurityException("Shizuku authorization is no longer available.");
        CountDownLatch connected = new CountDownLatch(1);
        ILockNovaUserService[] remote = new ILockNovaUserService[1];
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                remote[0] = ILockNovaUserService.Stub.asInterface(binder);
                connected.countDown();
            }
            @Override public void onServiceDisconnected(ComponentName name) { remote[0] = null; connected.countDown(); }
        };
        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(new ComponentName(context, LockNovaUserService.class))
                .tag("locknova-animation-scale")
                .version(1)
                .debuggable(false)
                .processNameSuffix("locknova");
        try {
            Shizuku.bindUserService(args, connection);
            if (!connected.await(15, TimeUnit.SECONDS) || remote[0] == null) throw new IllegalStateException("The privileged service did not connect in time.");
            return new RemoteSession(args, connection, remote[0]);
        } catch (Exception failure) {
            try { Shizuku.unbindUserService(args, connection, true); } catch (RuntimeException ignored) { }
            throw failure;
        }
    }

    private static boolean isAuthorized() {
        try { return Shizuku.pingBinder() && !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (RuntimeException ignored) { return false; }
    }

    private static boolean rollback(RemoteSession session, Map<String, String> values) {
        boolean okay = true;
        for (Map.Entry<String, String> item : values.entrySet()) {
            try { session.write(item.getKey(), item.getValue()); okay &= sameScale(session.read(item.getKey()), Float.parseFloat(item.getValue())); }
            catch (Exception ignored) { okay = false; }
        }
        return okay;
    }

    private static boolean isScale(String value) {
        if (value == null || !value.matches("[0-9]+(?:\\.[0-9]+)?")) return false;
        try { float f = Float.parseFloat(value); return f >= 0f && f <= 10f; }
        catch (NumberFormatException ignored) { return false; }
    }
    private static boolean sameScale(String value, float expected) {
        if (!isScale(value)) return false;
        return Math.abs(Float.parseFloat(value) - expected) < 0.001f;
    }
    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty() ? failure.getClass().getSimpleName() : message;
    }

    private static final class RemoteSession implements AutoCloseable {
        final Shizuku.UserServiceArgs args;
        final ServiceConnection connection;
        final ILockNovaUserService remote;
        RemoteSession(Shizuku.UserServiceArgs args, ServiceConnection connection, ILockNovaUserService remote) {
            this.args = args; this.connection = connection; this.remote = remote;
        }
        String read(String key) throws RemoteException { return remote.readAnimationScale(key); }
        void write(String key, String value) throws RemoteException { remote.writeAnimationScale(key, value); }
        @Override public void close() {
            try { Shizuku.unbindUserService(args, connection, true); } catch (RuntimeException ignored) { }
        }
    }
}
