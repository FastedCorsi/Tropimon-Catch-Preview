package fr.tropimon.catchpreview.preview;

import fr.tropimon.catchpreview.TropimonCatchPreviewClient;
import fr.tropimon.catchpreview.capture.KnownPokemonHistory;
import fr.tropimon.catchpreview.release.PokemonReleaseController;
import fr.tropimon.catchpreview.ui.CatchPreviewRenderer;
import fr.tropimon.catchpreview.ui.PokemonPortraitRenderer;
import fr.tropimon.catchpreview.ui.PreviewPosition;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.client.CobblemonClient;

import java.util.UUID;

public final class CatchPreviewController {
    private static final KnownPokemonHistory HISTORY = new KnownPokemonHistory();
    private static final PreviewState<Pokemon> PREVIEW = new PreviewState<>();

    private CatchPreviewController() {}

    public static synchronized void storageSet(Pokemon previousPokemon, Pokemon incomingPokemon) {
        boolean isNewCapture = HISTORY.storageSet(previousPokemon == null ? null : previousPokemon.getUuid(),
                incomingPokemon == null ? null : incomingPokemon.getUuid());
        if (incomingPokemon == null) {
            return;
        }
        // This runs before the destination write, so a Pokémon already in another
        // store is a transfer even when no outgoing move packet was observed.
        if (isNewCapture && !alreadyStored(incomingPokemon.getUuid())) {
            show(incomingPokemon);
        }
        if (PREVIEW.visible != null && PREVIEW.visible.getUuid().equals(incomingPokemon.getUuid())) {
            PREVIEW.visible = incomingPokemon;
        }
        if (PREVIEW.queued != null && PREVIEW.queued.getUuid().equals(incomingPokemon.getUuid())) {
            PREVIEW.queued = incomingPokemon;
        }
    }

    private static boolean alreadyStored(UUID pokemonId) {
        var storage = CobblemonClient.INSTANCE.getStorage();
        var party = storage.getParty();
        if (party != null && party.findByUUID(pokemonId) != null) {
            return true;
        }
        for (var pc : storage.getPcStores().values()) {
            if (pc.findByUUID(pokemonId) != null) {
                return true;
            }
        }
        return false;
    }

    public static synchronized Pokemon visible() {
        return PREVIEW.visible;
    }

    public static synchronized void tick() {
        if (PREVIEW.expire()) {
            PreviewPosition.stopDrag();
        }
    }

    public static synchronized void remember(UUID pokemonId) {
        HISTORY.remember(pokemonId);
    }

    public static synchronized void close() {
        PreviewPosition.stopDrag();
        PREVIEW.close();
    }

    private static void show(Pokemon pokemon) {
        boolean isConfirmationActive = PREVIEW.pending != null;
        PREVIEW.show(pokemon);
        if (isConfirmationActive) {
            return;
        }
        TropimonCatchPreviewClient.LOGGER.info("Aperçu affiché pour {} ({})",
                pokemon.getSpecies().getName(), pokemon.getUuid());
    }

    public static synchronized void storageReady() {
        HISTORY.ready();
        TropimonCatchPreviewClient.LOGGER.debug("Synchronisation initiale du stockage terminée ({} Pokémon connus)", HISTORY.size());
    }

    public static synchronized void beginSession() {
        PokemonReleaseController.reset();
        PokemonPortraitRenderer.reset();
        PreviewPosition.stopDrag();
        HISTORY.reset();
        PREVIEW.close();
    }

    public static synchronized void endSession() {
        beginSession();
    }

    public static synchronized boolean click(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        tick();
        if (PREVIEW.visible == null) {
            return false;
        }
        int left = CatchPreviewRenderer.left(screenWidth);
        int top = CatchPreviewRenderer.top(screenHeight);
        if (mouseX >= left + CatchPreviewRenderer.WIDTH - 18 && mouseX < left + CatchPreviewRenderer.WIDTH - 4
                && mouseY >= top + 3 && mouseY < top + 18) {
            close();
            return true;
        }
        if (mouseX >= left + CatchPreviewRenderer.WIDTH - 48 && mouseX < left + CatchPreviewRenderer.WIDTH - 21
                && mouseY >= top + 5 && mouseY < top + 19
                && PokemonReleaseController.canRelease(PREVIEW.visible)) {
            if (PREVIEW.pending != null) {
                Pokemon confirmedPokemon = PREVIEW.pending;
                boolean wasReleaseSubmitted = PokemonReleaseController.release(confirmedPokemon);
                PREVIEW.complete(wasReleaseSubmitted);
            } else {
                PREVIEW.confirm();
            }
            return true;
        }
        return false;
    }

    public static synchronized boolean releaseConfirmationActive() {
        return PREVIEW.pending != null;
    }
}
