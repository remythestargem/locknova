package app.locknova.core;

/** Stateless paging rules for LockNova's single-footprint, swipeable widget stack. */
public final class WidgetStackLogic {
    private WidgetStackLogic() { }

    public static int nextIndex(int current, int direction, int count) {
        if (count <= 1 || direction == 0) return 0;
        int normalized = ((current % count) + count) % count;
        return (normalized + (direction > 0 ? 1 : count - 1)) % count;
    }

    public static String positionLabel(int index, int count) {
        if (count <= 0) return "No widgets";
        int safeIndex = Math.max(0, Math.min(index, count - 1));
        return (safeIndex + 1) + " / " + count;
    }
}
