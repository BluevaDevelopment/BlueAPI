package net.blueva.foundation.menus.bedrock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * One row of a {@link CustomForm}.
 *
 * <p>Every component carries a {@code key} on top of what the Bedrock protocol
 * sends. The client answers a custom form with a bare positional array, so the
 * key never leaves the server - it exists so callers can read a response by
 * name instead of counting rows, which is the difference between a form that
 * survives an edit and one that silently reads the wrong field.</p>
 */
public abstract class FormComponent {

    private final String key;
    private final String text;

    FormComponent(String key, String text) {
        this.key = key;
        this.text = text == null ? "" : text;
    }

    /**
     * @return the server-side name of this component, never sent to the client
     */
    public String key() {
        return key;
    }

    /**
     * @return the label shown next to the component
     */
    public String text() {
        return text;
    }

    /**
     * @return the value of this component's {@code "type"} JSON property
     */
    public abstract String jsonType();

    /**
     * @return whether the client sends a value back for this component
     */
    public boolean readable() {
        return true;
    }

    /**
     * @return this component as a JSON object
     */
    public String toJson() {
        StringBuilder out = new StringBuilder();
        out.append("{\"type\":").append(Json.quote(jsonType()));
        out.append(",\"text\":").append(Json.quote(text));
        appendExtra(out);
        out.append('}');
        return out.toString();
    }

    /**
     * Append the component's own JSON properties, each prefixed with a comma.
     *
     * @param out the buffer being built
     */
    protected void appendExtra(StringBuilder out) {
    }

    /**
     * @return the value this component reports when the player never touched it
     */
    public abstract Object fallbackValue();

    /** Static text. The client sends nothing back for it. */
    public static final class Label extends FormComponent {
        Label(String key, String text) {
            super(key, text);
        }

        @Override
        public String jsonType() {
            return "label";
        }

        @Override
        public boolean readable() {
            return false;
        }

        @Override
        public Object fallbackValue() {
            return null;
        }
    }

    /** A single-line text box. */
    public static final class Input extends FormComponent {
        private final String placeholder;
        private final String defaultText;

        Input(String key, String text, String placeholder, String defaultText) {
            super(key, text);
            this.placeholder = placeholder == null ? "" : placeholder;
            this.defaultText = defaultText == null ? "" : defaultText;
        }

        /**
         * @return the hint shown while the box is empty
         */
        public String placeholder() {
            return placeholder;
        }

        /**
         * @return the text the box starts with
         */
        public String defaultText() {
            return defaultText;
        }

        @Override
        public String jsonType() {
            return "input";
        }

        @Override
        protected void appendExtra(StringBuilder out) {
            out.append(",\"placeholder\":").append(Json.quote(placeholder));
            out.append(",\"default\":").append(Json.quote(defaultText));
        }

        @Override
        public Object fallbackValue() {
            return defaultText;
        }
    }

    /** An on/off switch. */
    public static final class Toggle extends FormComponent {
        private final boolean defaultValue;

        Toggle(String key, String text, boolean defaultValue) {
            super(key, text);
            this.defaultValue = defaultValue;
        }

        /**
         * @return the position the switch starts in
         */
        public boolean defaultValue() {
            return defaultValue;
        }

        @Override
        public String jsonType() {
            return "toggle";
        }

        @Override
        protected void appendExtra(StringBuilder out) {
            out.append(",\"default\":").append(defaultValue);
        }

        @Override
        public Object fallbackValue() {
            return Boolean.valueOf(defaultValue);
        }
    }

    /** A continuous slider between two bounds. */
    public static final class Slider extends FormComponent {
        private final float min;
        private final float max;
        private final float step;
        private final float defaultValue;

        Slider(String key, String text, float min, float max, float step, float defaultValue) {
            super(key, text);
            this.min = min;
            this.max = Math.max(min, max);
            this.step = step <= 0 ? 1 : step;
            this.defaultValue = Math.min(this.max, Math.max(min, defaultValue));
        }

        /**
         * @return the lowest selectable value
         */
        public float min() {
            return min;
        }

        /**
         * @return the highest selectable value
         */
        public float max() {
            return max;
        }

        /**
         * @return the distance between two selectable values
         */
        public float step() {
            return step;
        }

        /**
         * @return where the handle starts
         */
        public float defaultValue() {
            return defaultValue;
        }

        @Override
        public String jsonType() {
            return "slider";
        }

