package app.locknova.screens;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.locknova.MainActivity;
import app.locknova.core.EventLog;
import app.locknova.ui.UiKit;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class ActivityScreen {
    private ActivityScreen() { }

    public static View create(MainActivity activity) {
        UiKit.Palette p = UiKit.palette(activity);
        ScrollView scroll = UiKit.scroll(activity);
        LinearLayout content = UiKit.pageContent(activity);
        scroll.addView(content);
        content.addView(UiKit.text(activity, "Activity", 26, p.text, true));
        LinearLayout heading = UiKit.row(activity);
        TextView intro = UiKit.text(activity, "Local history of LockNova actions", 13, p.muted, false);
        heading.addView(intro, UiKit.weightParams());
        TextView clear = UiKit.text(activity, "Clear", 12, p.accent, true);
        clear.setPadding(UiKit.dp(activity, 10), UiKit.dp(activity, 8), UiKit.dp(activity, 4), UiKit.dp(activity, 8));
        clear.setOnClickListener(v -> new AlertDialog.Builder(activity).setTitle("Clear activity history?")
                .setMessage("This removes locally stored LockNova activity entries only.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear history", (dialog, which) -> { EventLog.clear(activity); activity.refreshScreen(); })
                .show());
        heading.addView(clear);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headingParams.topMargin = UiKit.dp(activity, 5);
        content.addView(heading, headingParams);

        List<EventLog.Event> events = EventLog.list(activity);
        if (events.isEmpty()) {
            UiKit.section(content, activity, "Nothing recorded yet");
            LinearLayout empty = UiKit.card(activity);
            empty.addView(UiKit.text(activity, "When you apply a supported change, use a preset, request permission, or restore a setting, it will appear here.", 13, p.muted, false));
            UiKit.addCard(content, activity, empty);
            return scroll;
        }
        UiKit.section(content, activity, "Latest first");
        for (EventLog.Event event : events) {
            LinearLayout card = UiKit.card(activity);
            LinearLayout row = UiKit.row(activity);
            TextView marker = UiKit.text(activity, markerFor(event.category), 16, colorFor(p, event.category), true);
            row.addView(marker, new LinearLayout.LayoutParams(UiKit.dp(activity, 24), ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout details = UiKit.column(activity);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            detailsParams.leftMargin = UiKit.dp(activity, 7);
            row.addView(details, detailsParams);
            details.addView(UiKit.text(activity, event.title, 14, p.text, true));
            TextView date = UiKit.text(activity, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(event.timestamp)), 10, p.muted, false);
            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateParams.topMargin = UiKit.dp(activity, 4);
            details.addView(date, dateParams);
            TextView message = UiKit.text(activity, event.detail, 12, p.muted, false);
            LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            msgParams.topMargin = UiKit.dp(activity, 8);
            card.addView(row);
            card.addView(message, msgParams);
            UiKit.addCard(content, activity, card);
        }
        return scroll;
    }

    private static String markerFor(String category) { return "failure".equals(category) ? "!" : "system".equals(category) ? "●" : "•"; }
    private static int colorFor(UiKit.Palette p, String category) { return "failure".equals(category) ? 0xFFD15A64 : "system".equals(category) ? p.accent : p.muted; }
}
