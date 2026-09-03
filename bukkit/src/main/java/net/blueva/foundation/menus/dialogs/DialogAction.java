package net.blueva.foundation.menus.dialogs;

import java.util.function.BiConsumer;

import org.bukkit.entity.Player;

/**
 * What pressing a dialog button does.
 *
 * <p>Most kinds are client-side: the client runs the command, opens the URL or
 * copies the text without the server hearing about it. {@link Kind#CALLBACK}
 * is the exception and the interesting one - it sends a custom-click packet
 * back, carrying whatever the player typed into the dialog's inputs, which is
 * how a dialog becomes a form the server can read.</p>
 */
public final class DialogAction {

    /** The kinds of thing a dialog button can do. */
    public enum Kind {
        /** Run the value as a command, as the player. */
        RUN_COMMAND,
        /** Put the value in the player's chat box without sending it. */
        SUGGEST_COMMAND,
        /** Open the value as a URL, after the client's usual warning. */
        OPEN_URL,
        /** Copy the value to the player's clipboard. */
        COPY_TO_CLIPBOARD,
        /** Send the dialog's inputs back to the server. */
        CALLBACK,
        /** Nothing; the button just closes the dialog. */
        NONE
    }

    private final Kind kind;
    private final String value;
    private final BiConsumer<Player, DialogResponse> callback;

    private DialogAction(Kind kind, String value, BiConsumer<Player, DialogResponse> callback) {
        this.kind = kind;
        this.value = value == null ? "" : value;
        this.callback = callback;
    }

    /**
     * @param command the command, with or without a leading slash
     * @return an action that runs it as the player
     */
    public static DialogAction runCommand(String command) {
        return new DialogAction(Kind.RUN_COMMAND, command, null);
    }

    /**
     * @param command the command to pre-fill
     * @return an action that types it into the player's chat box
     */
    public static DialogAction suggestCommand(String command) {
        return new DialogAction(Kind.SUGGEST_COMMAND, command, null);
    }

    /**
     * @param url the address
     * @return an action that opens it in the player's browser
     */
    public static DialogAction openUrl(String url) {
        return new DialogAction(Kind.OPEN_URL, url, null);
    }

    /**
     * @param text what to copy
     * @return an action that puts it on the player's clipboard
     */
    public static DialogAction copyToClipboard(String text) {
        return new DialogAction(Kind.COPY_TO_CLIPBOARD, text, null);
    }

    /**
     * Run server-side code, with the dialog's inputs.
     *
     * @param callback receives the player and everything they filled in
     * @return the action
     */
    public static DialogAction callback(BiConsumer<Player, DialogResponse> callback) {
        return new DialogAction(Kind.CALLBACK, "", callback);
    }

    /**
     * @return an action that does nothing
     */
    public static DialogAction none() {
        return new DialogAction(Kind.NONE, "", null);
    }

    /**
     * @return which kind of action this is
     */
    public Kind kind() {
        return kind;
    }

    /**
     * @return the command, URL or text, never {@code null}
     */
    public String value() {
        return value;
    }

    /**
     * @return the server-side handler, or {@code null}
     */
    public BiConsumer<Player, DialogResponse> callback() {
        return callback;
    }
}
