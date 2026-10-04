package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.net.messages.server.storage.SwapPCPartyPokemonPacket;
import com.cobblemon.mod.common.net.messages.server.storage.party.*;
import com.cobblemon.mod.common.net.messages.server.storage.pc.*;
import fr.tropimon.catchpreview.capture.StorageTransferObserver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {SwapPCPartyPokemonPacket.class, MovePCPokemonToPartyPacket.class,
        MovePartyPokemonToPCPacket.class, MovePCPokemonPacket.class, MovePartyPokemonPacket.class,
        SwapPCPokemonPacket.class, SwapPartyPokemonPacket.class}, remap = false)
abstract class StorageTransferMixin {
    @Inject(method = "sendToServer()V", at = @At("HEAD"))
    private void tropimonCatchPreview$transfer(CallbackInfo ci) {
        StorageTransferObserver.sending(this);
    }
}
