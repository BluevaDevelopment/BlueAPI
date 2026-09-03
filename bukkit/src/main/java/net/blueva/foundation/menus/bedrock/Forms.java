package net.blueva.foundation.menus.bedrock;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sends Bedrock forms, picking a delivery route at runtime.
 *
 * <pre>{@code
 * BlueFoundation.Forms.send(plugin, player, SimpleForm.builder()
 *         .title("Warps")
 *         .content("Pick somewhere to go")
 *         .button("Spawn", p -> p.performCommand("spawn"))
 *         .button("Shop",  p -> p.performCommand("shop"))
 *         .build());
 * }</pre>
 *
 * <p>Three transports are tried in order: Floodgate's API, Geyser's API, then
 * the raw {@code floodgate:form} plugin message. BlueFoundation compiles
 * against none of them - the first two are reflective and the third is a
 * documented byte layout - so a consumer plugin needs no GeyserMC dependency,
 * no shading and no {@code softdepend} to send a form.</p>
 */
public class Forms {

    private static final PluginMessageFormTransport PLUGIN_MESSAGE = new PluginMessageFormTransport();

    private static final List<FormTransport> TRANSPORTS = Collections.unmodifiableList(
            new ArrayList<FormTransport>(java.util.Arrays.<FormTransport>asList(
                    Bedrock.floodgate(),
                    Bedrock.geyser(),
                    PLUGIN_MESSAGE)));

    protected Forms() {
    }

    /**
     * Show a form to a Bedrock player.
     *
     * @param plugin the plugin sending it, used for channels and scheduling
     * @param player the recipient
     * @param form   the form
     * @return {@code true} if a transport accepted it
     */
    public static boolean send(Plugin plugin, Player player, Form form) {
        if (plugin == null || player == null || form == null) {
            return false;
        }
        for (FormTransport transport : TRANSPORTS) {
            if (!transport.available(plugin, player)) {
                continue;
            }
            if (transport.send(plugin, player, form)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Which transport would carry a form to this player right now.
     *
     * <p>Useful in a startup log line or a diagnostics command: a server owner
     * seeing {@code plugin-message} knows why Floodgate is complaining about
     * unknown forms, and one seeing nothing at all knows Bedrock players will
     * fall back to chest menus.</p>
     *
     * @param plugin the plugin that would send
     * @param player the recipient
     * @return the transport's name, or {@code null} if none would work
     */
    public static String transportFor(Plugin plugin, Player player) {
        for (FormTransport transport : TRANSPORTS) {
            if (transport.available(plugin, player)) {
                return transport.name();
            }
        }
        return null;
    }

    /**
     * Drop the channel registration and any pending forms for a plugin.
     * Worth calling from {@code onDisable} when the plugin may be reloaded.
     *
     * @param plugin the plugin shutting down
     */
    public static void release(Plugin plugin) {
        PLUGIN_MESSAGE.release(plugin);
    }
}
