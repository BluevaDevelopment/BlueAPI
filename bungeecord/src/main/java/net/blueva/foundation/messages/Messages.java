package net.blueva.foundation.messages;

import net.blueva.foundation.text.Text;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Messaging helpers backed by BungeeCord's own {@link BaseComponent} chat API.
 * There is no Adventure {@code Audience} here, unlike Velocity - every method
 * takes the concrete BungeeCord type it needs. See {@link Text} for why the
 * deprecation warnings on this whole API are suppressed rather than fixed.
 */
@SuppressWarnings("deprecation")
public class Messages {

    protected Messages() {
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(Text.toBungee(message));
    }

    public static void broadcast(ProxyServer proxyServer, String message) {
        proxyServer.broadcast(Text.toBungee(message));
    }

    public static void actionBar(ProxiedPlayer player, String message) {
        if (player == null) {
            return;
        }
        player.sendMessage(ChatMessageType.ACTION_BAR, Text.toBungee(message));
    }

    public static void title(ProxiedPlayer player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) {
            return;
        }
        Title bungeeTitle = ProxyServer.getInstance().createTitle()
                .title(Text.toBungee(title))
                .subTitle(Text.toBungee(subtitle))
                .fadeIn(fadeInTicks)
                .stay(stayTicks)
                .fadeOut(fadeOutTicks);
        player.sendTitle(bungeeTitle);
    }
}
