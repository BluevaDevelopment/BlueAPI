package net.blueva.foundation.menus.dialogs;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps track of which custom-click id belongs to which
 * {@link DialogAction#callback} handler, and wires up the event that delivers
 * them.
 *
 * <p>The event class differs between the two server APIs and neither exists on
 * the version BlueFoundation compiles against, so it is registered through
 * {@code PluginManager#registerEvent} with a class looked up at runtime. That
 * is the supported way to listen for an event you cannot name at compile
 * time, and it keeps this working on 1.8 - where the class is simply absent
 * and nothing is registered.</p>
 */
final class DialogCallbacks {

    /**
     * A dialog the player dismissed never fires its callback, so ids would
     * accumulate for as long as the server runs. Capping the table and
     * dropping the least recently used entry bounds that; the cost is that a
     * button pressed after a thousand other dialogs have been shown does
     * nothing, which is a better failure than a slow leak.
     */
    private static final int MAX_PENDING = 1024;

    private static final Map<String, Pending> PENDING =
            Collections.synchronizedMap(new LinkedHashMap<String, Pending>(64, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Pending> eldest) {
                    return size() > MAX_PENDING;
                }
            });

    private static final Map<Plugin, Listener> LISTENERS =
            Collections.synchronizedMap(new LinkedHashMap<Plugin, Listener>());

    private static final AtomicLong COUNTER = new AtomicLong();

    /** What a backend knows how to read a single input out of. */
    interface ResponseSource {
        /**
         * @param input the field to read
         * @return its value as text, or {@code null} if absent
         */
        String read(DialogInput input);
    }

    private static final class Pending {
        private final Plugin plugin;
        private final Dialog dialog;
        private final DialogAction action;

        Pending(Plugin plugin, Dialog dialog, DialogAction action) {
            this.plugin = plugin;
            this.dialog = dialog;
            this.action = action;
        }
    }

    private DialogCallbacks() {
    }

    /**
     * Reserve an id for one callback button.
     *
     * @param plugin the plugin showing the dialog
     * @param dialog the dialog the button belongs to, for its input keys
     * @param action the callback action
     * @return the custom-click id, in {@code namespace:key} form
     */
    static String register(Plugin plugin, Dialog dialog, DialogAction action) {
        String key = sanitise(plugin.getName()) + "_" + COUNTER.incrementAndGet();
        String id = "bluefoundation:" + key;
        PENDING.put(id, new Pending(plugin, dialog, action));
        return id;
    }

    /**
     * Deliver a press to whoever registered the id.
     *
     * @param id     the custom-click id the client sent back
     * @param player who pressed
     * @param source how to read the dialog's inputs
     * @return whether the id was ours
     */
    static boolean fire(String id, final Player player, ResponseSource source) {
        final Pending entry = PENDING.remove(id);
        if (entry == null) {
            return false;
        }
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (DialogInput input : entry.dialog.inputs()) {
            String value = source == null ? null : source.read(input);
            if (value != null) {
                values.put(input.key(), value);
            }
        }
        final DialogResponse response = new DialogResponse(values);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    entry.action.callback().accept(player, response);
                } catch (Throwable e) {
                    entry.plugin.getLogger().warning(
                            "[BlueFoundation] Dialog callback threw: " + e);
                }
            }
        };
        if (Scheduler.isPrimaryThread()) {
            task.run();
        } else {
            Scheduler.runAtEntity(entry.plugin, player, task);
        }
        return true;
    }

    /**
     * Listen for custom clicks, once per plugin.
     *
     * @param plugin    the plugin that will own the registration
     * @param eventName the fully-qualified event class for this backend
     * @param executor  what to do with each event
     */
    static void listen(Plugin plugin, String eventName, final EventExecutor executor) {
        if (LISTENERS.containsKey(plugin)) {
            return;
        }
        Class<?> eventClass = Reflection.findClass(eventName);
        if (eventClass == null || !Event.class.isAssignableFrom(eventClass)) {
            return;
        }
        synchronized (LISTENERS) {
            if (LISTENERS.containsKey(plugin)) {
                return;
            }
            Listener listener = new Listener() {
            };
            @SuppressWarnings("unchecked")
            Class<? extends Event> typed = (Class<? extends Event>) eventClass;
            Bukkit.getPluginManager().registerEvent(typed, listener, EventPriority.NORMAL, executor, plugin);
            LISTENERS.put(plugin, listener);
        }
    }

    /**
     * Drop a plugin's listener and any callbacks it was still waiting on.
     *
     * @param plugin the plugin shutting down
     */
    static void release(Plugin plugin) {
        Listener listener = LISTENERS.remove(plugin);
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        synchronized (PENDING) {
            PENDING.values().removeIf(new java.util.function.Predicate<Pending>() {
                @Override
                public boolean test(Pending entry) {
                    return entry.plugin == plugin;
                }
            });
        }
    }

    /**
     * Namespaced keys accept only lowercase letters, digits and a few symbols,
     * and a plugin called "My Plugin!" would otherwise produce an id the
     * server rejects.
     */
    private static String sanitise(String name) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                out.append(c);
            }
        }
        return out.length() == 0 ? "plugin" : out.toString();
    }
}
