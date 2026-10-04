package fr.tropimon.catchpreview.mixin;

import fr.tropimon.catchpreview.release.PokemonReleaseController;

import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.pokemon.Pokemon;
import fr.tropimon.catchpreview.preview.CatchPreviewController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPC.class, remap = false)
abstract class ClientPCMixin {
    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/pc/PCPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("TAIL"))
    private void tropimonCatchPreview$invalidate(PCPosition position, Pokemon pokemon, CallbackInfo ci) {
        fr.tropimon.catchpreview.release.PokemonReleaseController.invalidateStorage();
    }

    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/pc/PCPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"))
    private void tropimonCatchPreview$set(PCPosition position, Pokemon pokemon, CallbackInfo ci) {
        ClientPC self = (ClientPC) (Object) this;
        CatchPreviewController.storageSet(self.get(position), pokemon);
    }
}
