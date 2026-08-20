package net.blueva.foundation.messages;

import com.hypixel.hytale.server.core.receiver.IMessageReceiver;
import net.blueva.foundation.text.Text;

/**
 * Messaging helper over {@link IMessageReceiver} - the interface
 * {@code CommandSender} (and anything else that can receive a {@link
 * com.hypixel.hytale.server.core.Message}) implements. There is no broadcast
 * or action-bar/title concept exposed at this level of the API.
 */
public class Messages {

    protected Messages() {
    }

    public static void send(IMessageReceiver receiver, String message) {
        if (receiver == null) {
            return;
        }
        receiver.sendMessage(Text.toMessage(message));
    }
}
