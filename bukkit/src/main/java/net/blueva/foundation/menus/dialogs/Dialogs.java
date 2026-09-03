package net.blueva.foundation.menus.dialogs;

import net.blueva.foundation.version.Version;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * Server-drawn dialog screens, for Minecraft 1.21.6 and newer.
 *
 * <pre>{@code
 * if (!BlueFoundation.Dialogs.supported()) {
 *     player.sendMessage("This server is too old for dialogs.");
 *     return;
 * }
 * BlueFoundation.Dialogs.show(plugin, player, Dialog.confirmation("<red>Delete?")
 *         .body("<gray>This cannot be undone.")
 *         .yes(DialogButton.of("<red>Delete", DialogAction.callback((p, r) -> delete(p))))
 *         .no(DialogButton.of("<gray>Cancel"))
 *         .build());
 * }</pre>
 *
 * <p>Two server APIs can draw a dialog and no server has both: Spigot's, in
 * {@code net.md_5.bungee.api.dialog}, and Paper's, in
 * {@code io.papermc.paper.registry.data.dialog}. Whichever is present is used.
 * Neither exists before 1.21.6, and there is no way to fake one on an older
 * client - so on an older server {@link #show} throws
 * {@link DialogUnsupportedException} and says so in the console once, rather
 * than failing silently.</p>
 *
 * <p><strong>Dialogs are a Java Edition feature.</strong> A Bedrock player
 * connected through Geyser will see one, because Geyser translates the packet
 * into a native Bedrock form, but that is Geyser's doing and it drops what a
 * form cannot draw. For a menu meant to look right on both editions, use
 * {@link net.blueva.foundation.menus.Menus}.</p>
 */
public class Dialogs {

    private static final List<DialogBackend> BACKENDS = Arrays.<DialogBackend>asList(
            new PaperDialogBackend(), new SpigotDialogBackend());

    private static volatile boolean warned;

    protected Dialogs() {
    }

    /**
     * @return whether this server can draw dialogs at all
     */
    public static boolean supported() {
        return backend() != null;
    }

    /**
     * @return the name of the API that would draw a dialog, or {@code null}
     */
    public static String backendName() {
        DialogBackend backend = backend();
        return backend == null ? null : backend.name();
    }

    /**
     * Show a dialog.
     *
     * @param plugin the plugin showing it, which owns the callback listener
     * @param player the viewer
     * @param dialog the dialog
     * @throws DialogUnsupportedException if the server predates 1.21.6 or
     *                                    exposes neither dialog API
     */
    public static void show(Plugin plugin, Player player, Dialog dialog) {
        if (plugin == null || player == null || dialog == null) {
            return;
        }
        DialogBackend backend = backend();
        if (backend == null) {
            warn(plugin);
            throw new DialogUnsupportedException(unsupportedMessage());
        }
        if (!backend.show(plugin, player, dialog)) {
            throw new DialogUnsupportedException(
                    "The " + backend.name() + " dialog API refused to show '" + dialog.title() + "'.");
        }
    }

    /**
     * Show a dialog, or report that this server cannot.
     *
     * <p>The same as {@link #show} without the exception, for callers who have
     * a reasonable fallback - a chest menu, a chat prompt - and would rather
     * branch than catch.</p>
     *
     * @param plugin the plugin showing it
     * @param player the viewer
     * @param dialog the dialog
     * @return whether it was shown
     */
    public static boolean trySend(Plugin plugin, Player player, Dialog dialog) {
        if (plugin == null || player == null || dialog == null) {
            return false;
        }
        DialogBackend backend = backend();
        if (backend == null) {
            warn(plugin);
            return false;
        }
        return backend.show(plugin, player, dialog);
    }

    /**
     * Close whatever dialog a player has open.
     *
     * @param player the viewer
     * @return whether the server was asked to close one
     */
    public static boolean clear(Player player) {
        DialogBackend backend = backend();
        return backend != null && player != null && backend.clear(player);
    }

    /**
     * Drop a plugin's callback listener and any pending callbacks. Worth
     * calling from {@code onDisable}.
     *
     * @param plugin the plugin shutting down
     */
    public static void release(Plugin plugin) {
        DialogCallbacks.release(plugin);
    }

    private static DialogBackend backend() {
        for (DialogBackend backend : BACKENDS) {
            if (backend.available()) {
                return backend;
            }
        }
        return null;
    }

    /**
     * Say once, in the console, why nothing appeared. A plugin that offers a
     * dialog on every menu would otherwise either spam the log or leave the
     * server owner with no idea what went wrong.
     */
    private static void warn(Plugin plugin) {
        if (warned) {
            return;
        }
        warned = true;
        plugin.getLogger().warning("[BlueFoundation] " + unsupportedMessage());
    }

    private static String unsupportedMessage() {
        return "Dialogs need Minecraft 1.21.6 or newer with either Spigot's"
                + " net.md_5.bungee.api.dialog API or Paper's"
                + " io.papermc.paper.registry.data.dialog API; this server is running "
                + Version.bukkitVersion() + " and has neither.";
    }
}
