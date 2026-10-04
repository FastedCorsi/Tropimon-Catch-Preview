package fr.tropimon.catchpreview.ui;

import fr.tropimon.catchpreview.preview.CatchPreviewController;
import fr.tropimon.catchpreview.preview.PokemonDetailsReader;
import fr.tropimon.catchpreview.release.PokemonReleaseController;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;

public final class CatchPreviewRenderer {
    public static final int WIDTH = 150;
    public static final int HEIGHT = 109;
    private static final Identifier PORTRAIT_BACKGROUND = Identifier.of("cobblemon", "textures/gui/summary/portrait_background.png");
    private static final Identifier TROPIMON_FRAME = Identifier.of("tropimon_catch_preview", "textures/gui/frame.png");
    private static final Map<String, Text> LABELS = new HashMap<>();
    private static final Map<String, Text> IV_LABELS = new HashMap<>();
    private static final Text[] IV_VALUES = new Text[32];
    private static final Text CLOSE = Text.literal("×"), SHINY = Text.literal("★");
    private static Pokemon textPokemon;
    private static Text name = Text.empty(), level = Text.empty(), nature = Text.empty(), ability = Text.empty();
    private static boolean hiddenAbility;
    private static Text details = Text.empty();
    static {
        for (int i = 0; i < IV_VALUES.length; i++) IV_VALUES[i] = Text.literal(Integer.toString(i));
        for (String key : new String[]{"hp", "attack", "defence", "special_attack", "special_defence", "speed"}) {
            IV_LABELS.put(key, Text.translatable("gui.tropimon_catch_preview.iv." + key).append(" "));
        }
    }

    private static Text label(String key) { return LABELS.computeIfAbsent(key, Text::translatable); }

    /** Once per game tick, and immediately for a newly displayed instance. Translations stay live. */
    public static void refreshText() {
        textPokemon = CatchPreviewController.visible();
        PreviewMarksRenderer.refresh(textPokemon);
        if (textPokemon == null) { name = level = nature = ability = details = Text.empty(); return; }
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        name = reuse(name, textRenderer.trimToWidth(textPokemon.getDisplayName(false).getString(), 52));
        nature = reuse(nature, textRenderer.trimToWidth(natureText(textPokemon).getString(), 78));
        ability = reuse(ability, textRenderer.trimToWidth(abilityText(textPokemon).getString(), 78));
        level = reuse(level, Text.translatable("gui.tropimon_catch_preview.level", textPokemon.getLevel())
                .append("  " + gender(textPokemon.getGender())).getString());
        hiddenAbility = PokemonDetailsReader.isHiddenAbility(textPokemon);
        String size = PokemonDetailsReader.size(textPokemon);
        String sizeText = size.isEmpty() ? "" : Text.translatable("gui.tropimon_catch_preview.size", size).getString();
        details = reuse(details, PokemonDetailsReader.alpha(textPokemon)
                ? Text.translatable("gui.tropimon_catch_preview.alpha").getString()
                    + (sizeText.isEmpty() ? "" : " · " + sizeText)
                : sizeText);
    }

    private static Text reuse(Text previous, String value) {
        return previous.getString().equals(value) ? previous : Text.literal(value);
    }

    private CatchPreviewRenderer() {}

    public static int left(int screenWidth) {
        return PreviewPosition.left(screenWidth);
    }

    public static int top(int screenHeight) {
        return PreviewPosition.top(screenHeight);
    }

    public static void render(DrawContext context) {
        Pokemon pokemon = CatchPreviewController.visible();
        MinecraftClient client = MinecraftClient.getInstance();
        if (pokemon == null || client.player == null || client.options.hudHidden) return;
        if (textPokemon != pokemon) refreshText();
        // Flush native HUD batches first so hotbar slots cannot later cover the IVs.
        context.draw();
        context.getMatrices().push();
        try {
            drawCard(context, pokemon, client);
            context.draw();
        } finally {
            context.getMatrices().pop();
        }
    }

