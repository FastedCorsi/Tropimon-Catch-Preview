package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.client.storage.ClientStorageManager;
import fr.tropimon.catchpreview.release.PokemonReleaseController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientStorageManager.class, remap = false)
abstract class StorageManagerMixin {
    @Inject(method = {"setParty", "createParty", "setPartyStore", "onLogin", "onLogout"}, at = @At("TAIL"))
    private void tropimonCatchPreview$changed(CallbackInfo ci) {
        PokemonReleaseController.invalidateStorage();
    }
}
