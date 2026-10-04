package fr.tropimon.catchpreview.release;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseRulesTest {
    @Test void lastPartyPokemonIsAlwaysProtected() {
        assertFalse(ReleaseRules.canReleaseParty(0)); assertFalse(ReleaseRules.canReleaseParty(1));
        for (int n = 2; n <= 6; n++) assertTrue(ReleaseRules.canReleaseParty(n));
    }
    @Test void originalPcCuboidIncludesCornersButNothingBeyond() {
        int positions = 0;
        for (int x = -6; x <= 6; x++) for (int y = -4; y <= 4; y++) for (int z = -6; z <= 6; z++) {
            boolean expected = Math.abs(x) <= 5 && Math.abs(y) <= 3 && Math.abs(z) <= 5;
            assertEquals(expected, ReleaseRules.withinPcRange(x, y, z));
            if (expected) positions++;
        }
        assertEquals(847, positions);
    }
}
