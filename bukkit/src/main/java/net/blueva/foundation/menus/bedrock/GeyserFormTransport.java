package net.blueva.foundation.menus.bedrock;

import net.blueva.foundation.reflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Sends forms through Geyser's own API, when Geyser is running in the same JVM.
 *
 * <p>This covers the setup neither of the other transports does: Geyser as a
 * Spigot plugin with Floodgate absent, where the Bedrock player arrived
 * through an online-mode account link. There is no {@code floodgate:form}
 * channel to speak to and no Floodgate API to call, but Geyser is right there
 * holding the session.</p>
 */
final class GeyserFormTransport implements FormTransport {

    private volatile boolean resolved;
    private volatile boolean usable;
    private Method api;
    private Method isBedrockPlayer;
    private Method sendForm;

    @Override
    public String name() {
        return "geyser-api";
    }

    @Override
    public boolean available(Plugin plugin, Player player) {
        return player != null && isBedrockPlayer(player.getUniqueId());
    }

    @Override
    public boolean send(Plugin plugin, Player player, Form form) {
        if (!resolve()) {
            return false;
        }
        Object cumulusForm = CumulusBridge.toCumulus(plugin, player, form);
        if (cumulusForm == null) {
            return false;
        }
        try {
            Object instance = api.invoke(null);
            if (instance == null) {
                return false;
            }
            Object result = sendForm.invoke(instance, player.getUniqueId(), cumulusForm);
            return !(result instanceof Boolean) || ((Boolean) result).booleanValue();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * @return whether Geyser holds a Bedrock session for this UUID
     */
    boolean isBedrockPlayer(UUID uuid) {
        if (!resolve() || uuid == null) {
            return false;
        }
        try {
            Object instance = api.invoke(null);
            if (instance == null) {
                return false;
            }
            Object result = isBedrockPlayer.invoke(instance, uuid);
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (Throwable e) {
            return false;
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
        Class<?> apiClass = Reflection.findClass("org.geysermc.geyser.api.GeyserApi");
        if (formClass == null || apiClass == null) {
            return false;
        }
        api = Reflection.method(apiClass, "api");
        // Both of these are inherited from org.geysermc.api.GeyserApiBase, which
        // Reflection#method finds because it goes through Class#getMethod.
        isBedrockPlayer = Reflection.method(apiClass, "isBedrockPlayer", UUID.class);
        sendForm = Reflection.method(apiClass, "sendForm", UUID.class, formClass);
        return api != null && isBedrockPlayer != null && sendForm != null;
    }
}
