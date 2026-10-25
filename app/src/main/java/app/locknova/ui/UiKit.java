package app.locknova.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.locknova.core.SettingsStore;

public final class UiKit {
    public static final class Palette {
        public final int background, surface, surfaceAlt, text, muted, border, accent, accentSoft, onAccent;
        Palette(Context c) {
            boolean dark = SettingsStore.isDark(c);
            accent = SettingsStore.getAccent(c);
            background = dark ? 0xFF15141B : 0xFFF8F7FC;
            surface = dark ? 0xFF211F29 : 0xFFFFFFFF;
            surfaceAlt = dark ? 0xFF2A2733 : 0xFFF1EFF8;
            text = dark ? 0xFFF5F2FB : 0xFF1D1A27;
            muted = dark ? 0xFFB8B2C5 : 0xFF716C7D;
            border = dark ? 0xFF393543 : 0xFFE6E2EF;
            accentSoft = dark ? 0xFF39304F : 0xFFF0EBFF;
            onAccent = 0xFFFFFFFF;
        }
    }

    private UiKit() { }
    public static Palette palette(Context c) { return new Palette(c); }
    public static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }

    public static LinearLayout column(Context c) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    public static LinearLayout row(Context c) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }
    public static ScrollView scroll(Context c) {
        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(palette(c).background);
        return scroll;
    }
    public static LinearLayout pageContent(Context c) {
        LinearLayout content = column(c);
        content.setPadding(dp(c, 20), dp(c, 12), dp(c, 20), dp(c, 28));
        return content;
    }
    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView view = new TextView(c);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", bold ? Typeface.NORMAL : Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }
    public static GradientDrawable shape(int color, int stroke, float radiusDp, Context c) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) drawable.setStroke(dp(c, 1), stroke);
        return drawable;
    }
    public static LinearLayout card(Context c) {
        Palette p = palette(c);
        LinearLayout card = column(c);
        card.setPadding(dp(c, 16), dp(c, 15), dp(c, 16), dp(c, 15));
        card.setBackground(shape(p.surface, p.border, 22, c));
        return card;
    }
    public static TextView button(Context c, String label, boolean primary, View.OnClickListener listener) {
        Palette p = palette(c);
        TextView button = text(c, label, 14, primary ? p.onAccent : p.text, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(c, 16), dp(c, 13), dp(c, 16), dp(c, 13));
        button.setMinHeight(dp(c, 46));
        button.setBackground(shape(primary ? p.accent : p.surfaceAlt, primary ? 0 : p.border, 16, c));
        button.setClickable(true);
        button.setFocusable(true);
        if (listener != null) button.setOnClickListener(v -> {
            if (SettingsStore.getHaptics(c)) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            listener.onClick(v);
        });
        return button;
    }
    public static TextView pill(Context c, String label, int color, int textColor) {
        TextView v = text(c, label, 11, textColor, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6));
        v.setBackground(shape(color, 0, 20, c));
        return v;
    }
    public static TextView label(Context c, String label) {
        Palette p = palette(c);
        TextView v = text(c, label, 12, p.muted, true);
        v.setLetterSpacing(0.05f);
        return v;
    }
    public static void section(LinearLayout parent, Context c, String title) {
        TextView label = text(c, title, 15, palette(c).text, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(c, 20);
        params.bottomMargin = dp(c, 10);
        parent.addView(label, params);
    }
    public static void addGap(LinearLayout parent, Context c, int heightDp) {
        View gap = new View(c);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(c, heightDp)));
    }
    public static void addCard(LinearLayout parent, Context c, View card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(c, 10);
        parent.addView(card, params);
    }
    public static void addText(LinearLayout parent, Context c, String value, float sp, int color, boolean bold, int bottomDp) {
        TextView t = text(c, value, sp, color, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(c, bottomDp);
        parent.addView(t, params);
    }
    public static LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }
}
