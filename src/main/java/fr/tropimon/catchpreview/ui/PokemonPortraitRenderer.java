package fr.tropimon.catchpreview.ui;

import fr.tropimon.catchpreview.TropimonCatchPreviewClient;

import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawContext;
import org.joml.Quaternionf;

import java.lang.reflect.Method;
import java.util.UUID;

public final class PokemonPortraitRenderer {
    private static final Method PROFILE_RENDERER = findProfileRenderer();
    private static UUID uuid;
    private static FloatingState state = new FloatingState();
    private static RenderablePokemon renderable;
    private static boolean hasReportedRenderFailure;
    private static final Quaternionf ROTATION = new Quaternionf();

    private PokemonPortraitRenderer() {}

    public static void reset() { uuid = null; renderable = null; hasReportedRenderFailure = false; }

    static void draw(DrawContext context, Pokemon pokemon, int x, int y, int size) {
        if (!pokemon.getUuid().equals(uuid)) {
            uuid = pokemon.getUuid();
            state = new FloatingState();
        }
        boolean clipped = false;
        boolean pushed = false;
        try {
            context.enableScissor(x, y, x + size, y + size);
            clipped = true;
            context.getMatrices().push();
            pushed = true;
            float scale = Math.max(0.8F, size / 38.0F);
            context.getMatrices().translate(x + size / 2.0D, y - 3.0D, 1000.0D);
            context.getMatrices().scale(scale, scale, scale);
            ROTATION.rotationXYZ(
                    (float) Math.toRadians(13), (float) Math.toRadians(25), 0.0F);
            // Match asRenderablePokemon exactly while retaining its live references.
            ItemStack held = pokemon.getHeldItemVisible() ? pokemon.heldItemNoCopy$common() : ItemStack.EMPTY;
            if (renderable == null) renderable = new RenderablePokemon(pokemon.getSpecies(), pokemon.getAspects(), held);
            else {
                renderable.setSpecies(pokemon.getSpecies());
                renderable.setAspects(pokemon.getAspects());
                renderable.setHeldItem(held);
            }
            drawProfilePokemon(context);
        } catch (RuntimeException | LinkageError failure) {
            // Keep the card usable and retry next frame, without flooding the log.
            if (!hasReportedRenderFailure) {
                hasReportedRenderFailure = true;
                TropimonCatchPreviewClient.LOGGER.warn("Unable to draw the capture portrait; preview details remain available", failure);
            }
        } finally {
            if (pushed) context.getMatrices().pop();
            if (clipped) context.disableScissor();
        }
    }

    private static void drawProfilePokemon(DrawContext context) {
        try {
            if (PROFILE_RENDERER.getParameterCount() == 16) {
                Class<?> transformType =
                        Class.forName("com.cobblemon.mod.common.client.gui.ProfileTransformType");
                Object profileTransform = transformType.getField("PROFILE").get(null);
                PROFILE_RENDERER.invoke(null, renderable, context.getMatrices(), ROTATION,
                        PoseType.PROFILE, state, 0.0F, 20.0F, profileTransform, true,
                        1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 15);
            } else {
                PROFILE_RENDERER.invoke(null, renderable, context.getMatrices(), ROTATION,
                        PoseType.PROFILE, state, 0.0F, 20.0F, true, false,
                        1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("API de portrait Cobblemon non compatible", exception);
        }
    }

    private static Method findProfileRenderer() {
        for (Method method : PokemonGuiUtilsKt.class.getMethods()) {
            if (method.getName().equals("drawProfilePokemon")
                    && method.getParameterCount() >= 15
                    && method.getParameterTypes()[0] == RenderablePokemon.class) {
                return method;
            }
        }
        throw new IllegalStateException("Rendu de portrait Cobblemon introuvable");
    }
}
