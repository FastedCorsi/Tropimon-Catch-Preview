package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.client.net.storage.pc.InitializePCHandler;
import fr.tropimon.catchpreview.release.PokemonReleaseController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InitializePCHandler.class, remap = false)
abstract class InitializePCHandlerMixin {
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/client/storage/pc/InitializePCPacket;Lnet/minecraft/client/MinecraftClient;)V", at = @At("TAIL"))
    private void tropimonCatchPreview$initialize(CallbackInfo ci) {
        PokemonReleaseController.invalidateStorage();
    }
}
