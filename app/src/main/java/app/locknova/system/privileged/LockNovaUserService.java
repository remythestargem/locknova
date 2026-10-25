package app.locknova.system.privileged;

import android.os.Parcel;
import android.os.RemoteException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Runs inside Shizuku's shell-identity UserService process. */
public final class LockNovaUserService extends ILockNovaUserService.Stub {
    private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
            "window_animation_scale",
            "transition_animation_scale",
            "animator_duration_scale"));

    @Override public String readAnimationScale(String key) throws RemoteException {
        requireKey(key);
        return runSettings("get", key, null);
    }

    @Override public void writeAnimationScale(String key, String value) throws RemoteException {
        requireKey(key);
        if (value == null || !value.matches("[0-9]+(?:\\.[0-9]+)?")) throw new RemoteException("Invalid animation scale value.");
        try {
            float scale = Float.parseFloat(value);
            if (scale < 0f || scale > 10f) throw new RemoteException("Animation scale is outside the allowed range.");
        } catch (NumberFormatException invalid) {
            throw new RemoteException("Invalid animation scale value.");
        }
        runSettings("put", key, value);
    }

    private static void requireKey(String key) throws RemoteException {
        if (key == null || !ALLOWED_KEYS.contains(key)) throw new RemoteException("The requested setting is not allowed.");
    }

    private static String runSettings(String operation, String key, String value) throws RemoteException {
        Process process = null;
        try {
            ProcessBuilder builder = value == null
                    ? new ProcessBuilder("/system/bin/settings", operation, "global", key)
                    : new ProcessBuilder("/system/bin/settings", operation, "global", key, value);
            process = builder.start();
            String output = readFully(process.getInputStream()).trim();
            String error = readFully(process.getErrorStream()).trim();
            int code = process.waitFor();
            if (code != 0) throw new RemoteException(error.isEmpty() ? "Android settings command exited with " + code : error);
            return output;
        } catch (RemoteException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new RemoteException(failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
        } finally {
            if (process != null) process.destroy();
        }
    }

    private static String readFully(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 16777115) {
            data.enforceInterface("app.locknova.system.privileged.ILockNovaUserService");
            if (reply != null) reply.writeNoException();
            new Thread(() -> System.exit(0), "locknova-user-service-stop").start();
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}
