package net.blueva.foundation.menus.dialogs;

/**
 * A button on a dialog.
 */
public final class DialogButton {

    private final String label;
    private final String tooltip;
    private final int width;
    private final DialogAction action;

    private DialogButton(String label, String tooltip, int width, DialogAction action) {
        this.label = label == null ? "" : label;
        this.tooltip = tooltip;
        this.width = width > 0 ? width : 150;
        this.action = action == null ? DialogAction.none() : action;
    }

    /**
     * @param label what the button says, in MiniMessage
     * @return a button that only closes the dialog
     */
    public static DialogButton of(String label) {
        return new DialogButton(label, null, 0, null);
    }

    /**
     * @param label  what the button says, in MiniMessage
     * @param action what pressing it does
     * @return the button
     */
    public static DialogButton of(String label, DialogAction action) {
        return new DialogButton(label, null, 0, action);
    }

    /**
     * @param label   what the button says, in MiniMessage
     * @param tooltip the hover text, in MiniMessage, or {@code null}
     * @param width   the button width in pixels
     * @param action  what pressing it does
     * @return the button
     */
    public static DialogButton of(String label, String tooltip, int width, DialogAction action) {
        return new DialogButton(label, tooltip, width, action);
    }

    /**
     * @return the label, in MiniMessage
     */
    public String label() {
        return label;
    }

    /**
     * @return the hover text, or {@code null}
     */
    public String tooltip() {
        return tooltip;
    }

    /**
     * @return the width in pixels
     */
    public int width() {
        return width;
    }

    /**
     * @return what pressing it does
     */
    public DialogAction action() {
        return action;
    }
}
