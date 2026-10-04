package fr.tropimon.catchpreview;

import fr.tropimon.catchpreview.preview.CatchPreviewController;
import fr.tropimon.catchpreview.release.PokemonReleaseController;
import fr.tropimon.catchpreview.ui.CatchPreviewRenderer;
import fr.tropimon.catchpreview.ui.PreviewPosition;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TropimonCatchPreviewClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("tropimon_catch_preview");
    private static KeyBinding closeKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Tropimon Catch Preview initialisé");
        closeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tropimon_catch_preview.close",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                "category.tropimon_catch_preview"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PokemonReleaseController.checkContext();
            if (client.currentScreen == null || client.options.hudHidden || !client.isWindowFocused()) PreviewPosition.stopDrag();
            while (closeKey.wasPressed()) CatchPreviewController.close();
            CatchPreviewController.tick();
            CatchPreviewRenderer.refreshText();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> CatchPreviewController.beginSession());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CatchPreviewController.endSession());
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterRender(screen).register((current, context, mouseX, mouseY, delta) ->
                        CatchPreviewRenderer.renderMarkTooltip(context, mouseX, mouseY)));
    }
}

