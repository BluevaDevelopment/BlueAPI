package net.blueva.foundation.menus.dialogs;

/**
 * Thrown when a dialog is shown on a server that cannot draw one.
 *
 * <p>Dialogs arrived in Minecraft 1.21.6 and are exposed through two different
 * server APIs, neither of which exists before that. There is no shim that
 * could make an older client render one, so rather than fail quietly,
 * {@link Dialogs#show} throws this - and
 * {@link Dialogs#trySend} exists for callers who would rather branch than
 * catch.</p>
 */
public class DialogUnsupportedException extends UnsupportedOperationException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what was missing
     */
    public DialogUnsupportedException(String message) {
        super(message);
    }
}
