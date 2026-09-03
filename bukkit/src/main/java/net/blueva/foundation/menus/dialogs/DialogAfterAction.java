package net.blueva.foundation.menus.dialogs;

/**
 * What the client does with a dialog once a button has been pressed.
 */
public enum DialogAfterAction {

    /** Close the dialog. The default, and what almost every dialog wants. */
    CLOSE,

    /** Leave it open, so the player can press another button. */
    NONE,

    /**
     * Leave it open and blocked until the server sends something else. Use it
     * when the button starts work the player must not press twice.
     */
    WAIT_FOR_RESPONSE
}
