package net.blueva.foundation.menus;

import java.util.Locale;

/**
 * Whether a menu shows a fixed set of buttons or a page of a longer list.
 */
public enum MenuLayout {

    /** Only the buttons declared in the menu. */
    STATIC,

    /**
     * The declared buttons plus a page of entries supplied at open time, with
     * previous/next navigation.
     */
    PAGINATED;

    /**
     * @param value a name from a config, may be {@code null}
     * @return the matching layout, or {@link #STATIC}
     */
    public static MenuLayout parse(String value) {
        if (value == null) {
            return STATIC;
        }
        String name = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        // Older spellings for the same thing.
        if ("LIST".equals(name) || "GRID".equals(name) || "DYNAMIC".equals(name)) {
            return PAGINATED;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return STATIC;
        }
    }
}
