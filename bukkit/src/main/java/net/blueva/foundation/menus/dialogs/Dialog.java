package net.blueva.foundation.menus.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A server-drawn dialog screen.
 *
 * <pre>{@code
 * Dialog confirm = Dialog.confirmation("<red>Delete this house?")
 *         .body("<gray>This cannot be undone.")
 *         .yes(DialogButton.of("<red>Delete", DialogAction.callback((p, r) -> delete(p))))
 *         .no(DialogButton.of("<gray>Cancel"))
 *         .build();
 * BlueFoundation.Dialogs.show(plugin, player, confirm);
 * }</pre>
 *
 * <p>Dialogs are a Java Edition feature, added in 1.21.6. A Bedrock player
 * connected through Geyser does see one, because Geyser translates the packet
 * into a native form - but that is Geyser's doing, not this API's, and it
 * drops what a form cannot render. For a menu that is meant to look right on
 * both editions, use {@link net.blueva.foundation.menus.Menus} instead.</p>
 */
public final class Dialog {

    /** The three dialog shapes this API builds. */
    public enum Type {
        /** A message with one dismiss button. */
        NOTICE,
        /** A question with two buttons. */
        CONFIRMATION,
        /** A grid of buttons, with an optional exit button. */
        MULTI_ACTION
    }

    private final Type type;
    private final String title;
    private final String externalTitle;
    private final List<String> body;
    private final List<DialogInput> inputs;
    private final List<DialogButton> buttons;
    private final DialogButton exitButton;
    private final int columns;
    private final boolean canCloseWithEscape;
    private final boolean pause;
    private final DialogAfterAction afterAction;

    private Dialog(Builder builder) {
        this.type = builder.type;
        this.title = builder.title == null ? "" : builder.title;
        this.externalTitle = builder.externalTitle;
        this.body = Collections.unmodifiableList(new ArrayList<String>(builder.body));
        this.inputs = Collections.unmodifiableList(new ArrayList<DialogInput>(builder.inputs));
        this.buttons = Collections.unmodifiableList(new ArrayList<DialogButton>(builder.buttons));
        this.exitButton = builder.exitButton;
        this.columns = builder.columns > 0 ? builder.columns : 2;
        this.canCloseWithEscape = builder.canCloseWithEscape;
        this.pause = builder.pause;
        this.afterAction = builder.afterAction;
    }

    /**
     * @param title the dialog title, in MiniMessage
     * @return a builder for a one-button message
     */
    public static Builder notice(String title) {
        return new Builder(Type.NOTICE).title(title);
    }

    /**
     * @param title the dialog title, in MiniMessage
     * @return a builder for a two-button question
     */
    public static Builder confirmation(String title) {
        return new Builder(Type.CONFIRMATION).title(title);
    }

    /**
     * @param title the dialog title, in MiniMessage
     * @return a builder for a grid of buttons
     */
    public static Builder multiAction(String title) {
        return new Builder(Type.MULTI_ACTION).title(title);
    }

    /**
     * @return which shape this dialog is
     */
    public Type type() {
        return type;
    }

    /**
     * @return the title, in MiniMessage
     */
    public String title() {
        return title;
    }

    /**
     * @return the title used when this dialog is listed inside another, or {@code null}
     */
    public String externalTitle() {
        return externalTitle;
    }

    /**
     * @return the body lines, in MiniMessage
     */
    public List<String> body() {
        return body;
    }

    /**
     * @return the fields the player fills in
     */
    public List<DialogInput> inputs() {
        return inputs;
    }

    /**
     * @return the buttons, in order
     */
    public List<DialogButton> buttons() {
        return buttons;
    }

    /**
     * @return the multi-action exit button, or {@code null}
     */
    public DialogButton exitButton() {
        return exitButton;
    }

    /**
     * @return how many buttons per row in a multi-action dialog
     */
    public int columns() {
        return columns;
    }

    /**
     * @return whether escape dismisses the dialog
     */
    public boolean canCloseWithEscape() {
        return canCloseWithEscape;
    }

    /**
     * @return whether the game pauses while this dialog is open
     */
    public boolean pause() {
        return pause;
    }

    /**
     * @return what happens after a button is pressed
     */
    public DialogAfterAction afterAction() {
        return afterAction;
    }

    /** Builds a {@link Dialog}. */
    public static final class Builder {
        private final Type type;
        private String title;
        private String externalTitle;
        private final List<String> body = new ArrayList<String>();
        private final List<DialogInput> inputs = new ArrayList<DialogInput>();
        private final List<DialogButton> buttons = new ArrayList<DialogButton>();
        private DialogButton exitButton;
        private int columns = 2;
        private boolean canCloseWithEscape = true;
        private boolean pause;
        private DialogAfterAction afterAction = DialogAfterAction.CLOSE;

        Builder(Type type) {
            this.type = type;
        }

        /**
         * @param title the dialog title, in MiniMessage
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * @param externalTitle the title used when listed inside another dialog
         * @return this builder
         */
        public Builder externalTitle(String externalTitle) {
            this.externalTitle = externalTitle;
            return this;
        }

        /**
         * @param lines body lines, in MiniMessage
         * @return this builder
         */
        public Builder body(String... lines) {
            for (String line : lines) {
                if (line != null) {
                    body.add(line);
                }
            }
            return this;
        }

        /**
         * @param lines body lines, in MiniMessage
         * @return this builder
         */
        public Builder body(Collection<String> lines) {
            if (lines != null) {
                for (String line : lines) {
                    if (line != null) {
                        body.add(line);
                    }
                }
            }
            return this;
        }

        /**
         * @param input a field to add
         * @return this builder
         */
        public Builder input(DialogInput input) {
            if (input != null) {
                inputs.add(input);
            }
            return this;
        }

        /**
         * @param button a button to add
         * @return this builder
         */
        public Builder button(DialogButton button) {
            if (button != null) {
                buttons.add(button);
            }
            return this;
        }

        /**
         * The confirm button of a confirmation dialog. The same slot as the
         * first {@link #button(DialogButton)}.
         *
         * @param button the button
         * @return this builder
         */
        public Builder yes(DialogButton button) {
            return button(button);
        }

        /**
         * The cancel button of a confirmation dialog.
         *
         * @param button the button
         * @return this builder
         */
        public Builder no(DialogButton button) {
            return button(button);
        }

        /**
         * @param exitButton the multi-action exit button
         * @return this builder
         */
        public Builder exitButton(DialogButton exitButton) {
            this.exitButton = exitButton;
            return this;
        }

        /**
         * @param columns how many buttons per row
         * @return this builder
         */
        public Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        /**
         * @param canCloseWithEscape whether escape dismisses the dialog
         * @return this builder
         */
        public Builder canCloseWithEscape(boolean canCloseWithEscape) {
            this.canCloseWithEscape = canCloseWithEscape;
            return this;
        }

        /**
         * @param pause whether the game pauses while it is open
         * @return this builder
         */
        public Builder pause(boolean pause) {
            this.pause = pause;
            return this;
        }

        /**
         * @param afterAction what happens after a button is pressed
         * @return this builder
         */
        public Builder afterAction(DialogAfterAction afterAction) {
            this.afterAction = afterAction == null ? DialogAfterAction.CLOSE : afterAction;
            return this;
        }

        /**
         * @return the finished dialog
         */
        public Dialog build() {
            return new Dialog(this);
        }
    }
}
