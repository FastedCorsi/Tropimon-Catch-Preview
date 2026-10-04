package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.net.storage.pc.SetPCBoxHandler;
import com.cobblemon.mod.common.net.messages.client.storage.pc.SetPCBoxPacket;
import fr.tropimon.catchpreview.preview.CatchPreviewController;
import fr.tropimon.catchpreview.release.PokemonReleaseController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SetPCBoxHandler.class, remap = false)
abstract class SetPCBoxHandlerMixin {
    // Bulk sync writes ClientBox.slots directly, without ClientPC.set.
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/client/storage/pc/SetPCBoxPacket;Lnet/minecraft/client/MinecraftClient;)V", at = @At("TAIL"))
    private void tropimonCatchPreview$box(SetPCBoxPacket packet, MinecraftClient client, CallbackInfo ci) {
        var pc = CobblemonClient.INSTANCE.getStorage().getPcStores().get(packet.getStoreID());
        if (pc != null && packet.getBoxNumber() >= 0 && packet.getBoxNumber() < pc.getBoxes().size()) {
            for (var pokemon : pc.getBoxes().get(packet.getBoxNumber()).getSlots()) {
                if (pokemon != null) CatchPreviewController.remember(pokemon.getUuid());
            }
        }
        PokemonReleaseController.invalidateStorage();
    }
}
