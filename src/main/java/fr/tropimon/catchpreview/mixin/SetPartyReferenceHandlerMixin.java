package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.client.net.storage.party.SetPartyReferenceHandler;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyReferencePacket;
import fr.tropimon.catchpreview.preview.CatchPreviewController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SetPartyReferenceHandler.class, remap = false)
abstract class SetPartyReferenceHandlerMixin {
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/client/storage/party/SetPartyReferencePacket;Lnet/minecraft/client/MinecraftClient;)V", at = @At("TAIL"))
    private void tropimonCatchPreview$storageReady(SetPartyReferencePacket packet, MinecraftClient client, CallbackInfo ci) {
        CatchPreviewController.storageReady();
    }
}
