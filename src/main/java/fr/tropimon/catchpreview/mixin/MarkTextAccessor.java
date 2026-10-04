package fr.tropimon.catchpreview.mixin;

import com.cobblemon.mod.common.api.mark.Mark;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Select the official translated overloads (Kotlin also exposes String getters of the same name). */
@Mixin(value = Mark.class, remap = false)
public interface MarkTextAccessor {
    @Invoker("getName")
    MutableText catchPreview$name();

    @Invoker("getDescription")
    MutableText catchPreview$description();
}