    private static void drawCard(DrawContext context, Pokemon pokemon, MinecraftClient client) {
        TextRenderer textRenderer = client.textRenderer;
        int x = left(context.getScaledWindowWidth());
        int y = top(context.getScaledWindowHeight());

        drawScaled(context, TROPIMON_FRAME, x, y, 345, 205, WIDTH, HEIGHT);
        context.fill(x + 7, y + 6, x + WIDTH - 7, y + HEIGHT - 6, 0xEA111A22);
        try {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.18F);
            drawScaled(context, PORTRAIT_BACKGROUND, x + 7, y + 6, 66, 66, WIDTH - 14, HEIGHT - 12);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        context.fill(x + 8, y + 20, x + WIDTH - 8, y + 21, 0xAA58DFF4);
        context.drawTextWithShadow(textRenderer, label("gui.tropimon_catch_preview.title"), x + 12, y + 10, 0xFF7CF0D1);
        if (PokemonReleaseController.canRelease(pokemon)) {
            boolean confirming = CatchPreviewController.releaseConfirmationActive();
            int buttonX = x + WIDTH - 48;
            context.fill(buttonX, y + 6, x + WIDTH - 21, y + 19,
                    confirming ? 0xCC8A6518 : 0xCC6D2525);
            Text label = label(confirming
                    ? "gui.tropimon_catch_preview.release_confirm_short"
                    : "gui.tropimon_catch_preview.release_short");
            context.drawCenteredTextWithShadow(textRenderer, label, buttonX + 13, y + 9,
                    confirming ? 0xFFFFD84D : 0xFFFFAAAA);
        }
        context.drawTextWithShadow(textRenderer, CLOSE, x + WIDTH - 18, y + 9, 0xFFFFFFFF);

        PokemonPortraitRenderer.draw(context, pokemon, x + 5, y + 19, 50);
        context.drawCenteredTextWithShadow(textRenderer, name, x + 31, y + 65, 0xFFFFFFFF);
        PreviewMarksRenderer.draw(context, x + PreviewMarksRenderer.OFFSET_X, y + PreviewMarksRenderer.OFFSET_Y);
        if (pokemon.getShiny()) context.drawCenteredTextWithShadow(textRenderer, SHINY, x + 12, y + 23, 0xFFFFD45A);

        int detailsX = x + 61;
        context.drawTextWithShadow(textRenderer, level, detailsX, y + 23, genderColor(pokemon.getGender()));
        context.getMatrices().push();
        try {
            context.getMatrices().translate(detailsX, y + 34, 0);
            float detailScale = Math.min(0.75F, 80.0F / Math.max(1, textRenderer.getWidth(details)));
            context.getMatrices().scale(detailScale, detailScale, 1);
            context.drawTextWithShadow(textRenderer, details, 0, 0, 0xFFB8DDE5);
        } finally { context.getMatrices().pop(); }
        context.drawTextWithShadow(textRenderer, label("gui.tropimon_catch_preview.nature"), detailsX, y + 42, 0xFF9BA6AD);
        context.drawTextWithShadow(textRenderer, nature, detailsX, y + 51, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, label("gui.tropimon_catch_preview.ability"), detailsX, y + 61, 0xFF9BA6AD);
        context.drawTextWithShadow(textRenderer, ability, detailsX, y + 70,
                hiddenAbility ? 0xFFFFD84D : 0xFFFFFFFF);
        context.fill(x + 8, y + 81, x + WIDTH - 8, y + 82, 0xAA58DFF4);
        drawIv(context, textRenderer, "hp", pokemon.getIvs().getOrDefault(Stats.HP), x + 8, y + 85);
        drawIv(context, textRenderer, "attack", pokemon.getIvs().getOrDefault(Stats.ATTACK), x + 54, y + 85);
        drawIv(context, textRenderer, "defence", pokemon.getIvs().getOrDefault(Stats.DEFENCE), x + 101, y + 85);
        drawIv(context, textRenderer, "special_attack", pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK), x + 8, y + 95);
        drawIv(context, textRenderer, "special_defence", pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE), x + 54, y + 95);
        drawIv(context, textRenderer, "speed", pokemon.getIvs().getOrDefault(Stats.SPEED), x + 101, y + 95);
    }

    public static void renderMarkTooltip(DrawContext context, int mouseX, int mouseY) {
        var client = MinecraftClient.getInstance();
        Pokemon pokemon = CatchPreviewController.visible();
        if (pokemon == null || client.player == null || client.options.hudHidden) return;
        if (textPokemon != pokemon) refreshText();
        PreviewMarksRenderer.tooltip(context, mouseX, mouseY, left(context.getScaledWindowWidth()) + PreviewMarksRenderer.OFFSET_X,
                top(context.getScaledWindowHeight()) + PreviewMarksRenderer.OFFSET_Y);
    }

    private static void drawIv(DrawContext context, TextRenderer textRenderer, String statKey, int ivValue, int x, int y) {
        context.getMatrices().push();
        try {
            context.getMatrices().translate(x, y, 0.0F);
            context.getMatrices().scale(0.85F, 0.85F, 1.0F);
            Text statLabel = IV_LABELS.get(statKey);
            context.drawTextWithShadow(textRenderer, statLabel, 0, 0, 0xFFFFFFFF);
            Text ivNumber = ivValue >= 0 && ivValue < IV_VALUES.length ? IV_VALUES[ivValue] : Text.literal(Integer.toString(ivValue));
            context.drawTextWithShadow(textRenderer, ivNumber, textRenderer.getWidth(statLabel), 0, ivColor(ivValue));
        } finally {
            context.getMatrices().pop();
        }
    }

    private static int ivColor(int value) {
        if (value == 31) return 0xFFFFD84D;
        if (value >= 21) return 0xFF73E6A1;
        if (value >= 11) return 0xFFFFFFFF;
        return 0xFFFF6666;
    }

    private static Text natureText(Pokemon pokemon) {
        return pokemon.getNature() == null ? Text.literal("—") : label(pokemon.getNature().getDisplayName());
    }

    private static Text abilityText(Pokemon pokemon) {
        return pokemon.getAbility() == null ? Text.literal("—") : label(pokemon.getAbility().getDisplayName());
    }

    private static void drawScaled(DrawContext context, Identifier texture, int x, int y,
                                   int sourceWidth, int sourceHeight, int width, int height) {
        context.getMatrices().push();
        try {
            context.getMatrices().translate(x, y, 0.0F);
            context.getMatrices().scale(width / (float) sourceWidth, height / (float) sourceHeight, 1.0F);
            context.drawTexture(texture, 0, 0, 0.0F, 0.0F,
                    sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        } finally {
            context.getMatrices().pop();
        }
    }


    private static String gender(Gender gender) {
        if (gender == Gender.MALE) return "♂";
        if (gender == Gender.FEMALE) return "♀";
        return "—";
    }

    private static int genderColor(Gender gender) {
        if (gender == Gender.MALE) return 0xFF68A9FF;
        if (gender == Gender.FEMALE) return 0xFFFF86B8;
        return 0xFFD8DDE1;
    }
}
