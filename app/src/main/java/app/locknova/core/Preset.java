package app.locknova.core;

public final class Preset {
    public final String name;
    public final int accent;
    public final String theme;
    public final boolean builtIn;

    public Preset(String name, int accent, String theme, boolean builtIn) {
        this.name = name;
        this.accent = accent;
        this.theme = "dark".equals(theme) || "light".equals(theme) ? theme : "system";
        this.builtIn = builtIn;
    }
}
