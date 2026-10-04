package fr.tropimon.catchpreview.mixin;

import fr.tropimon.catchpreview.preview.CatchPreviewController;
import fr.tropimon.catchpreview.ui.PreviewPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
abstract class MouseMixin {
    @Shadow private double x;
    @Shadow private double y;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void tropimonCatchPreview$click(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (window != client.getWindow().getHandle() || button != 0) return;
        if (action == 0 && PreviewPosition.stopDrag()) { ci.cancel(); return; }
        if (action != 1 || client.currentScreen == null || client.options.hudHidden) return;
        double scaledMouseX = x * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double scaledMouseY = y * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        if (CatchPreviewController.click(scaledMouseX, scaledMouseY,
                client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight())
                || PreviewPosition.startDrag(scaledMouseX, scaledMouseY,
                client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight())) ci.cancel();
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void tropimonCatchPreview$drag(long window, double mouseX, double mouseY, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (window != client.getWindow().getHandle()) return;
        if (client.currentScreen == null || client.options.hudHidden) { PreviewPosition.stopDrag(); return; }
        if (PreviewPosition.drag(mouseX * client.getWindow().getScaledWidth() / client.getWindow().getWidth(),
                mouseY * client.getWindow().getScaledHeight() / client.getWindow().getHeight(),
                client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight())) {
            x = mouseX; y = mouseY;
            ci.cancel();
        }
    }
}
