package net.blueva.foundation.menus.bedrock;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * A Bedrock form: what a Bedrock player sees where a Java player would see a
 * chest inventory.
 *
 * <p>Forms are plain data plus their handlers. Nothing here talks to Geyser or
 * Floodgate; {@link Forms} picks a transport and calls
 * {@link #handle(Player, String)} when the client answers.</p>
 */
public abstract class Form {

    private final String title;
    private Consumer<Player> closedHandler;

    Form(String title, Consumer<Player> closedHandler) {
        this.title = title == null ? "" : title;
        this.closedHandler = closedHandler;
    }

    /**
     * @return the text in the form's title bar
     */
    public String title() {
        return title;
    }

    /**
     * @return which of the three Bedrock form kinds this is
     */
    public abstract FormType type();

    /**
     * @return the form serialized for the Bedrock client
     */
    public abstract String toJson();

    /**
     * Dispatch the client's answer to this form's handlers.
     *
     * <p>A closed form arrives as {@code null} or an empty string, which is
     * also what a malformed response is treated as: a player who dismissed a
     * form and a player whose client sent nonsense both want the same thing,
     * which is for the server not to act on their behalf.</p>
     *
     * @param player who answered
     * @param raw    the raw response payload
     */
    public abstract void handle(Player player, String raw);

    /**
     * Replace what runs when the player dismisses this form.
     *
     * @param closedHandler the handler, or {@code null} to do nothing
     */
    public void onClosed(Consumer<Player> closedHandler) {
        this.closedHandler = closedHandler;
    }

    /**
     * Run the closed handler, swallowing nothing: a handler that throws is the
     * caller's bug and should reach the caller's log.
     *
     * @param player who dismissed the form
     */
    protected void fireClosed(Player player) {
        if (closedHandler != null) {
            closedHandler.accept(player);
        }
    }

    /**
     * @param raw a response payload
     * @return whether it means "the player closed this without answering"
     */
    protected static boolean isClosed(String raw) {
        return raw == null || raw.trim().isEmpty() || "null".equals(raw.trim());
    }
}
