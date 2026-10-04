package fr.tropimon.catchpreview.release;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SingleEntryCacheTest {
    @Test void stableFramesReuseBothPositiveAndNegativeLocations() {
        var c = new SingleEntryCache<String, String>();
        assertFalse(c.contains("a")); c.put("a", "slot1");
        for (int i = 0; i < 1000; i++) { assertTrue(c.contains("a")); assertEquals("slot1", c.value()); }
        c.put("a", null); assertTrue(c.contains("a")); assertNull(c.value());
    }
    @Test void storageWorldAndSessionInvalidationDiscardStaleLocations() {
        var c = new SingleEntryCache<String, String>();
        for (int i = 0; i < 3; i++) {
            c.put("a", "old"); c.invalidate();
            assertFalse(c.contains("a")); assertNull(c.value());
            c.put("a", "new"); assertEquals("new", c.value());
        }
        assertFalse(c.contains("next-capture"));
    }
}
