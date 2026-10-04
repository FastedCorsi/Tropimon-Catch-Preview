package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.client.net.storage.pc.OpenPCHandler;
import com.cobblemon.mod.common.net.messages.client.storage.pc.OpenPCPacket;
import fr.tropimon.catchpreview.release.PokemonReleaseController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OpenPCHandler.class, remap = false)
abstract class OpenPCHandlerMixin {
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/client/storage/pc/OpenPCPacket;Lnet/minecraft/client/MinecraftClient;)V", at = @At("TAIL"))
    private void tropimonCatchPreview$pcOpened(OpenPCPacket packet, MinecraftClient client, CallbackInfo ci) {
        PokemonReleaseController.onPcOpened(client, packet.getStoreID());
    }
}