        @Override
        protected void appendExtra(StringBuilder out) {
            out.append(",\"min\":").append(Json.number(min));
            out.append(",\"max\":").append(Json.number(max));
            out.append(",\"step\":").append(Json.number(step));
            out.append(",\"default\":").append(Json.number(defaultValue));
        }

        @Override
        public Object fallbackValue() {
            return Float.valueOf(defaultValue);
        }
    }

    /** A slider that snaps to named stops. */
    public static final class StepSlider extends FormComponent {
        private final List<String> steps;
        private final int defaultStep;

        StepSlider(String key, String text, Collection<String> steps, int defaultStep) {
            super(key, text);
            this.steps = Collections.unmodifiableList(new ArrayList<String>(steps));
            this.defaultStep = clampIndex(defaultStep, this.steps.size());
        }

        /**
         * @return the labels of every stop, in order
         */
        public List<String> steps() {
            return steps;
        }

        /**
         * @return the index of the stop the slider starts on
         */
        public int defaultStep() {
            return defaultStep;
        }

        @Override
        public String jsonType() {
            return "step_slider";
        }

        @Override
        protected void appendExtra(StringBuilder out) {
            appendStringArray(out, "steps", steps);
            out.append(",\"default\":").append(defaultStep);
        }

        @Override
        public Object fallbackValue() {
            return Integer.valueOf(defaultStep);
        }
    }

    /** A drop-down list. */
    public static final class Dropdown extends FormComponent {
        private final List<String> options;
        private final int defaultOption;

        Dropdown(String key, String text, Collection<String> options, int defaultOption) {
            super(key, text);
            this.options = Collections.unmodifiableList(new ArrayList<String>(options));
            this.defaultOption = clampIndex(defaultOption, this.options.size());
        }

        /**
         * @return every option, in order
         */
        public List<String> options() {
            return options;
        }

        /**
         * @return the index of the option that starts selected
         */
        public int defaultOption() {
            return defaultOption;
        }

        @Override
        public String jsonType() {
            return "dropdown";
        }

        @Override
        protected void appendExtra(StringBuilder out) {
            appendStringArray(out, "options", options);
            out.append(",\"default\":").append(defaultOption);
        }

        @Override
        public Object fallbackValue() {
            return Integer.valueOf(defaultOption);
        }
    }

    /**
     * @param key   the server-side name
     * @param text  the text to show
     * @return static text
     */
    public static Label label(String key, String text) {
        return new Label(key, text);
    }

    /**
     * @param key         the server-side name
     * @param text        the label
     * @param placeholder the hint shown while empty
     * @param defaultText the starting text
     * @return a text box
     */
    public static Input input(String key, String text, String placeholder, String defaultText) {
        return new Input(key, text, placeholder, defaultText);
    }

    /**
     * @param key          the server-side name
     * @param text         the label
     * @param defaultValue the starting position
     * @return an on/off switch
     */
    public static Toggle toggle(String key, String text, boolean defaultValue) {
        return new Toggle(key, text, defaultValue);
    }

    /**
     * @param key          the server-side name
     * @param text         the label
     * @param min          the lowest value
     * @param max          the highest value
     * @param step         the distance between values
     * @param defaultValue the starting value
     * @return a slider
     */
    public static Slider slider(String key, String text, float min, float max, float step, float defaultValue) {
        return new Slider(key, text, min, max, step, defaultValue);
    }

    /**
     * @param key         the server-side name
     * @param text        the label
     * @param steps       the stops, in order
     * @param defaultStep the index of the starting stop
     * @return a stepped slider
     */
    public static StepSlider stepSlider(String key, String text, Collection<String> steps, int defaultStep) {
        return new StepSlider(key, text, steps, defaultStep);
    }

    /**
     * @param key           the server-side name
     * @param text          the label
     * @param options       the options, in order
     * @param defaultOption the index of the starting option
     * @return a drop-down
     */
    public static Dropdown dropdown(String key, String text, Collection<String> options, int defaultOption) {
        return new Dropdown(key, text, options, defaultOption);
    }

    /**
     * @param key     the server-side name
     * @param text    the label
     * @param options the options, in order
     * @return a drop-down starting on its first option
     */
    public static Dropdown dropdown(String key, String text, String... options) {
        return new Dropdown(key, text, Arrays.asList(options), 0);
    }

    private static void appendStringArray(StringBuilder out, String property, List<String> values) {
        out.append(',').append(Json.quote(property)).append(":[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(Json.quote(values.get(i)));
        }
        out.append(']');
    }

    private static int clampIndex(int index, int size) {
        if (size == 0 || index < 0) {
            return 0;
        }
        return Math.min(index, size - 1);
    }
}
