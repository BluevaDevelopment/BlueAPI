package net.blueva.foundation.menus;

import org.bukkit.event.inventory.ClickType;

import java.util.Locale;

/**
 * One thing that happens when a button is pressed.
 *
 * <p>Config files may write an action three ways, all of which parse here:</p>
 *
 * <pre>{@code
 * actions:
 *   - "menu: flags_safety"          # short form, the shared spelling
 *   - "MENU;flags_safety"           # the older flat form, still accepted
 *   - type: open_menu               # the mapping form, the only one that can filter clicks
 *     value: flags_safety
 *     click: RIGHT
 * }</pre>
 */
public final class MenuAction {

    private final MenuActionType type;
    private final String value;
    private final String click;

    /**
     * @param type  what to do
     * @param value the argument, may be {@code null}
     * @param click the click that triggers it, or {@code null} for any click
     */
    public MenuAction(MenuActionType type, String value, String click) {
        this.type = type == null ? MenuActionType.NONE : type;
        this.value = value == null ? "" : value;
        this.click = click == null || click.trim().isEmpty()
                ? null
                : click.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * @param type  what to do
     * @param value the argument
     * @return an action that runs on any click
     */
    public static MenuAction of(MenuActionType type, String value) {
        return new MenuAction(type, value, null);
    }

    /**
     * Parse the short or flat form: {@code "menu: shop"}, {@code "MENU;shop"}
     * or a bare {@code "close"}.
     *
     * @param raw the line from a config
     * @return the parsed action, never {@code null}
     */
    public static MenuAction parse(String raw) {
        if (raw == null) {
            return of(MenuActionType.NONE, "");
        }
        String line = raw.trim();
        if (line.isEmpty()) {
            return of(MenuActionType.NONE, "");
        }

        int separator = indexOfSeparator(line);
        if (separator < 0) {
            // A bare keyword: "close", "back".
            return of(MenuActionType.parse(line), "");
        }

        String type = line.substring(0, separator).trim();
        String value = line.substring(separator + 1).trim();
        MenuActionType parsed = MenuActionType.parse(type);
        if (parsed == MenuActionType.NONE) {
            // Not a type at all - treat the whole line as a command, which is
            // what a config author writing "- spawn" almost certainly meant.
            return of(MenuActionType.COMMAND, line);
        }
        return of(parsed, value);
    }

    /**
     * Find the {@code :} or {@code ;} that splits type from value, ignoring a
     * {@code :} that is part of the value itself - {@code "message: <red>hi"}
     * splits at the first colon, but {@code "command: warp a:b"} must not
     * split at the second.
     */
    private static int indexOfSeparator(String line) {
        int semicolon = line.indexOf(';');
        int colon = line.indexOf(':');
        if (semicolon < 0) {
            return colon;
        }
        if (colon < 0) {
            return semicolon;
        }
        return Math.min(semicolon, colon);
    }

    /**
     * @return what this action does
     */
    public MenuActionType type() {
        return type;
    }

    /**
     * @return the argument, never {@code null}
     */
    public String value() {
        return value;
    }

    /**
     * @return the click name this action is limited to, or {@code null}
     */
    public String click() {
        return click;
    }

    /**
     * Whether this action should run for a given click.
     *
     * <p>Click filters are matched by name rather than against
     * {@link ClickType} constants, because which constants exist has changed
     * across the versions BlueFoundation supports and a config should not stop
     * loading because a server is old. Unknown names simply never match.</p>
     *
     * @param clickType the click, or {@code null} when the menu is a Bedrock
     *                  form and there is no such thing as a right click
     * @return whether to run
     */
    public boolean matches(ClickType clickType) {
        if (click == null || "ANY".equals(click) || "ALL".equals(click)) {
            return true;
        }
        if (clickType == null) {
            // Forms have one kind of press. Anything asking for a specific
            // click is a Java-only refinement and is skipped rather than
            // fired for the wrong reason.
            return false;
        }
        String name = clickType.name();
        if (click.equals(name)) {
            return true;
        }
        if ("LEFT".equals(click)) {
            return "LEFT".equals(name) || "SHIFT_LEFT".equals(name) || "DOUBLE_CLICK".equals(name);
        }
        if ("RIGHT".equals(click)) {
            return "RIGHT".equals(name) || "SHIFT_RIGHT".equals(name);
        }
        if ("SHIFT".equals(click)) {
            return "SHIFT_LEFT".equals(name) || "SHIFT_RIGHT".equals(name);
        }
        if ("MIDDLE".equals(click)) {
            return "MIDDLE".equals(name) || "CREATIVE".equals(name);
        }
        return false;
    }

    /**
     * @return a copy with placeholders applied to the value
     */
    MenuAction withValue(String newValue) {
        return new MenuAction(type, newValue, click);
    }

    @Override
    public String toString() {
        return "MenuAction[" + type + (value.isEmpty() ? "" : ": " + value)
                + (click == null ? "" : ", click=" + click) + "]";
    }
}
