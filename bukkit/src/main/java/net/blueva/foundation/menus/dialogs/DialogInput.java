package net.blueva.foundation.menus.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A field the player fills in before pressing a dialog button.
 *
 * <p>The four kinds here are the four the protocol has, which is also the four
 * Geyser knows how to turn into Bedrock form components. Anything else would
 * render on Java and vanish on Bedrock.</p>
 */
public final class DialogInput {

    /** The kinds of field a dialog can contain. */
    public enum Kind {
        /** A text box. */
        TEXT,
        /** A checkbox. */
        BOOLEAN,
        /** A slider between two numbers. */
        NUMBER_RANGE,
        /** A cycling picker over a fixed list. */
        SINGLE_OPTION
    }

    /** One choice in a {@link Kind#SINGLE_OPTION} input. */
    public static final class Option {
        private final String id;
        private final String label;
        private final boolean initial;

        /**
         * @param id      the value reported back in the response
         * @param label   what the player sees
         * @param initial whether this option starts selected
         */
        public Option(String id, String label, boolean initial) {
            this.id = id == null ? "" : id;
            this.label = label == null ? this.id : label;
            this.initial = initial;
        }

        /**
         * @return the value reported back
         */
        public String id() {
            return id;
        }

        /**
         * @return the label shown
         */
        public String label() {
            return label;
        }

        /**
         * @return whether it starts selected
         */
        public boolean initial() {
            return initial;
        }
    }

    private final Kind kind;
    private final String key;
    private final String label;
    private final int width;
    private final String initialText;
    private final int maxLength;
    private final Integer maxLines;
    private final Integer height;
    private final boolean initialBoolean;
    private final String onTrue;
    private final String onFalse;
    private final float start;
    private final float end;
    private final Float initialNumber;
    private final Float step;
    private final String labelFormat;
    private final List<Option> options;

    private DialogInput(Kind kind, String key, String label, int width, String initialText, int maxLength,
                        Integer maxLines, Integer height, boolean initialBoolean, String onTrue, String onFalse,
                        float start, float end, Float initialNumber, Float step, String labelFormat,
                        List<Option> options) {
        this.kind = kind;
        this.key = key == null ? "" : key;
        this.label = label == null ? "" : label;
        this.width = width > 0 ? width : 200;
        this.initialText = initialText == null ? "" : initialText;
        this.maxLength = maxLength > 0 ? maxLength : 32;
        this.maxLines = maxLines;
        this.height = height;
        this.initialBoolean = initialBoolean;
        this.onTrue = onTrue == null ? "true" : onTrue;
        this.onFalse = onFalse == null ? "false" : onFalse;
        this.start = start;
        this.end = end;
        this.initialNumber = initialNumber;
        this.step = step;
        this.labelFormat = labelFormat == null ? "options.generic_value" : labelFormat;
        this.options = Collections.unmodifiableList(
                options == null ? new ArrayList<Option>() : new ArrayList<Option>(options));
    }

    /**
     * @param key     the name this field reports under
     * @param label   what the player sees
     * @param initial the starting text
     * @return a single-line text box
     */
    public static DialogInput text(String key, String label, String initial) {
        return new DialogInput(Kind.TEXT, key, label, 200, initial, 32, null, null,
                false, null, null, 0, 0, null, null, null, null);
    }

    /**
     * @param key       the name this field reports under
     * @param label     what the player sees
     * @param initial   the starting text
     * @param maxLength how much the player may type
     * @param maxLines  how many lines tall, or {@code null} for one line
     * @return a text box
     */
    public static DialogInput text(String key, String label, String initial, int maxLength, Integer maxLines) {
        return new DialogInput(Kind.TEXT, key, label, 200, initial, maxLength, maxLines, null,
                false, null, null, 0, 0, null, null, null, null);
    }

    /**
     * @param key     the name this field reports under
     * @param label   what the player sees
     * @param initial whether it starts ticked
     * @return a checkbox
     */
    public static DialogInput bool(String key, String label, boolean initial) {
        return new DialogInput(Kind.BOOLEAN, key, label, 200, null, 0, null, null,
                initial, null, null, 0, 0, null, null, null, null);
    }

    /**
     * @param key     the name this field reports under
     * @param label   what the player sees
     * @param start   the low end
     * @param end     the high end
     * @param initial where the handle starts, or {@code null} for the middle
     * @param step    the increment, or {@code null} for continuous
     * @return a slider
     */
    public static DialogInput numberRange(String key, String label, float start, float end,
                                          Float initial, Float step) {
        return new DialogInput(Kind.NUMBER_RANGE, key, label, 200, null, 0, null, null,
                false, null, null, start, end, initial, step, null, null);
    }

    /**
     * @param key     the name this field reports under
     * @param label   what the player sees
     * @param options the choices
     * @return a cycling picker
     */
    public static DialogInput singleOption(String key, String label, Collection<Option> options) {
        return new DialogInput(Kind.SINGLE_OPTION, key, label, 200, null, 0, null, null,
                false, null, null, 0, 0, null, null, null, new ArrayList<Option>(options));
    }

    /**
     * @param key     the name this field reports under
     * @param label   what the player sees
     * @param options the choices; the first starts selected
     * @return a cycling picker
     */
    public static DialogInput singleOption(String key, String label, String... options) {
        List<Option> list = new ArrayList<Option>();
        for (int i = 0; i < options.length; i++) {
            list.add(new Option(options[i], options[i], i == 0));
        }
        return singleOption(key, label, list);
    }

    /**
     * @return which kind of field this is
     */
    public Kind kind() {
        return kind;
    }

    /**
     * @return the name this field reports under
     */
    public String key() {
        return key;
    }

    /**
     * @return the label shown beside it
     */
    public String label() {
        return label;
    }

    /**
     * @return the field's width in pixels
     */
    public int width() {
        return width;
    }

    /**
     * @return the starting text
     */
    public String initialText() {
        return initialText;
    }

    /**
     * @return how much the player may type
     */
    public int maxLength() {
        return maxLength;
    }

    /**
     * @return how many lines tall, or {@code null}
     */
    public Integer maxLines() {
        return maxLines;
    }

    /**
     * @return the box height in pixels, or {@code null}
     */
    public Integer height() {
        return height;
    }

    /**
     * @return whether the checkbox starts ticked
     */
    public boolean initialBoolean() {
        return initialBoolean;
    }

    /**
     * @return what a ticked box reports
     */
    public String onTrue() {
        return onTrue;
    }

    /**
     * @return what an unticked box reports
     */
    public String onFalse() {
        return onFalse;
    }

    /**
     * @return the slider's low end
     */
    public float start() {
        return start;
    }

    /**
     * @return the slider's high end
     */
    public float end() {
        return end;
    }

    /**
     * @return where the handle starts, or {@code null}
     */
    public Float initialNumber() {
        return initialNumber;
    }

    /**
     * @return the slider increment, or {@code null}
     */
    public Float step() {
        return step;
    }

    /**
     * @return the translation key used to render the slider's value
     */
    public String labelFormat() {
        return labelFormat;
    }

    /**
     * @return the choices of a single-option input
     */
    public List<Option> options() {
        return options;
    }

    /**
     * @param ids option ids, the first starting selected
     * @return the options
     */
    public static List<Option> options(String... ids) {
        List<Option> list = new ArrayList<Option>();
        for (String id : Arrays.asList(ids)) {
            list.add(new Option(id, id, list.isEmpty()));
        }
        return list;
    }
}
