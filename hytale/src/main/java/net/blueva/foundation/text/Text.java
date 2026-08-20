package net.blueva.foundation.text;

import com.hypixel.hytale.server.core.Message;
import net.blueva.foundation.text.component.BfColor;
import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.component.BfStyle;
import net.blueva.foundation.text.minimessage.MiniMessageParser;
import net.blueva.foundation.text.serializer.PlainSerializer;

/**
 * Bridges BlueFoundation's own MiniMessage parser to Hytale's {@link Message}
 * builder. Hytale has no Adventure-style component tree of its own - a
 * {@code Message} is a single mutable builder over a flat property bag, not a
 * tree - so {@link #toMessage(String)} walks {@link BfComponent} and rebuilds
 * the equivalent {@code Message} using the fluent API it does expose: bold,
 * italic and color. There is no strikethrough/underline/obfuscated setter on
 * {@code Message} in this server version, so those styles are dropped rather
 * than silently guessed at.
 */
public class Text {

    protected Text() {
    }

    public static BfComponent parse(String input) {
        if (isBlank(input)) {
            return BfComponent.empty();
        }
        return MiniMessageParser.parse(input);
    }

    public static String plain(String input) {
        return PlainSerializer.serialize(parse(input));
    }

    public static Message toMessage(String input) {
        return toMessage(parse(input));
    }

    public static Message toMessage(BfComponent component) {
        return toMessagePiece(component);
    }

    private static Message toMessagePiece(BfComponent component) {
        Message message = Message.raw(component.content());
        BfStyle style = component.style();
        if (style.bold()) {
            message = message.bold(true);
        }
        if (style.italic()) {
            message = message.italic(true);
        }
        BfColor color = style.color();
        if (color != null) {
            message = message.color(String.format("#%02X%02X%02X", color.red(), color.green(), color.blue()));
        }
        for (BfComponent child : component.children()) {
            message = message.insert(toMessagePiece(child));
        }
        return message;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
