package fr.tropimon.catchpreview.preview;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbility;
import java.lang.reflect.Method;

/** Pokemon facts independent of text layout, using only official Cobblemon data. */
public final class PokemonDetailsReader {
    private static final Method ALPHA = optional("isAlpha");
    private static final Method SIZE = optional("getSizeCategory");

    private PokemonDetailsReader() {}

    public static boolean isHiddenAbility(Pokemon pokemon) {
        if (pokemon.getAbility() == null || pokemon.getForm() == null) return false;
        for (PotentialAbility potential : pokemon.getForm().getAbilities()) {
            if (potential instanceof HiddenAbility
                    && potential.getTemplate().getName().equals(pokemon.getAbility().getTemplate().getName())) return true;
        }
        return false;
    }

    public static boolean alpha(Pokemon pokemon) { return Boolean.TRUE.equals(read(ALPHA, pokemon)); }

    public static String size(Pokemon pokemon) {
        Object value = read(SIZE, pokemon);
        return value instanceof Enum<?> category ? category.name() : "";
    }

    private static Method optional(String name) {
        try { return Pokemon.class.getMethod(name); }
        catch (NoSuchMethodException absentOnOlderVersion) { return null; }
    }

    private static Object read(Method method, Pokemon pokemon) {
        if (method == null) return null;
        try { return method.invoke(pokemon); }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read official Cobblemon preview details", exception);
        }
    }
}
