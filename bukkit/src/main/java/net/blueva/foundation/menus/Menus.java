package net.blueva.foundation.menus;

import net.blueva.foundation.config.ConfigSection;
import net.blueva.foundation.menus.bedrock.Bedrock;
import net.blueva.foundation.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Cross-edition menus: one definition, a chest inventory for Java players and
 * a native form for Bedrock ones.
 *
 * <pre>{@code
 * MenuDefinition warps = BlueFoundation.Menus.load(config.section("menu"));
 * BlueFoundation.Menus.register(warps);
 * BlueFoundation.Menus.open(plugin, player, "warps");
 * }</pre>
 *
 * <p>Which surface a player gets is decided per player at open time, so a
 * server with both editions on it needs one config, not two. Nothing here
 * requires Floodgate, Geyser or Cumulus on the classpath; see
 * {@link net.blueva.foundation.menus.bedrock.Forms} for how the Bedrock side
 * is delivered without them.</p>
 */
public class Menus {

    private static final Map<String, MenuDefinition> DEFINITIONS =
            new ConcurrentHashMap<String, MenuDefinition>();
    private static final Map<UUID, Open> OPEN =
            new ConcurrentHashMap<UUID, Open>();
    private static final Map<Plugin, MenuListener> LISTENERS =
            new ConcurrentHashMap<Plugin, MenuListener>();

    /** One player's open menu, and the plugin that put it there. */
    private static final class Open {
        private final Plugin plugin;
        private final MenuContext context;

        Open(Plugin plugin, MenuContext context) {
            this.plugin = plugin;
            this.context = context;
        }
    }

    protected Menus() {
    }

    /**
     * Parse a menu from a config section.
     *
     * @param section the {@code menu:} section, or the whole file's root
     * @return the definition
     * @see MenuLoader for the schema
     */
    public static MenuDefinition load(ConfigSection section) {
        return MenuLoader.load(null, section);
    }

    /**
     * Parse a menu from a config section, naming it explicitly.
     *
     * @param id      the menu's identifier, usually the file name
     * @param section the {@code menu:} section, or the whole file's root
     * @return the definition
     */
    public static MenuDefinition load(String id, ConfigSection section) {
        return MenuLoader.load(id, section);
    }

    /**
     * Parse and register in one step.
     *
     * @param id      the menu's identifier
     * @param section the config
     * @return the definition
     */
    public static MenuDefinition register(String id, ConfigSection section) {
        MenuDefinition definition = load(id, section);
        register(definition);
        return definition;
    }

    /**
     * Make a menu reachable by id, which is what {@code open_menu} actions and
     * {@link #open(Plugin, Player, String)} look up.
     *
     * @param definition the menu
     */
    public static void register(MenuDefinition definition) {
        if (definition != null && !definition.id().isEmpty()) {
            DEFINITIONS.put(definition.id(), definition);
        }
    }

    /**
     * @param definitions menus to register
     */
    public static void register(Collection<MenuDefinition> definitions) {
        if (definitions != null) {
            for (MenuDefinition definition : definitions) {
                register(definition);
            }
        }
    }

    /**
     * @param id a menu identifier
     * @return the registered menu, or {@code null}
     */
    public static MenuDefinition definition(String id) {
        return id == null ? null : DEFINITIONS.get(id);
    }

    /**
     * @param id a menu identifier
     */
    public static void unregister(String id) {
        if (id != null) {
            DEFINITIONS.remove(id);
        }
    }

    /**
     * Forget every registered menu. Worth calling before a config reload so
     * deleted menus do not survive it.
     */
    public static void unregisterAll() {
        DEFINITIONS.clear();
    }

    /**
     * Open a registered menu.
     *
     * @param plugin the plugin opening it
     * @param player who to show it to
     * @param id     the menu identifier
     * @return the view, or {@code null} if the menu is unknown or could not be shown
     */
    public static MenuContext open(Plugin plugin, Player player, String id) {
        MenuDefinition definition = definition(id);
        if (definition == null) {
            if (plugin != null) {
                plugin.getLogger().warning("[BlueFoundation] No menu registered under '" + id + "'.");
            }
            return null;
        }
        return open(plugin, new MenuContext(player, definition));
    }

