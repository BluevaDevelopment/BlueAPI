package net.blueva.foundation.menus;

import org.bukkit.entity.Player;

/**
 * Handles a {@link MenuActionType#CUSTOM} action.
 *
 * <p>Registered under an id, so a config can say
 * {@code "custom: shop buy 3"} and BlueFoundation routes {@code "buy 3"} to
 * whoever registered {@code shop}. This is the extension
 * point that keeps the shared action vocabulary from growing a case for every
 * plugin that uses it.</p>
 */
public interface MenuActionHandler {

    /**
     * @param player  who pressed the button
     * @param payload the rest of the action value, after the handler id
     * @param context the view the button belongs to
     * @return whether the action was handled; {@code false} logs a warning
     */
    boolean handle(Player player, String payload, MenuContext context);
}
