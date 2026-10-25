package app.locknova.system;

import android.content.Context;
import android.content.pm.PackageManager;

import rikka.shizuku.Shizuku;

public final class ShizukuBridge {
    public static final int REQUEST_CODE = 8107;

    public interface Listener { void onShizukuStateChanged(State state); }
    public static final class State {
        public final boolean managerInstalled;
        public final boolean binderAvailable;
        public final boolean authorized;
        public final String message;
        State(boolean installed, boolean available, boolean authorized, String message) {
            this.managerInstalled = installed;
            this.binderAvailable = available;
            this.authorized = authorized;
            this.message = message;
        }
    }

    private final Context appContext;
    private final Listener listener;
    private boolean registered;
    private State last;

    private final Shizuku.OnBinderReceivedListener binderReceived = this::refresh;
    private final Shizuku.OnBinderDeadListener binderDead = () -> publish(readState());
    private final Shizuku.OnRequestPermissionResultListener permissionResult = (requestCode, grantResult) -> {
        if (requestCode == REQUEST_CODE) refresh();
    };

    public ShizukuBridge(Context context, Listener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
    }

    public void start() {
        if (registered) return;
        registered = true;
        Shizuku.addBinderDeadListener(binderDead);
        Shizuku.addRequestPermissionResultListener(permissionResult);
        Shizuku.addBinderReceivedListenerSticky(binderReceived);
        refresh();
    }

    public void stop() {
        if (!registered) return;
        registered = false;
        Shizuku.removeBinderReceivedListener(binderReceived);
        Shizuku.removeBinderDeadListener(binderDead);
        Shizuku.removeRequestPermissionResultListener(permissionResult);
    }

    public State getState() {
        if (last != null) return last;
        return readState();
    }

    public void requestPermission() {
        State state = readState();
        if (!state.binderAvailable) return;
        try {
            if (!Shizuku.isPreV11() && !state.authorized) Shizuku.requestPermission(REQUEST_CODE);
        } catch (RuntimeException ignored) { refresh(); }
    }

    public boolean isAuthorizedNow() {
        try { return Shizuku.pingBinder() && !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (RuntimeException ignored) { return false; }
    }

    public void refresh() { publish(readState()); }

    private State readState() {
        boolean installed;
        try {
            appContext.getPackageManager().getApplicationInfo("moe.shizuku.privileged.api", 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) { installed = false; }
        boolean available = false;
        boolean authorized = false;
        String message;
        try {
            available = Shizuku.pingBinder();
            if (available) {
                if (Shizuku.isPreV11()) message = "Shizuku API version is too old";
                else {
                    authorized = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
                    message = authorized ? "Connected and authorized" : "Connected · permission needed";
                }
            } else message = installed ? "Shizuku is installed but not running" : "Shizuku is not detected";
        } catch (RuntimeException failure) {
            available = false;
            authorized = false;
            message = "Shizuku connection is unavailable";
        }
        return new State(installed, available, authorized, message);
    }

    private void publish(State state) {
        boolean changed = last == null || last.managerInstalled != state.managerInstalled || last.binderAvailable != state.binderAvailable || last.authorized != state.authorized || !last.message.equals(state.message);
        last = state;
        if (changed && listener != null) listener.onShizukuStateChanged(state);
    }
}
