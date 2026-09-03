package net.blueva.foundation.menus.bedrock;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A title, a body and a vertical list of buttons - the Bedrock counterpart of
 * a chest menu.
 */
public final class SimpleForm extends Form {

    /** One button, with the handler that runs when it is pressed. */
    public static final class Button {
        private final String text;
        private final FormImage image;
        private final Consumer<Player> action;

        Button(String text, FormImage image, Consumer<Player> action) {
            this.text = text == null ? "" : text;
            this.image = image;
            this.action = action;
        }

        /**
         * @return the button's label
         */
        public String text() {
            return text;
        }

        /**
         * @return the icon beside the label, or {@code null}
         */
        public FormImage image() {
            return image;
        }

        /**
         * @return this button as a JSON object
         */
        public String toJson() {
            StringBuilder out = new StringBuilder();
            out.append("{\"text\":").append(Json.quote(text));
            if (image != null) {
                out.append(",\"image\":").append(image.toJson());
            }
            out.append('}');
            return out.toString();
        }
    }

    private final String content;
    private final List<Button> buttons;
    private final BiConsumer<Player, Integer> handler;

    private SimpleForm(String title, String content, List<Button> buttons,
                       BiConsumer<Player, Integer> handler, Consumer<Player> closedHandler) {
        super(title, closedHandler);
        this.content = content == null ? "" : content;
        this.buttons = Collections.unmodifiableList(new ArrayList<Button>(buttons));
        this.handler = handler;
    }

    /**
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the body text above the buttons
     */
    public String content() {
        return content;
    }

    /**
     * @return every button, in the order the client shows them
     */
    public List<Button> buttons() {
        return buttons;
    }

    @Override
    public FormType type() {
        return FormType.SIMPLE;
    }

    @Override
    public String toJson() {
        StringBuilder out = new StringBuilder();
        out.append("{\"type\":").append(Json.quote(type().jsonName()));
        out.append(",\"title\":").append(Json.quote(title()));
        out.append(",\"content\":").append(Json.quote(content));
        out.append(",\"buttons\":[");
        for (int i = 0; i < buttons.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(buttons.get(i).toJson());
        }
        out.append("]}");
        return out.toString();
    }

    @Override
    public void handle(Player player, String raw) {
        if (isClosed(raw)) {
            fireClosed(player);
            return;
        }
        int index;
        try {
            index = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            fireClosed(player);
            return;
        }
        if (index < 0 || index >= buttons.size()) {
            fireClosed(player);
            return;
        }
        Button button = buttons.get(index);
        if (button.action != null) {
            button.action.accept(player);
        }
        if (handler != null) {
            handler.accept(player, Integer.valueOf(index));
        }
    }

    /** Builds a {@link SimpleForm}. */
    public static final class Builder {
        private String title = "";
        private String content = "";
        private final List<Button> buttons = new ArrayList<Button>();
        private BiConsumer<Player, Integer> handler;
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
         * Append body lines, joined with newlines.
         *
         * @param lines the lines
         * @return this builder
         */
        public Builder content(List<String> lines) {
            if (lines == null || lines.isEmpty()) {
                return this;
            }
            StringBuilder joined = new StringBuilder();
            for (String line : lines) {
                if (joined.length() > 0) {
                    joined.append('\n');
                }
                joined.append(line == null ? "" : line);
            }
            this.content = joined.toString();
            return this;
        }

        /**
         * @param text the label
         * @return this builder
         */
        public Builder button(String text) {
            return button(text, null, null);
        }

        /**
         * @param text   the label
         * @param action what to run when it is pressed
         * @return this builder
         */
        public Builder button(String text, Consumer<Player> action) {
            return button(text, null, action);
        }

        /**
         * @param text   the label
         * @param image  the icon, or {@code null}
         * @param action what to run when it is pressed, or {@code null}
         * @return this builder
         */
        public Builder button(String text, FormImage image, Consumer<Player> action) {
            buttons.add(new Button(text, image, action));
            return this;
        }

        /**
         * Run something for every press, on top of the per-button handlers.
         *
         * @param handler receives the player and the pressed button's index
         * @return this builder
         */
        public Builder onPress(BiConsumer<Player, Integer> handler) {
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
        public SimpleForm build() {
            return new SimpleForm(title, content, buttons, handler, closedHandler);
        }
    }
}
