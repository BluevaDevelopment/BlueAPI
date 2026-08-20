package net.blueva.foundation.text;

import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.minimessage.MiniMessageParser;
import net.blueva.foundation.text.serializer.LegacySerializer;
import net.blueva.foundation.text.serializer.PlainSerializer;

/**
 * BlueFoundation text utilities backed by its own MiniMessage parser. Unlike
 * the Bukkit module, Velocity has Adventure natively at all times, so prefer
 * {@link AdventureText} or {@link net.blueva.foundation.messages.Messages}
 * when you need to actually render something - this class is for raw
 * MiniMessage-in/plain-or-legacy-out processing (e.g. building config values).
 */
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
