package fr.tropimon.catchpreview.ui;

import fr.tropimon.catchpreview.preview.CatchPreviewController;

/** GUI-scaled coordinates retained independently of popup visibility and capture state. */
public final class PreviewPosition {
    private static final DraggableWindowPosition POSITION = new DraggableWindowPosition(CatchPreviewRenderer.WIDTH, CatchPreviewRenderer.HEIGHT);
    private PreviewPosition() {}
    static int left(int width) { return POSITION.left(width); }
    static int top(int height) { return POSITION.top(height); }
    public static boolean startDrag(double x, double y, int width, int height) {
        if (CatchPreviewController.visible() == null) return false;
        return POSITION.start(x, y, width, height);
    }
    public static boolean drag(double x, double y, int width, int height) {
        return POSITION.drag(x, y, width, height);
    }
    public static boolean stopDrag() { return POSITION.stop(); }
}
