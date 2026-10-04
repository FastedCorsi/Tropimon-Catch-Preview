package fr.tropimon.catchpreview.capture;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Session-long history: never evict UUIDs, including removed or transferred Pokémon. */
public final class KnownPokemonHistory {
    private final Set<UUID> knownPokemonIds = new HashSet<>();
    private boolean isStorageReady;

    public void remember(UUID pokemonId) {
        if (pokemonId != null) {
            knownPokemonIds.add(pokemonId);
        }
    }

    public boolean storageSet(UUID previousPokemonId, UUID incomingPokemonId) {
        remember(previousPokemonId);
        if (incomingPokemonId == null) {
            return false;
        }

        boolean isNewPokemon = knownPokemonIds.add(incomingPokemonId);
        return previousPokemonId == null && isNewPokemon && isStorageReady;
    }

    public void ready() {
        isStorageReady = true;
    }

    public int size() {
        return knownPokemonIds.size();
    }

    public void reset() {
        knownPokemonIds.clear();
        isStorageReady = false;
    }
}
