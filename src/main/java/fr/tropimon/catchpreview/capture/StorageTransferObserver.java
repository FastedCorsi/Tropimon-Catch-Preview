package fr.tropimon.catchpreview.capture;

import fr.tropimon.catchpreview.preview.CatchPreviewController;

import com.cobblemon.mod.common.net.messages.server.storage.SwapPCPartyPokemonPacket;
import com.cobblemon.mod.common.net.messages.server.storage.party.*;
import com.cobblemon.mod.common.net.messages.server.storage.pc.*;

/** Observe only official protocol data, never the sender's implementation or screen. */
public final class StorageTransferObserver {
    private StorageTransferObserver() {}

    public static void sending(Object packet) {
        if (packet instanceof MovePCPokemonToPartyPacket p) CatchPreviewController.remember(p.getPokemonID());
        else if (packet instanceof MovePartyPokemonToPCPacket p) CatchPreviewController.remember(p.getPokemonID());
        else if (packet instanceof MovePCPokemonPacket p) CatchPreviewController.remember(p.getPokemonID());
        else if (packet instanceof MovePartyPokemonPacket p) CatchPreviewController.remember(p.getPokemonID());
        else if (packet instanceof SwapPCPartyPokemonPacket p) {
            CatchPreviewController.remember(p.getPartyPokemonID());
            CatchPreviewController.remember(p.getPcPokemonID());
        } else if (packet instanceof SwapPCPokemonPacket p) {
            CatchPreviewController.remember(p.getPokemon1ID());
            CatchPreviewController.remember(p.getPokemon2ID());
        } else if (packet instanceof SwapPartyPokemonPacket p) {
            CatchPreviewController.remember(p.getPokemon1ID());
            CatchPreviewController.remember(p.getPokemon2ID());
        }
    }
}
