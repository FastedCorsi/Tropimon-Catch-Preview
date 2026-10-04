package fr.tropimon.catchpreview.preview;

import java.util.function.LongSupplier;

/** Normal previews expire; a first release click locks its target without a timeout. */
final class PreviewState<T> {
    private static final long DISPLAY_DURATION_NANOS = 20_000_000_000L;
    private final LongSupplier nanoTimeSource;
    private long shownAtNanos;

    T visible;
    T queued;
    T pending;

    PreviewState() {
        this(System::nanoTime);
    }

    PreviewState(LongSupplier nanoTimeSource) {
        this.nanoTimeSource = nanoTimeSource;
    }

    void show(T preview) {
        if (pending != null) {
            // Keep the confirmed target visible; only the latest catch waits behind it.
            queued = preview;
        } else {
            visible = preview;
            queued = null;
            shownAtNanos = nanoTimeSource.getAsLong();
        }
    }

    void confirm() {
        pending = visible;
    }

    void complete(boolean wasReleaseSubmitted) {
        pending = null;
        if (wasReleaseSubmitted || queued != null) {
            visible = queued;
            queued = null;
        }
        shownAtNanos = nanoTimeSource.getAsLong();
    }

    boolean expire() {
        if (visible == null || pending != null) {
            return false;
        }
        if (nanoTimeSource.getAsLong() - shownAtNanos < DISPLAY_DURATION_NANOS) {
            return false;
        }

        close();
        return true;
    }

    void close() {
        visible = null;
        queued = null;
        pending = null;
        shownAtNanos = 0;
    }
}
