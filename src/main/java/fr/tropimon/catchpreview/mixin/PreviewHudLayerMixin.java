package fr.tropimon.catchpreview.mixin;

import fr.tropimon.catchpreview.ui.CatchPreviewRenderer;
import net.minecraft.client.gui.LayeredDrawer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
abstract class PreviewHudLayerMixin {
    @Shadow @Final private LayeredDrawer layeredDrawer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tropimonCatchPreview$lastHudLayer(CallbackInfo ci) {
        // Keep Minecraft's accumulated HUD depth instead of returning to Z=0
        // after LayeredDrawer pops its matrix, where the hotbar can occlude IVs.
        layeredDrawer.addLayer((context, tickCounter) -> CatchPreviewRenderer.render(context));
    }
}
