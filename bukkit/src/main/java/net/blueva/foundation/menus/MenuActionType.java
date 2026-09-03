package net.blueva.foundation.menus;

import java.util.Locale;

/**
 * What a menu action does when its button is pressed.
 *
 * <p>Deliberately small. Anything a plugin needs beyond this list goes through
 * {@link #CUSTOM}, which hands the value to a handler the plugin registered -
 * that way the shared vocabulary stays shared, and BlueFoundation never grows
 * an action type that only one plugin will ever use.</p>
 */
public enum MenuActionType {

    /** Run the value as a command, as the player. */
    COMMAND,

    /** Run the value as a command, from the console. */
    CONSOLE,

    /** Send the value to the player as a message. */
    MESSAGE,

    /** Open the menu whose id is the value. */
    OPEN_MENU,

    /** Go back to the menu the player came from. */
    BACK,

    /** Close whatever is open. */
    CLOSE,

    /**
     * Move within a paginated menu. The value is {@code next},
     * {@code previous}, {@code first}, {@code last} or an absolute page number
     * counting from one.
     */
    PAGE,

    /** Play the sound named by the value to the player. */
    SOUND,

    /**
     * Hand the value to a handler registered under an id.
     *
     * <p>The value is {@code <handler-id> <payload>}, so one plugin can own a
     * whole namespace of actions with a single registration.</p>
     */
    CUSTOM,

    /** Do nothing. What an unparseable action becomes. */
    NONE;

    /**
     * Parse a type name, accepting the older spellings that menu configs in
     * the wild already use alongside the canonical ones.
     *
     * @param value the name from a config, may be {@code null}
     * @return the matching type, or {@link #NONE}
     */
    public static MenuActionType parse(String value) {
        if (value == null) {
            return NONE;
        }
        String name = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (name.isEmpty()) {
            return NONE;
        }
        // Older spellings, kept so existing menu configs keep loading.
        if ("PLAYER".equals(name) || "PLAYER_COMMAND".equals(name)) {
            return COMMAND;
        }
        if ("MENU".equals(name)) {
            return OPEN_MENU;
        }
        if ("MODULE".equals(name)) {
            return CUSTOM;
        }
        if ("BROADCAST".equals(name) || "TELL".equals(name)) {
            return MESSAGE;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
