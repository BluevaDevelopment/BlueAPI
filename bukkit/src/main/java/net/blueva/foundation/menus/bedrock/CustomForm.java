package net.blueva.foundation.menus.bedrock;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A form built out of inputs: labels, text boxes, toggles, sliders and
 * drop-downs. The Bedrock equivalent of an anvil prompt or a settings screen.
 */
public final class CustomForm extends Form {

    private final FormImage icon;
    private final List<FormComponent> components;
    private final BiConsumer<Player, CustomFormResponse> handler;

    private CustomForm(String title, FormImage icon, List<FormComponent> components,
                       BiConsumer<Player, CustomFormResponse> handler, Consumer<Player> closedHandler) {
        super(title, closedHandler);
        this.icon = icon;
        this.components = Collections.unmodifiableList(new ArrayList<FormComponent>(components));
        this.handler = handler;
    }

    /**
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the icon in the title bar, or {@code null}
     */
    public FormImage icon() {
        return icon;
    }

    /**
     * @return every component, in the order the client shows them
     */
    public List<FormComponent> components() {
        return components;
    }

    @Override
    public FormType type() {
        return FormType.CUSTOM;
    }

    @Override
    public String toJson() {
        StringBuilder out = new StringBuilder();
        out.append("{\"type\":").append(Json.quote(type().jsonName()));
        out.append(",\"title\":").append(Json.quote(title()));
        if (icon != null) {
            out.append(",\"icon\":").append(icon.toJson());
        }
        out.append(",\"content\":[");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(components.get(i).toJson());
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
        Object parsed;
        try {
            parsed = Json.parse(raw);
        } catch (IllegalArgumentException e) {
            fireClosed(player);
            return;
        }
        if (!(parsed instanceof List)) {
            fireClosed(player);
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object> raws = (List<Object>) parsed;
        if (handler != null) {
            handler.accept(player, new CustomFormResponse(components, align(raws)));
        }
    }

    /**
     * Line the client's values up with our components.
     *
     * <p>The client is supposed to send one value per component, {@code null}
     * included for labels. Not every client build does: some omit the label
     * slots. Rather than mis-assign every field after the first label, detect
     * which shape arrived and map accordingly.</p>
     *
     * @param raws the values the client sent
     * @return one value per component, defaults filled in for anything missing
     */
    private List<Object> align(List<Object> raws) {
        List<Object> aligned = new ArrayList<Object>(components.size());
        if (raws.size() == components.size()) {
            for (int i = 0; i < components.size(); i++) {
                FormComponent component = components.get(i);
                aligned.add(component.readable() ? raws.get(i) : null);
            }
            return aligned;
        }
        int cursor = 0;
        for (FormComponent component : components) {
            if (!component.readable()) {
                aligned.add(null);
                continue;
            }
            if (cursor < raws.size()) {
                aligned.add(raws.get(cursor++));
            } else {
                aligned.add(component.fallbackValue());
            }
        }
        return aligned;
    }

    /** Builds a {@link CustomForm}. */
    public static final class Builder {
        private String title = "";
        private FormImage icon;
        private final List<FormComponent> components = new ArrayList<FormComponent>();
        private BiConsumer<Player, CustomFormResponse> handler;
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
         * @param icon the icon beside the title, or {@code null}
         * @return this builder
         */
        public Builder icon(FormImage icon) {
            this.icon = icon;
            return this;
        }

        /**
         * @param component a ready-made component
         * @return this builder
         */
        public Builder component(FormComponent component) {
            if (component != null) {
                components.add(component);
            }
            return this;
        }

        /**
         * @param text static text to show
         * @return this builder
         */
        public Builder label(String text) {
            return component(FormComponent.label("label" + components.size(), text));
        }

        /**
         * @param key         the key to read the answer back with
         * @param text        the label
         * @param placeholder the hint shown while empty
         * @param defaultText the starting text
         * @return this builder
         */
        public Builder input(String key, String text, String placeholder, String defaultText) {
            return component(FormComponent.input(key, text, placeholder, defaultText));
        }

        /**
         * @param key  the key to read the answer back with
         * @param text the label
         * @return this builder
         */
        public Builder input(String key, String text) {
            return input(key, text, "", "");
        }

        /**
         * @param key          the key to read the answer back with
         * @param text         the label
         * @param defaultValue the starting position
         * @return this builder
         */
        public Builder toggle(String key, String text, boolean defaultValue) {
            return component(FormComponent.toggle(key, text, defaultValue));
        }

        /**
         * @param key          the key to read the answer back with
         * @param text         the label
         * @param min          the lowest value
         * @param max          the highest value
         * @param step         the distance between values
         * @param defaultValue the starting value
         * @return this builder
         */
        public Builder slider(String key, String text, float min, float max, float step, float defaultValue) {
            return component(FormComponent.slider(key, text, min, max, step, defaultValue));
        }

        /**
         * @param key         the key to read the answer back with
         * @param text        the label
         * @param steps       the stops, in order
         * @param defaultStep the index of the starting stop
         * @return this builder
         */
        public Builder stepSlider(String key, String text, Collection<String> steps, int defaultStep) {
            return component(FormComponent.stepSlider(key, text, steps, defaultStep));
        }

        /**
         * @param key           the key to read the answer back with
         * @param text          the label
         * @param options       the options, in order
         * @param defaultOption the index of the starting option
         * @return this builder
         */
        public Builder dropdown(String key, String text, Collection<String> options, int defaultOption) {
            return component(FormComponent.dropdown(key, text, options, defaultOption));
        }

        /**
         * @param key     the key to read the answer back with
         * @param text    the label
         * @param options the options, in order
         * @return this builder
         */
        public Builder dropdown(String key, String text, String... options) {
            return dropdown(key, text, Arrays.asList(options), 0);
        }

        /**
         * @param handler receives the player and everything they filled in
         * @return this builder
         */
        public Builder onSubmit(BiConsumer<Player, CustomFormResponse> handler) {
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
        public CustomForm build() {
            return new CustomForm(title, icon, components, handler, closedHandler);
        }
    }
}
