package net.blueva.foundation.menus.dialogs;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * One server API capable of showing a dialog.
 *
 * <p>Two exist: Spigot's, in {@code net.md_5.bungee.api.dialog}, and Paper's,
 * in {@code io.papermc.paper.registry.data.dialog}. They are not the same API
 * and no server has both, so {@link Dialogs} asks each in turn.</p>
 */
interface DialogBackend {

    /**
     * @return a short name for logs
     */
    String name();

    /**
     * @return whether this server exposes the API this backend drives
     */
    boolean available();

    /**
     * Draw the dialog for one player.
     *
     * @param plugin the plugin showing it, which owns the callback listener
     * @param player the viewer
     * @param dialog the dialog
     * @return whether it was shown
     */
    boolean show(Plugin plugin, Player player, Dialog dialog);

    /**
     * Close whatever dialog the player has open.
     *
     * @param player the viewer
     * @return whether the call was made
     */
    boolean clear(Player player);
}
