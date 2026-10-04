package fr.tropimon.catchpreview.ui;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.pokemon.Pokemon;
import fr.tropimon.catchpreview.mixin.MarkTextAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Owned marks only, never potential marks. Rebuilt on data/language/tooltip-width changes. */
final class PreviewMarksRenderer {
    private static final Identifier ICON = Identifier.of("cobblemon", "textures/gui/summary/summary_tab_icon_marks.png");
    static final int OFFSET_X = 49;
    static final int OFFSET_Y = 23;
    private static final int SIZE = 8;
    private static List<Mark> marks = List.of();
    private static Mark active;
    private static Language language;
    private static int tooltipWidth;
    private static List<OrderedText> allTooltip = List.of();
    private PreviewMarksRenderer() {}

    static void refresh(Pokemon pokemon) {
        var client = MinecraftClient.getInstance();
        int width = Math.max(40, Math.min(210, client.getWindow().getScaledWidth() - 24));
        var owned = pokemon == null ? java.util.Set.<Mark>of() : pokemon.getMarks();
        Mark equipped = pokemon == null ? null : pokemon.getActiveMark();
        if (language == Language.getInstance() && active == equipped && tooltipWidth == width
                && marks.size() == owned.size() && owned.containsAll(marks)) return;
        active = equipped;
        language = Language.getInstance();
        tooltipWidth = width;
        var sorted = new ArrayList<>(owned);
        sorted.sort(Comparator.comparing((Mark mark) -> mark != equipped)
                .thenComparing(mark -> mark.getIdentifier().toString()));
        marks = List.copyOf(sorted);
        var all = new ArrayList<OrderedText>();
        if (marks.isEmpty()) all.addAll(client.textRenderer.wrapLines(
                Text.translatable("gui.tropimon_catch_preview.no_marks"), width));
        for (int i = 0; i < marks.size(); i++) {
            var text = (MarkTextAccessor) (Object) marks.get(i);
            if (i > 0) all.add(Text.empty().asOrderedText());
            var lines = new ArrayList<OrderedText>();
            lines.addAll(client.textRenderer.wrapLines(text.catchPreview$name().formatted(Formatting.GOLD), width));
            lines.addAll(client.textRenderer.wrapLines(text.catchPreview$description(), width));
            all.addAll(lines);
        }
        allTooltip = List.copyOf(all);
    }

    static void draw(DrawContext context, int x, int y) {
        context.drawTexture(ICON, x, y, SIZE, SIZE, 0, 0, 17, 17, 17, 17);
    }

    static void tooltip(DrawContext context, int mouseX, int mouseY, int x, int y) {
        if (mouseY >= y && mouseY < y + SIZE && mouseX >= x && mouseX < x + SIZE)
            context.drawOrderedTooltip(MinecraftClient.getInstance().textRenderer, allTooltip, mouseX, mouseY);
    }
}
