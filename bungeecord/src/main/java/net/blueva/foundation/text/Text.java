package net.blueva.foundation.text;

import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.minimessage.MiniMessageParser;
import net.blueva.foundation.text.serializer.LegacySerializer;
import net.blueva.foundation.text.serializer.PlainSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * BlueFoundation text utilities backed by its own MiniMessage parser.
 * BungeeCord has no Adventure of its own, so rendering goes through its
 * {@code net.md_5.bungee.api.chat} component API - see {@link #toBungee(String)}
 * and {@link net.blueva.foundation.messages.Messages}.
 *
 * <p>BungeeCord marked the whole chat package {@code @Deprecated} without
 * shipping a first-party replacement (no {@code CommandSender.sendMessage}
 * overload exists for anything else); the suppression below reflects that,
 * not an oversight. A server that adds {@code net.kyori:adventure-platform-bungeecord}
 * can bridge {@code CommandSender}/{@code ProxiedPlayer} to a real Adventure
 * {@code Audience} itself and skip this class entirely.</p>
 */
@SuppressWarnings("deprecation")
public class Text {

    protected Text() {
    }

    public static BfComponent parse(String message) {
        if (isBlank(message)) {
            return BfComponent.empty();
        }
        return MiniMessageParser.parse(message);
    }

    public static String legacySection(String message) {
        return LegacySerializer.serialize(parse(message));
    }

    public static String plain(String message) {
        return PlainSerializer.serialize(parse(message));
    }

    /** Parses MiniMessage/legacy input and renders it as BungeeCord's own component array. */
    public static BaseComponent[] toBungee(String message) {
        return TextComponent.fromLegacyText(legacySection(message), ChatColor.WHITE);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