    /**
     * Open a menu that is not registered, or one with entries and placeholders
     * already filled in.
     *
     * @param plugin  the plugin opening it
     * @param context the view to show
     * @return the view, or {@code null} if it could not be shown
     */
    public static MenuContext open(Plugin plugin, MenuContext context) {
        if (plugin == null || context == null || context.player() == null) {
            return null;
        }
        Player player = context.player();
        if (!player.isOnline()) {
            return null;
        }
        listener(plugin);

        MenuDefinition definition = context.definition();
        boolean wantsForm = definition.mode() == MenuRenderMode.FORM
                || (definition.mode() == MenuRenderMode.AUTO && Bedrock.isBedrockPlayer(player));

        if (wantsForm && FormRenderer.open(plugin, context)) {
            OPEN.put(player.getUniqueId(), new Open(plugin, context));
            return context;
        }
        if (definition.mode() == MenuRenderMode.FORM) {
            // Declared form-only and no transport took it. Drawing a chest here
            // would put a menu in front of a Java player that was written for
            // Bedrock, which is worse than nothing.
            plugin.getLogger().warning("[BlueFoundation] Menu '" + definition.id()
                    + "' is form-only but no Bedrock transport is available for "
                    + player.getName() + ".");
            return null;
        }

        ChestRenderer.open(plugin, context);
        OPEN.put(player.getUniqueId(), new Open(plugin, context));
        return context;
    }

    /**
     * Open a menu on the next tick.
     *
     * <p>Opening an inventory from inside {@code InventoryClickEvent} leaves
     * the client and server disagreeing about the cursor, so every menu that
     * opens as a result of a click goes through here.</p>
     *
     * @param plugin  the plugin opening it
     * @param context the view to show
     */
    public static void openLater(final Plugin plugin, final MenuContext context) {
        if (plugin == null || context == null || context.player() == null) {
            return;
        }
        Scheduler.runAtEntityLater(plugin, context.player(), new Runnable() {
            @Override
            public void run() {
                open(plugin, context);
            }
        }, 1L);
    }

    /**
     * Close whatever menu a player has open.
     *
     * @param player the player
     * @return whether they had one open
     */
    public static boolean close(Player player) {
        if (player == null) {
            return false;
        }
        Open open = OPEN.remove(player.getUniqueId());
        player.closeInventory();
        return open != null;
    }

    /**
     * @param player a player
     * @return the menu they have open, or {@code null}
     */
    public static MenuContext viewing(Player player) {
        Open open = player == null ? null : OPEN.get(player.getUniqueId());
        return open == null ? null : open.context;
    }

    /**
     * Install the resolver every menu string is passed through.
     *
     * @param resolver takes the viewing player and a line of text
     */
    public static void resolver(BiFunction<Player, String, String> resolver) {
        MenuPlaceholders.resolver(resolver);
    }

    /**
     * Register a handler for {@code custom:} actions under an id.
     *
     * @param id      the handler id, matched case-insensitively
     * @param handler the handler
     */
    public static void handler(String id, MenuActionHandler handler) {
        MenuActionExecutor.register(id, handler);
    }

    /**
     * @param id a handler id
     */
    public static void unregisterHandler(String id) {
        MenuActionExecutor.unregister(id);
    }

    /**
     * Whether this player would see a Bedrock form rather than a chest.
     *
     * @param player the player
     * @return whether they are on Bedrock Edition
     */
    public static boolean isBedrockPlayer(Player player) {
        return Bedrock.isBedrockPlayer(player);
    }

    /**
     * Release a plugin's listener and forget anyone still viewing its menus.
     * Worth calling from {@code onDisable}.
     *
     * @param plugin the plugin shutting down
     */
    public static void release(Plugin plugin) {
        MenuListener listener = LISTENERS.remove(plugin);
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        net.blueva.foundation.menus.bedrock.Forms.release(plugin);
        // Only this plugin's menus. Another plugin's are none of our business,
        // and closing them on our reload would be a bug for its users.
        for (Map.Entry<UUID, Open> entry : OPEN.entrySet()) {
            if (entry.getValue().plugin != plugin) {
                continue;
            }
            OPEN.remove(entry.getKey());
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.closeInventory();
            }
        }
    }

    static void forget(Player player, MenuHolder holder) {
        if (player == null) {
            return;
        }
        Open open = OPEN.get(player.getUniqueId());
        if (open == null) {
            return;
        }
        if (holder == null || holder.context() == open.context) {
            OPEN.remove(player.getUniqueId());
        }
    }

    private static void listener(Plugin plugin) {
        if (LISTENERS.containsKey(plugin)) {
            return;
        }
        synchronized (LISTENERS) {
            if (LISTENERS.containsKey(plugin)) {
                return;
            }
            MenuListener listener = new MenuListener(plugin);
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            LISTENERS.put(plugin, listener);
        }
    }
}
