package app.locknova.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/** A single shared widget footprint that cycles its active Android widget on horizontal swipes. */
public final class WidgetStackFrame extends FrameLayout {
    public interface OnCycleListener {
        void onCycle(int direction);
    }

    private final int touchSlop;
    private float downX;
    private float downY;
    private OnCycleListener cycleListener;

    public WidgetStackFrame(Context context) {
        this(context, null);
    }

    public WidgetStackFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClipToPadding(false);
    }

    public void setOnCycleListener(OnCycleListener listener) {
        cycleListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (cycleListener != null && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            if (cycleListener != null && Math.abs(dx) > touchSlop * 1.5f) {
                cycleListener.onCycle(dx < 0 ? 1 : -1);
                announceForAccessibility(dx < 0 ? "Next widget" : "Previous widget");
            }
            return true;
        }
        return event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_MOVE;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.ViewFlipper");
        if (cycleListener != null) {
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, android.os.Bundle args) {
        if (cycleListener != null && action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            cycleListener.onCycle(1);
            return true;
        }
        if (cycleListener != null && action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            cycleListener.onCycle(-1);
            return true;
        }
        return super.performAccessibilityAction(action, args);
    }
}
