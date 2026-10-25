package app.locknova.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.locknova.core.SettingsStore;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class WidgetStackView extends LinearLayout {
    private final Context context;
    private final UiKit.Palette palette;
    private final FrameLayout deck;
    private final TextView positionLabel;
    private List<String> stack;
    private int index;
    private float downX;
    private float downY;

    public WidgetStackView(Context context) {
        super(context);
        this.context = context;
        this.palette = UiKit.palette(context);
        setOrientation(VERTICAL);
        stack = SettingsStore.getWidgetStack(context);

        LinearLayout heading = UiKit.row(context);
        TextView title = UiKit.text(context, "Widget stack", 16, palette.text, true);
        heading.addView(title, UiKit.weightParams());
        heading.addView(UiKit.pill(context, "CONCEPT PREVIEW", palette.accentSoft, palette.accent));
        addView(heading);
        TextView subtitle = UiKit.text(context, "Try card layouts here · host real widgets in LockNova Home", 12, palette.muted, false);
        LayoutParams subtitleParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = UiKit.dp(context, 6);
        addView(subtitle, subtitleParams);

        deck = new FrameLayout(context);
        deck.setClipChildren(false);
        deck.setClipToPadding(false);
        deck.setBackground(UiKit.shape(palette.surfaceAlt, palette.border, SettingsStore.getQuickRound(context), context));
        LayoutParams deckParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(context, 140));
        deckParams.topMargin = UiKit.dp(context, 13);
        addView(deck, deckParams);
        deck.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { downX = event.getX(); downY = event.getY(); return true; }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > UiKit.dp(context, 32) && Math.abs(dx) > Math.abs(dy)) cycle(dx < 0 ? 1 : -1);
                return true;
            }
            return true;
        });

        LinearLayout controls = UiKit.row(context);
        LayoutParams controlsParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsParams.topMargin = UiKit.dp(context, 10);
        addView(controls, controlsParams);
        positionLabel = UiKit.text(context, "", 12, palette.muted, true);
        controls.addView(positionLabel, UiKit.weightParams());
        TextView reorder = smallAction("Reorder", () -> showReorderDialog());
        TextView edit = smallAction("Edit stack", () -> showEditDialog());
        controls.addView(reorder);
        controls.addView(edit);
        renderCard(false);
    }

    private TextView smallAction(String label, Runnable action) {
        TextView text = UiKit.text(context, label, 12, palette.accent, true);
        text.setPadding(UiKit.dp(context, 8), UiKit.dp(context, 8), UiKit.dp(context, 4), UiKit.dp(context, 8));
        text.setClickable(true);
        text.setFocusable(true);
        text.setOnClickListener(v -> action.run());
        return text;
    }

    private void cycle(int direction) {
        if (stack.size() < 2) return;
        index = (index + direction + stack.size()) % stack.size();
        renderCard(true);
    }

    private void renderCard(boolean animate) {
        if (stack.isEmpty()) stack.add("Clock");
        index = Math.max(0, Math.min(index, stack.size() - 1));
        View card = cardFor(stack.get(index));
        if (animate && deck.getChildCount() > 0) {
            View old = deck.getChildAt(0);
            float sign = 1f;
            old.animate().alpha(0f).translationX(-UiKit.dp(context, 18) * sign).setDuration(110).withEndAction(() -> {
                deck.removeAllViews();
                card.setAlpha(0f);
                card.setTranslationX(UiKit.dp(context, 18) * sign);
                deck.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                card.animate().alpha(1f).translationX(0f).setDuration(SettingsStore.getMotion(context) ? 220 : 1).start();
            }).start();
        } else {
            deck.removeAllViews();
            deck.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        positionLabel.setText((index + 1) + " / " + stack.size());
    }

    private View cardFor(String kind) {
        LinearLayout layout = UiKit.column(context);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(UiKit.dp(context, 20), UiKit.dp(context, 16), UiKit.dp(context, 20), UiKit.dp(context, 16));
        String top;
        String body;
        switch (kind) {
            case "Clock":
                top = android.text.format.DateFormat.getTimeFormat(context).format(new Date());
                body = DateFormat.getDateInstance(DateFormat.FULL).format(new Date());
                break;
            case "Calendar":
                top = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());
                body = "No calendar data connected · preview only";
                break;
            case "Weather":
                top = "Weather layout";
                body = "Preview only · no weather provider connected";
                break;
            case "Music":
                top = "Media layout";
                body = "Preview only · no player data connected";
                break;
            default:
                top = "Notes layout";
                body = "Preview only · your launcher owns real widgets";
                break;
        }
        TextView kindLabel = UiKit.text(context, kind.toUpperCase(), 10, palette.accent, true);
        kindLabel.setLetterSpacing(0.08f);
        TextView primary = UiKit.text(context, top, "Clock".equals(kind) ? 30 : 20, palette.text, true);
        TextView secondary = UiKit.text(context, body, 12, palette.muted, false);
        LinearLayout.LayoutParams p1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p1.bottomMargin = UiKit.dp(context, 7);
        layout.addView(kindLabel, p1);
        layout.addView(primary);
        LinearLayout.LayoutParams p2 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p2.topMargin = UiKit.dp(context, 5);
        layout.addView(secondary, p2);
        return layout;
    }

    private void showEditDialog() {
        String[] all = {"Clock", "Calendar", "Weather", "Music", "Notes"};
        boolean[] checked = new boolean[all.length];
        for (int i = 0; i < all.length; i++) checked[i] = stack.contains(all[i]);
        new AlertDialog.Builder(context).setTitle("Choose stack cards")
                .setMultiChoiceItems(all, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    ArrayList<String> next = new ArrayList<>();
                    for (String existing : stack) for (int i = 0; i < all.length; i++) if (checked[i] && all[i].equals(existing) && !next.contains(existing)) next.add(existing);
                    for (int i = 0; i < all.length; i++) if (checked[i] && !next.contains(all[i])) next.add(all[i]);
                    if (next.isEmpty()) next.add("Clock");
                    stack = next;
                    index = 0;
                    SettingsStore.setWidgetStack(context, stack);
                    renderCard(SettingsStore.getMotion(context));
                }).show();
    }

    private void showReorderDialog() {
        if (stack.size() < 2) return;
        String[] labels = new String[stack.size()];
        for (int i = 0; i < stack.size(); i++) labels[i] = (i == 0 ? "Top · " : "") + stack.get(i);
        new AlertDialog.Builder(context).setTitle("Move a card to the top")
                .setItems(labels, (dialog, selected) -> {
                    String chosen = stack.remove(selected);
                    stack.add(0, chosen);
                    index = 0;
                    SettingsStore.setWidgetStack(context, stack);
                    renderCard(true);
                }).show();
    }
}
