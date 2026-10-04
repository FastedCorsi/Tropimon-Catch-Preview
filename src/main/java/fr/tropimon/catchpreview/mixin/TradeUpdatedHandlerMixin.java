package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.client.net.trade.TradeUpdatedHandler;
import com.cobblemon.mod.common.net.messages.client.trade.TradeUpdatedPacket;
import fr.tropimon.catchpreview.preview.CatchPreviewController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TradeUpdatedHandler.class, remap = false)
abstract class TradeUpdatedHandlerMixin {
    // Offers precede the storage remove/add pair. Completion arrives too late to filter it.
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/client/trade/TradeUpdatedPacket;Lnet/minecraft/client/MinecraftClient;)V", at = @At("HEAD"))
    private void tropimonCatchPreview$offered(TradeUpdatedPacket packet, MinecraftClient client, CallbackInfo ci) {
        if (packet.getPokemon() != null) CatchPreviewController.remember(packet.getPokemon().getUuid());
    }
}
