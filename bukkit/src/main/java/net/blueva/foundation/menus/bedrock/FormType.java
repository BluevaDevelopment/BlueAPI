package net.blueva.foundation.menus.bedrock;

/**
 * The three form kinds the Bedrock client understands.
 *
 * <p>The ordinals are wire values: the first byte of a {@code floodgate:form}
 * plugin message is this enum's ordinal, and Geyser reads it back with
 * {@code FormType.fromOrdinal}. The order must therefore match Cumulus'
 * {@code org.geysermc.cumulus.form.util.FormType} exactly.</p>
 */
public enum FormType {

    /** A title, a body and a vertical list of buttons. */
    SIMPLE("form"),

    /** A title, a body and exactly two buttons. */
    MODAL("modal"),

    /** A title and a stack of inputs (labels, toggles, sliders, dropdowns). */
    CUSTOM("custom_form");

    private final String jsonName;

    FormType(String jsonName) {
        this.jsonName = jsonName;
    }

    /**
     * @return the value of the form's {@code "type"} JSON property
     */
    public String jsonName() {
        return jsonName;
    }

    /**
     * @return the byte the plugin message protocol uses for this type
     */
    public byte wireId() {
        return (byte) ordinal();
    }
}
