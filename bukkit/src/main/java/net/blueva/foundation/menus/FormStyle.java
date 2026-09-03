package net.blueva.foundation.menus;

import java.util.Locale;

/**
 * Which Bedrock form a menu becomes when it is drawn for a Bedrock player.
 */
public enum FormStyle {

    /** A scrollable list of buttons. The right answer for almost every menu. */
    SIMPLE,

    /**
     * Exactly two buttons. Suits confirmations; a menu declaring this style
     * with more than two visible buttons falls back to {@link #SIMPLE} rather
     * than dropping the extras.
     */
    MODAL;

    /**
     * @param value a name from a config, may be {@code null}
     * @return the matching style, or {@link #SIMPLE}
     */
    public static FormStyle parse(String value) {
        if (value == null) {
            return SIMPLE;
        }
        String name = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("MODAL_FORM".equals(name) || "CONFIRM".equals(name) || "CONFIRMATION".equals(name)) {
            return MODAL;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return SIMPLE;
        }
    }
}
