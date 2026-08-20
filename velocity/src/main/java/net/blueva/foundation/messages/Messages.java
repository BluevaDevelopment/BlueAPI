package net.blueva.foundation.messages;

import com.velocitypowered.api.proxy.ProxyServer;
import net.blueva.foundation.text.AdventureText;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;

import java.time.Duration;

/**
 * Messaging helpers backed by native Adventure {@link Audience}. Every
 * Velocity {@code Player}, {@code ConsoleCommandSource} and {@code ProxyServer}
 * itself already implements {@code Audience}, so unlike Bukkit there is no
 * fallback path to maintain here.
 */
public class Messages {

    protected Messages() {
    }

    public static void send(Audience audience, String message) {
        if (audience == null) {
            return;
        }
        audience.sendMessage(AdventureText.component(message));
    }

    /** {@link ProxyServer} is itself an {@link Audience} that forwards to every connected player and the console. */
    public static void broadcast(ProxyServer proxyServer, String message) {
        send(proxyServer, message);
    }

    public static void actionBar(Audience audience, String message) {
        if (audience == null) {
            return;
        }
        audience.sendActionBar(AdventureText.component(message));
    }

    public static void title(Audience audience, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (audience == null) {
            return;
        }
        Title.Times times = Title.Times.times(ticks(fadeInTicks), ticks(stayTicks), ticks(fadeOutTicks));
        audience.showTitle(Title.title(AdventureText.component(title), AdventureText.component(subtitle), times));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }
}
