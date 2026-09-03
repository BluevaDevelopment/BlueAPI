package net.blueva.foundation.menus;

import java.util.Locale;

/**
 * Which surface a menu is drawn on.
 */
public enum MenuRenderMode {

    /**
     * A chest inventory for Java players, a Bedrock form for Bedrock players.
     * The default, and the reason this API exists.
     */
    AUTO,

    /**
     * Always a chest inventory. Bedrock players get one too - Geyser renders
     * Java containers natively - it just looks like a chest rather than a
     * native Bedrock screen.
     */
    CHEST,

    /**
     * Always a Bedrock form. Java players cannot see one, so a menu marked
     * this way is skipped for them; use it only for menus that exist purely
     * to give Bedrock players a native alternative.
     */
    FORM;

    /**
     * @param value a name from a config, may be {@code null}
     * @return the matching mode, or {@link #AUTO}
     */
    public static MenuRenderMode parse(String value) {
        if (value == null) {
            return AUTO;
        }
        String name = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("INVENTORY".equals(name) || "GUI".equals(name)) {
            return CHEST;
        }
        if ("BEDROCK".equals(name)) {
            return FORM;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
