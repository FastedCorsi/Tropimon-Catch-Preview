package fr.tropimon.catchpreview.mixin;

import fr.tropimon.catchpreview.release.PokemonReleaseController;

import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.pokemon.Pokemon;
import fr.tropimon.catchpreview.preview.CatchPreviewController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientParty.class, remap = false)
abstract class ClientPartyMixin {
    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("TAIL"))
    private void tropimonCatchPreview$invalidate(PartyPosition position, Pokemon pokemon, CallbackInfo ci) {
        fr.tropimon.catchpreview.release.PokemonReleaseController.invalidateStorage();
    }

    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"))
    private void tropimonCatchPreview$set(PartyPosition position, Pokemon pokemon, CallbackInfo ci) {
        ClientParty self = (ClientParty) (Object) this;
        CatchPreviewController.storageSet(self.get(position), pokemon);
    }
}
