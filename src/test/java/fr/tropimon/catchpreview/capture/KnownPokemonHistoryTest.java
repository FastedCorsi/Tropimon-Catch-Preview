package fr.tropimon.catchpreview.capture;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class KnownPokemonHistoryTest {
    @Test void repeatedPcToPartyTransfersNeverBecomeCapturesAfterSourceRemoval() {
        var history = new KnownPokemonHistory();
        history.ready();
        UUID transferredPokemonId = UUID.randomUUID();
        history.remember(transferredPokemonId); // Outgoing official transfer, before either storage update.
        for (int transfer = 0; transfer < 10; transfer++) {
            assertFalse(history.storageSet(transferredPokemonId, null));
            assertTrue(history.storageSet(null, UUID.randomUUID()), "Concurrent catches remain visible");
            assertFalse(history.storageSet(null, transferredPokemonId));
        }
    }

    @Test void tradedPokemonIsIgnoredBeforeStorageAddWithoutSuppressingConcurrentCaptures() {
        var h = new KnownPokemonHistory(); h.ready();
        UUID offered = UUID.randomUUID(), outgoing = UUID.randomUUID();
        h.remember(outgoing);
        h.remember(offered); // Official trade offer, before completion and storage updates.
        assertFalse(h.storageSet(outgoing, null));
        assertTrue(h.storageSet(null, UUID.randomUUID()));
        assertFalse(h.storageSet(null, offered));
        assertTrue(h.storageSet(null, UUID.randomUUID()));
        assertFalse(h.storageSet(offered, offered)); // Trade evolution/update.
        assertFalse(h.storageSet(offered, null));
        assertFalse(h.storageSet(null, offered)); // Later PC -> party transfer.
    }
    @Test void initialSyncAndPostBattleUpdatesAreNotCaptures() {
        var h = new KnownPokemonHistory();
        UUID team = UUID.randomUUID();
        assertFalse(h.storageSet(null, team));
        h.ready();
        assertFalse(h.storageSet(team, team));
        assertFalse(h.storageSet(team, null));
        assertFalse(h.storageSet(null, team));
    }
    @Test void capturesInEmptyPartyOrPcSlotsRemainDetectedInRapidSuccession() {
        var h = new KnownPokemonHistory(); h.ready();
        for (int i = 0; i < 20_000; i++) assertTrue(h.storageSet(null, new UUID(1, i)));
        assertEquals(20_000, h.size());
        assertFalse(h.storageSet(null, new UUID(1, 0)), "No arbitrary UUID eviction");
    }
    @Test void teamTransfersAndBulkSyncDoNotSuppressConcurrentNewCaptures() {
        var h = new KnownPokemonHistory(); h.ready();
        UUID transferred = UUID.randomUUID(), pcSync = UUID.randomUUID();
        h.remember(transferred); h.remember(pcSync);
        assertFalse(h.storageSet(null, transferred));
        assertTrue(h.storageSet(null, UUID.randomUUID()));
        assertFalse(h.storageSet(null, pcSync));
    }
    @Test void occupiedSlotReplacementIsRememberedButNotShown() {
        var h = new KnownPokemonHistory(); h.ready();
        UUID old = UUID.randomUUID(), replacement = UUID.randomUUID();
        assertFalse(h.storageSet(old, replacement));
        assertFalse(h.storageSet(null, old));
        assertFalse(h.storageSet(null, replacement));
    }
    @Test void sessionResetWaitsForInitialSyncAgain() {
        var h = new KnownPokemonHistory(); h.ready(); h.remember(UUID.randomUUID());
        h.reset();
        assertEquals(0, h.size());
        assertFalse(h.storageSet(null, UUID.randomUUID()));
        h.ready();
        assertTrue(h.storageSet(null, UUID.randomUUID()));
    }
}
