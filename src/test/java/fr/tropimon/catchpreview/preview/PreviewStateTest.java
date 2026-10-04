package fr.tropimon.catchpreview.preview;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreviewStateTest {
    @Test void closesAtTwentySecondsButNotBefore() {
        var now = new java.util.concurrent.atomic.AtomicLong();
        var q = new PreviewState<String>(now::get);
        q.show("a");
        now.set(19_999_999_999L); assertFalse(q.expire()); assertEquals("a", q.visible);
        now.incrementAndGet(); assertTrue(q.expire()); assertNull(q.visible);
        assertFalse(q.expire());
    }
    @Test void newCaptureRestartsFullDelay() {
        var now = new java.util.concurrent.atomic.AtomicLong();
        var q = new PreviewState<String>(now::get);
        q.show("a"); now.set(19_000_000_000L); q.show("b");
        now.set(20_000_000_000L); assertFalse(q.expire()); assertEquals("b", q.visible);
        now.set(39_000_000_000L); assertTrue(q.expire());
    }
    @Test void confirmationNeverExpiresAndQueuedCaptureGetsItsOwnTwentySeconds() {
        var now = new java.util.concurrent.atomic.AtomicLong();
        var q = new PreviewState<String>(now::get);
        q.show("locked"); q.confirm(); q.show("next");
        now.set(600_000_000_000L);
        assertFalse(q.expire()); assertEquals("locked", q.pending); assertEquals("locked", q.visible);
        q.complete(true);
        assertFalse(q.expire()); assertEquals("next", q.visible);
        now.addAndGet(20_000_000_000L); assertTrue(q.expire());
    }
    @Test void manualCloseAndReopenDoNotReuseOldDeadline() {
        var now = new java.util.concurrent.atomic.AtomicLong();
        var q = new PreviewState<String>(now::get);
        q.show("a"); q.close(); now.set(100_000_000_000L); q.show("b");
        assertFalse(q.expire());
        now.addAndGet(20_000_000_000L); assertTrue(q.expire());
    }
    @Test void rapidCapturesCannotReplaceConfirmedTarget() {
        var q = new PreviewState<String>();
        q.show("clicked"); q.confirm();
        for (int i = 0; i < 1000; i++) q.show("capture-" + i);
        assertEquals("clicked", q.pending);
        assertEquals("clicked", q.visible);
        q.complete(true);
        assertEquals("capture-999", q.visible);
        assertNull(q.pending);
    }
    @Test void noImplicitTimeoutOrExpiry() {
        var q = new PreviewState<String>();
        q.show("a"); q.confirm();
        assertEquals("a", q.pending);
        q.show("b"); q.complete(false);
        assertEquals("b", q.visible);
    }
    @Test void failedValidationWithoutNewCaptureKeepsPreview() {
        var q = new PreviewState<String>(); q.show("a"); q.confirm(); q.complete(false);
        assertEquals("a", q.visible); assertNull(q.pending);
    }
    @Test void dismissalCancelsConfirmationAndQueuedCapture() {
        var q = new PreviewState<String>(); q.show("a"); q.confirm(); q.show("b"); q.close();
        assertNull(q.visible); assertNull(q.pending); assertNull(q.queued);
        q.show("c"); assertEquals("c", q.visible);
    }
    @Test void normalCapturesReplaceImmediatelyAndSuccessfulReleaseCloses() {
        var q = new PreviewState<String>(); q.show("a"); q.show("b");
        assertEquals("b", q.visible);
        q.confirm(); q.complete(true); assertNull(q.visible);
    }
}
