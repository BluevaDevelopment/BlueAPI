package net.blueva.foundation.menus;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Text substitution for menus.
 *
 * <p>BlueFoundation ships no placeholder engine and deliberately does not
 * depend on one. What it offers is a single hook: install a resolver once and
 * every menu string - titles, names, lore, form bodies, action values and
 * visibility conditions - goes through it. A plugin with PlaceholderAPI wires
 * it in one line; a plugin without one loses nothing.</p>
 *
 * <pre>{@code
 * BlueFoundation.Menus.resolver((player, text) ->
 *         PlaceholderAPI.setPlaceholders(player, text));
 * }</pre>
 *
 * <p>Order matters and is fixed: pagination placeholders first, then the
 * context's own literal replacements, then the global resolver. That way a
 * caller's {@code {house}} value can itself contain {@code %papi_%}
 * placeholders and still be resolved.</p>
 */
public final class MenuPlaceholders {

    private static volatile BiFunction<Player, String, String> resolver;

    private MenuPlaceholders() {
    }

    /**
     * Install the resolver every menu string is passed through.
     *
     * @param resolver takes the viewing player and a line of text; may be
     *                 {@code null} to remove the hook
     */
    public static void resolver(BiFunction<Player, String, String> resolver) {
        MenuPlaceholders.resolver = resolver;
    }

    /**
     * @return the installed resolver, or {@code null}
     */
    public static BiFunction<Player, String, String> resolver() {
        return resolver;
    }

    /**
     * Run one line through the whole substitution chain.
     *
     * @param player       the viewer, may be {@code null}
     * @param text         the line, may be {@code null}
     * @param replacements literal replacements, may be {@code null}
     * @param pagination   page state, may be {@code null}
     * @return the resolved line
     */
    public static String apply(Player player, String text,
                               Map<String, String> replacements, Pagination pagination) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        if (pagination != null) {
            result = pagination.apply(result);
        }
        if (replacements != null && !replacements.isEmpty()) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                if (entry.getKey() != null) {
                    result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
                }
            }
        }
        BiFunction<Player, String, String> hook = resolver;
        if (hook != null) {
            try {
                String resolved = hook.apply(player, result);
                if (resolved != null) {
                    result = resolved;
                }
            } catch (Throwable e) {
                // A placeholder plugin throwing is not a reason for the menu
                // not to open; the unresolved text is a better outcome.
            }
        }
        return result;
    }

    /**
     * Run every line of a list through the substitution chain.
     *
     * @param player       the viewer, may be {@code null}
     * @param lines        the lines, may be {@code null}
     * @param replacements literal replacements, may be {@code null}
     * @param pagination   page state, may be {@code null}
     * @return the resolved lines, never {@code null}
     */
    public static List<String> apply(Player player, List<String> lines,
                                     Map<String, String> replacements, Pagination pagination) {
        List<String> result = new ArrayList<String>();
        if (lines == null) {
            return result;
        }
        for (String line : lines) {
            result.add(apply(player, line, replacements, pagination));
        }
        return result;
    }

    /**
     * Read a resolved condition as a boolean.
     *
     * <p>Anything other than a recognised false is true, so a condition whose
     * placeholder failed to resolve leaves the button visible rather than
     * silently emptying a menu.</p>
     *
     * @param value the resolved condition, may be {@code null}
     * @return whether it counts as true
     */
    public static boolean isTrue(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return true;
        }
        return !("false".equals(trimmed) || "0".equals(trimmed) || "no".equals(trimmed)
                || "off".equals(trimmed) || "null".equals(trimmed));
    }
}
