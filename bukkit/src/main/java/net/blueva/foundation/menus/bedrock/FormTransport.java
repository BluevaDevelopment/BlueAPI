package net.blueva.foundation.menus.bedrock;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * One way of getting a {@link Form} onto a Bedrock client's screen.
 *
 * <p>{@link Forms} holds several of these and uses the first that reports
 * itself usable for the player at hand, so a server that has Floodgate's API
 * on the classpath gets the clean path and one that does not still gets a
 * working form.</p>
 */
public interface FormTransport {

    /**
     * @return a short name for logs
     */
    String name();

    /**
     * Whether this transport can deliver to this player right now.
     *
     * @param plugin the plugin sending the form
     * @param player the recipient
     * @return {@code true} if {@link #send} is worth attempting
     */
    boolean available(Plugin plugin, Player player);

    /**
     * Send the form and arrange for {@link Form#handle(Player, String)} to run
     * when the client answers.
     *
     * @param plugin the plugin sending the form
     * @param player the recipient
     * @param form   the form
     * @return {@code true} if the form was handed off to the client
     */
    boolean send(Plugin plugin, Player player, Form form);
}
