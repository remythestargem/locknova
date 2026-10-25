package app.locknova.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/** Minimal root bridge. It exposes only a tiny allowlist of Android settings keys, never arbitrary shell commands. */
public final class RootBridge {
    public static final class Result {
        public final boolean success;
        public final String output;
        public final String message;
        Result(boolean success, String output, String message) {
            this.success = success;
            this.output = output == null ? "" : output.trim();
            this.message = message;
        }
    }

    private RootBridge() { }

    public static Result checkRoot() {
        Result result = execute("id");
        boolean granted = result.success && result.output.contains("uid=0");
        return new Result(granted, result.output, granted ? "Root access is available." : "Root access was not granted to LockNova.");
    }

    public static Result read(String namespace, String key) {
        if (!allowed(namespace, key)) return new Result(false, "", "This Android setting is not on LockNova's allowlist.");
        Result root = checkRoot();
        if (!root.success) return root;
        return execute("/system/bin/settings get " + namespace + " " + key);
    }

    public static Result write(String namespace, String key, String value) {
        if (!allowed(namespace, key)) return new Result(false, "", "This Android setting is not on LockNova's allowlist.");
        if (value != null && (value.length() > 4096 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\0') >= 0)) {
            return new Result(false, "", "The setting value is invalid or too long.");
        }
        Result root = checkRoot();
        if (!root.success) return root;
        String command = value == null || "null".equals(value)
                ? "/system/bin/settings delete " + namespace + " " + key
                : "/system/bin/settings put " + namespace + " " + key + " " + quote(value);
        return execute(command);
    }

    private static boolean allowed(String namespace, String key) {
        if ("global".equals(namespace)) {
            return "window_animation_scale".equals(key) || "transition_animation_scale".equals(key) || "animator_duration_scale".equals(key);
        }
        if ("secure".equals(namespace)) return "icon_blacklist".equals(key) || "sysui_qs_tiles".equals(key);
        return false;
    }

    private static String quote(String value) { return "'" + value.replace("'", "'\\''") + "'"; }

    private static Result execute(String command) {
        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            final Process active = process;
            Thread reader = new Thread(() -> {
                try (BufferedReader stream = new BufferedReader(new InputStreamReader(active.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = stream.readLine()) != null) {
                        synchronized (output) { if (output.length() < 8192) output.append(line).append('\n'); }
                    }
                } catch (Exception ignored) { }
            }, "LockNova-root-output");
            reader.setDaemon(true);
            reader.start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                reader.join(500);
                return new Result(false, output.toString(), "Root command timed out; no success was assumed.");
            }
            reader.join(1000);
            int exit = process.exitValue();
            String text;
            synchronized (output) { text = output.toString().trim(); }
            return new Result(exit == 0, text, exit == 0 ? "Root command completed." : "Root command failed (exit " + exit + ").");
        } catch (Exception failure) {
            if (process != null) process.destroyForcibly();
            String message = failure.getMessage();
            return new Result(false, output.toString(), message == null ? "Could not start the root command." : message);
        }
    }
}
