package fr.tropimon.catchpreview.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DraggableWindowPositionTest {
    @Test void allFourGuiSizesAndResizeKeepTheWholeWindowAndDragTargetVisible() {
        var p = new DraggableWindowPosition(150, 109);
        for (int[] viewport : new int[][]{{1920,1080},{960,540},{640,360},{480,270},{320,240},{427,240}}) {
            int width = viewport[0], height = viewport[1];
            assertTrue(p.left(width) >= 2 && p.left(width) + 150 <= width - 2);
            assertTrue(p.top(height) >= 2 && p.top(height) + 109 <= height - 2);
            assertTrue(p.start(p.left(width) + 12, p.top(height) + 8, width, height));
            assertTrue(p.drag(width * 2, height * 2, width, height));
            assertEquals(width - 152, p.left(width));
            assertEquals(height - 111, p.top(height));
            assertTrue(p.stop());
        }
    }
    @Test void defaultPositionIsUnchanged() {
        var p = new DraggableWindowPosition(150, 101);
        assertEquals(638, p.left(800)); assertEquals(348, p.top(600));
    }
    @Test void dragPositionSurvivesStopAndNextDragWithoutJumping() {
        var p = new DraggableWindowPosition(150, 101);
        assertTrue(p.start(650, 360, 800, 600));
        assertTrue(p.drag(200, 150, 800, 600));
        assertEquals(188, p.left(800)); assertEquals(138, p.top(600));
        assertTrue(p.stop()); assertFalse(p.drag(500, 500, 800, 600));
        assertEquals(188, p.left(800));
        assertTrue(p.start(200, 150, 800, 600));
        p.drag(200, 150, 800, 600);
        assertEquals(188, p.left(800)); assertEquals(138, p.top(600));
    }
    @Test void releaseAndCloseButtonsAndPortraitAreNotDragHandles() {
        var p = new DraggableWindowPosition(150, 101);
        assertFalse(p.start(750, 358, 800, 600));
        assertFalse(p.start(775, 358, 800, 600));
        assertFalse(p.start(650, 390, 800, 600));
    }
    @Test void draggingAndWindowResizeStayWithinBounds() {
        var p = new DraggableWindowPosition(150, 101);
        p.start(650, 360, 800, 600); p.drag(-100, -100, 800, 600);
        assertEquals(2, p.left(800)); assertEquals(2, p.top(600));
        p.drag(900, 900, 800, 600);
        assertEquals(648, p.left(800)); assertEquals(497, p.top(600));
        assertEquals(168, p.left(320)); assertEquals(137, p.top(240));
    }
}
