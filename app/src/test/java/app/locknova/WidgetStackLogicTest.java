package app.locknova;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import app.locknova.core.WidgetStackLogic;

public final class WidgetStackLogicTest {
    @Test public void swipeForwardCyclesWithinOneSharedStack() {
        assertEquals(1, WidgetStackLogic.nextIndex(0, 1, 3));
        assertEquals(2, WidgetStackLogic.nextIndex(1, 1, 3));
        assertEquals(0, WidgetStackLogic.nextIndex(2, 1, 3));
    }

    @Test public void swipeBackwardWrapsToLastWidget() {
        assertEquals(2, WidgetStackLogic.nextIndex(0, -1, 3));
        assertEquals(1, WidgetStackLogic.nextIndex(2, -1, 3));
    }

    @Test public void emptyAndSingleWidgetStacksRemainAtFirstPage() {
        assertEquals(0, WidgetStackLogic.nextIndex(0, 1, 0));
        assertEquals(0, WidgetStackLogic.nextIndex(0, -1, 1));
        assertEquals("No widgets", WidgetStackLogic.positionLabel(0, 0));
        assertEquals("1 / 1", WidgetStackLogic.positionLabel(7, 1));
    }

    @Test public void positionLabelClampsInvalidIndexes() {
        assertEquals("1 / 3", WidgetStackLogic.positionLabel(-1, 3));
        assertEquals("3 / 3", WidgetStackLogic.positionLabel(99, 3));
    }
}
