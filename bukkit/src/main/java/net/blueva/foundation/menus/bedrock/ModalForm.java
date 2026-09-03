package net.blueva.foundation.menus.bedrock;

import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A yes/no form: a title, a body and exactly two buttons.
 *
 * <p>The client answers with {@code true} for the first button and
 * {@code false} for the second.</p>
 */
public final class ModalForm extends Form {

    private final String content;
    private final String button1;
    private final String button2;
    private final Consumer<Player> firstAction;
    private final Consumer<Player> secondAction;
    private final BiConsumer<Player, Boolean> handler;

    private ModalForm(String title, String content, String button1, String button2,
                      Consumer<Player> firstAction, Consumer<Player> secondAction,
                      BiConsumer<Player, Boolean> handler, Consumer<Player> closedHandler) {
        super(title, closedHandler);
        this.content = content == null ? "" : content;
        this.button1 = button1 == null ? "" : button1;
        this.button2 = button2 == null ? "" : button2;
        this.firstAction = firstAction;
        this.secondAction = secondAction;
        this.handler = handler;
    }

    /**
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the body text
     */
    public String content() {
        return content;
    }

    /**
     * @return the first button's label
     */
    public String button1() {
        return button1;
    }

    /**
     * @return the second button's label
     */
    public String button2() {
        return button2;
    }

    @Override
    public FormType type() {
        return FormType.MODAL;
    }

    @Override
    public String toJson() {
        return "{\"type\":" + Json.quote(type().jsonName())
                + ",\"title\":" + Json.quote(title())
                + ",\"content\":" + Json.quote(content)
                + ",\"button1\":" + Json.quote(button1)
                + ",\"button2\":" + Json.quote(button2)
                + "}";
    }

    @Override
    public void handle(Player player, String raw) {
        if (isClosed(raw)) {
            fireClosed(player);
            return;
        }
        String answer = raw.trim();
        boolean first;
        if ("true".equalsIgnoreCase(answer)) {
            first = true;
        } else if ("false".equalsIgnoreCase(answer)) {
            first = false;
        } else {
            fireClosed(player);
            return;
        }
        Consumer<Player> action = first ? firstAction : secondAction;
        if (action != null) {
            action.accept(player);
        }
        if (handler != null) {
            handler.accept(player, Boolean.valueOf(first));
        }
    }

    /** Builds a {@link ModalForm}. */
    public static final class Builder {
        private String title = "";
        private String content = "";
        private String button1 = "";
        private String button2 = "";
        private Consumer<Player> firstAction;
        private Consumer<Player> secondAction;
        private BiConsumer<Player, Boolean> handler;
        private Consumer<Player> closedHandler;

        Builder() {
        }

        /**
         * @param title the title bar text
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * @param content the body text
         * @return this builder
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * @param text   the first button's label
         * @param action what to run when it is pressed, or {@code null}
         * @return this builder
         */
        public Builder first(String text, Consumer<Player> action) {
            this.button1 = text;
            this.firstAction = action;
            return this;
        }

        /**
         * @param text   the second button's label
         * @param action what to run when it is pressed, or {@code null}
         * @return this builder
         */
        public Builder second(String text, Consumer<Player> action) {
            this.button2 = text;
            this.secondAction = action;
            return this;
        }

        /**
         * Run something for every answer, on top of the per-button handlers.
         *
         * @param handler receives the player and {@code true} for the first button
         * @return this builder
         */
        public Builder onAnswer(BiConsumer<Player, Boolean> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * @param closedHandler what to run when the player dismisses the form
         * @return this builder
         */
        public Builder onClosed(Consumer<Player> closedHandler) {
            this.closedHandler = closedHandler;
            return this;
        }

        /**
         * @return the finished form
         */
        public ModalForm build() {
            return new ModalForm(title, content, button1, button2,
                    firstAction, secondAction, handler, closedHandler);
        }
    }
}
