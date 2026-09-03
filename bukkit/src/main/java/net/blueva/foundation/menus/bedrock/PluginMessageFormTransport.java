package net.blueva.foundation.menus.bedrock;

import net.blueva.foundation.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Speaks the {@code floodgate:form} protocol directly, with no GeyserMC class
 * anywhere on the classpath.
 *
 * <p>Geyser handles this channel itself, in
 * {@code JavaCustomPayloadTranslator}: it reads a form type byte, a two-byte
 * form id and a JSON body, builds the form, and answers on the same channel
 * with the id followed by the client's raw response. That is the entire
 * contract, and it is one this class can hold up on its own.</p>
 *
 * <p>The catch, and the reason this is the last transport tried rather than
 * the first: Floodgate also listens on this channel, and a response to a form
 * it did not send makes it log {@code Couldn't find stored form for player}.
 * Harmless, but noisy, so it is only reached when neither the Floodgate nor
 * the Geyser API is available to do the same job quietly.</p>
 */
final class PluginMessageFormTransport implements FormTransport {

    static final String CHANNEL = "floodgate:form";

    /**
     * Form ids start high so they cannot realistically collide with
     * Floodgate's, which count up from zero for the same player. The top bit
     * stays clear: Floodgate's proxy path reads it as "this form came from the
     * proxy" and would keep the response there instead of forwarding it to us.
     */
    private static final int FIRST_ID = 0x4000;
    private static final int LAST_ID = Short.MAX_VALUE;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Map<Plugin, Registration> registrations =
            Collections.synchronizedMap(new HashMap<Plugin, Registration>());

    @Override
    public String name() {
        return "plugin-message";
    }

    @Override
    public boolean available(Plugin plugin, Player player) {
        return plugin != null && plugin.isEnabled() && player != null && player.isOnline();
    }

    @Override
    public boolean send(Plugin plugin, Player player, Form form) {
        Registration registration = registration(plugin);
        if (registration == null) {
            return false;
        }
        byte[] json = form.toJson().getBytes(UTF_8);
        if (json.length + 3 > Messenger.MAX_MESSAGE_SIZE) {
            plugin.getLogger().warning("[BlueFoundation] Bedrock form '" + form.title()
                    + "' is " + json.length + " bytes and will not fit in a plugin message;"
                    + " shorten it or paginate it.");
            return false;
        }

        short id = registration.nextId();
        registration.pending(player.getUniqueId()).put(Short.valueOf(id), form);

        byte[] data = new byte[json.length + 3];
        data[0] = form.type().wireId();
        data[1] = (byte) ((id >> 8) & 0xFF);
        data[2] = (byte) (id & 0xFF);
        System.arraycopy(json, 0, data, 3, json.length);

        try {
            player.sendPluginMessage(plugin, CHANNEL, data);
            return true;
        } catch (Throwable e) {
            registration.pending(player.getUniqueId()).remove(Short.valueOf(id));
            return false;
        }
    }

    /**
     * Forget everything pending for a plugin. Called when {@link Forms} is shut
     * down so a reload does not leave handlers pointing at an old class loader.
     *
     * @param plugin the plugin to release
     */
    void release(Plugin plugin) {
        Registration registration = registrations.remove(plugin);
        if (registration != null) {
            registration.close();
        }
    }

    private Registration registration(Plugin plugin) {
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        Registration existing = registrations.get(plugin);
        if (existing != null) {
            return existing;
        }
        synchronized (registrations) {
            existing = registrations.get(plugin);
            if (existing != null) {
                return existing;
            }
            try {
                Registration created = new Registration(plugin);
                registrations.put(plugin, created);
                return created;
            } catch (Throwable e) {
                plugin.getLogger().warning("[BlueFoundation] Could not open the " + CHANNEL
                        + " channel, Bedrock forms are unavailable: " + e.getMessage());
                return null;
            }
        }
    }

    /** One plugin's channel registration, id counter and pending forms. */
    private static final class Registration implements PluginMessageListener, Listener {

        private final Plugin plugin;
        private final AtomicInteger nextId = new AtomicInteger(FIRST_ID);
        private final Map<UUID, Map<Short, Form>> pending =
                new ConcurrentHashMap<UUID, Map<Short, Form>>();

        Registration(Plugin plugin) {
            this.plugin = plugin;
            Messenger messenger = Bukkit.getMessenger();
            messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
            messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        void close() {
            Messenger messenger = Bukkit.getMessenger();
            messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
            messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
            HandlerList.unregisterAll(this);
            pending.clear();
        }

        short nextId() {
            int id = nextId.getAndUpdate(new java.util.function.IntUnaryOperator() {
                @Override
                public int applyAsInt(int current) {
                    return current >= LAST_ID ? FIRST_ID : current + 1;
                }
            });
            return (short) id;
        }

        Map<Short, Form> pending(UUID uuid) {
            Map<Short, Form> forms = pending.get(uuid);
            if (forms == null) {
                forms = new ConcurrentHashMap<Short, Form>();
                Map<Short, Form> raced = pending.putIfAbsent(uuid, forms);
                if (raced != null) {
                    forms = raced;
                }
            }
            return forms;
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            pending.remove(event.getPlayer().getUniqueId());
        }

        @Override
        public void onPluginMessageReceived(String channel, final Player player, byte[] message) {
            if (!CHANNEL.equals(channel) || message == null || message.length < 2) {
                return;
            }
            short id = (short) (((message[0] & 0xFF) << 8) | (message[1] & 0xFF));
            Map<Short, Form> forms = pending.get(player.getUniqueId());
            if (forms == null) {
                return;
            }
            final Form form = forms.remove(Short.valueOf(id));
            if (form == null) {
                // Somebody else's form id on a shared channel. Not ours to answer.
                return;
            }
            final String raw = decode(message);
            if (Scheduler.isPrimaryThread()) {
                form.handle(player, raw);
                return;
            }
            Scheduler.runAtEntity(plugin, player, new Runnable() {
                @Override
                public void run() {
                    form.handle(player, raw);
                }
            });
        }

        private static String decode(byte[] message) {
            // A bare id with no payload is how the client says "I closed this".
            if (message.length <= 2) {
                return null;
            }
            return new String(message, 2, message.length - 2, UTF_8);
        }
    }
}
