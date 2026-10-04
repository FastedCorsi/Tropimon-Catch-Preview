package fr.tropimon.catchpreview.ui;

/** Pure layout/drag state; no mouse locking and no dependency on another mod's config. */
final class DraggableWindowPosition {
    private final int width;
    private final int height;
    private boolean hasSavedPosition;
    private boolean isDragging;
    private int x;
    private int y;
    private double dragOffsetX;
    private double dragOffsetY;

    DraggableWindowPosition(int width, int height) {
        this.width = width;
        this.height = height;
    }

    int left(int screenWidth) {
        return hasSavedPosition ? clamp(x, screenWidth, width) : Math.max(2, screenWidth - width - 12);
    }

    int top(int screenHeight) {
        return hasSavedPosition ? clamp(y, screenHeight, height)
                : Math.min(Math.max(2, screenHeight - height - 2), Math.max(12, (int) (screenHeight * .58F)));
    }

    boolean start(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int left = left(screenWidth);
        int top = top(screenHeight);
        // Header only; leave the release and close buttons untouched, even when hidden.
        if (mouseX < left + 7 || mouseX >= left + width - 49 || mouseY < top + 4 || mouseY >= top + 20) {
            return false;
        }
        dragOffsetX = mouseX - left;
        dragOffsetY = mouseY - top;
        x = left;
        y = top;
        hasSavedPosition = true;
        isDragging = true;
        return true;
    }

    boolean drag(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!isDragging) {
            return false;
        }
        x = clamp((int) Math.round(mouseX - dragOffsetX), screenWidth, width);
        y = clamp((int) Math.round(mouseY - dragOffsetY), screenHeight, height);
        return true;
    }
    boolean stop() {
        boolean wasDragging = isDragging;
        isDragging = false;
        return wasDragging;
    }

    private static int clamp(int coordinate, int screenSize, int size) {
        return Math.max(2, Math.min(coordinate, Math.max(2, screenSize - size - 2)));
    }
}
