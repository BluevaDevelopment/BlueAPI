package net.blueva.foundation.menus.bedrock;

import net.blueva.foundation.reflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Sends forms through Floodgate's own API, reached entirely by reflection.
 *
 * <p>This is the preferred transport when Floodgate is installed. Not because
 * it is the only one that works - {@link PluginMessageFormTransport} speaks
 * the same protocol - but because Floodgate then owns the form ids and the
 * pending-form table. Sending the plugin message ourselves means Floodgate
 * sees a response to a form it has no record of and logs an error for every
 * button a Bedrock player presses.</p>
 */
final class FloodgateFormTransport implements FormTransport {

    private volatile boolean resolved;
    private volatile boolean usable;
    private Method getInstance;
    private Method getPlayer;
    private Method sendForm;

    @Override
    public String name() {
        return "floodgate-api";
    }

    @Override
    public boolean available(Plugin plugin, Player player) {
        return resolve() && floodgatePlayer(player) != null;
    }

    @Override
    public boolean send(Plugin plugin, Player player, Form form) {
        if (!resolve()) {
            return false;
        }
        Object floodgatePlayer = floodgatePlayer(player);
        if (floodgatePlayer == null) {
            return false;
        }
        Object cumulusForm = CumulusBridge.toCumulus(plugin, player, form);
        if (cumulusForm == null) {
            return false;
        }
        try {
            Object result = sendForm.invoke(floodgatePlayer, cumulusForm);
            return !(result instanceof Boolean) || ((Boolean) result).booleanValue();
        } catch (Throwable e) {
            // A Floodgate build whose signatures moved is not fatal: the caller
            // falls through to the next transport.
            return false;
        }
    }

    /**
     * @return whether Floodgate knows this player as a Bedrock one
     */
    boolean isFloodgatePlayer(UUID uuid) {
        if (!resolve() || uuid == null) {
            return false;
        }
        try {
            Object api = getInstance.invoke(null);
            return api != null && getPlayer.invoke(api, uuid) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private Object floodgatePlayer(Player player) {
        if (player == null) {
            return null;
        }
        try {
            Object api = getInstance.invoke(null);
            return api == null ? null : getPlayer.invoke(api, player.getUniqueId());
        } catch (Throwable e) {
            return null;
        }
    }

    private boolean resolve() {
        if (resolved) {
            return usable;
        }
        synchronized (this) {
            if (resolved) {
                return usable;
            }
            resolved = true;
            usable = resolve0();
            return usable;
        }
    }

    private boolean resolve0() {
        Class<?> formClass = CumulusBridge.formClass();
        Class<?> apiClass = Reflection.findClass("org.geysermc.floodgate.api.FloodgateApi");
        Class<?> playerClass = Reflection.findClass("org.geysermc.floodgate.api.player.FloodgatePlayer");
        if (formClass == null || apiClass == null || playerClass == null) {
            return false;
        }
        getInstance = Reflection.method(apiClass, "getInstance");
        getPlayer = Reflection.method(apiClass, "getPlayer", UUID.class);
        sendForm = Reflection.method(playerClass, "sendForm", formClass);
        return getInstance != null && getPlayer != null && sendForm != null;
    }
}
