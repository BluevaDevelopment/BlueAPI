package net.blueva.foundation.menus.dialogs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a player filled into a dialog's inputs, as delivered by a
 * {@link DialogAction#callback} button.
 *
 * <p>Every value arrives as a string - that is what the custom-click payload
 * carries - so the typed getters parse rather than cast, and fall back to a
 * default instead of throwing. A dialog response is client input.</p>
 */
public final class DialogResponse {

    private final Map<String, String> values;

    DialogResponse(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(
                values == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<String, String>(values));
    }

    /**
     * @return every input keyed by its name
     */
    public Map<String, String> values() {
        return values;
    }

    /**
     * @param key an input key
     * @return whether the dialog carried a value for it
     */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /**
     * @param key an input key
     * @return what was typed, or an empty string
     */
    public String getString(String key) {
        return getString(key, "");
    }

    /**
     * @param key      an input key
     * @param fallback what to return when the key is missing
     * @return the value
     */
    public String getString(String key, String fallback) {
        String value = values.get(key);
        return value == null ? fallback : value;
    }

    /**
     * @param key an input key
     * @return the toggle's position, or {@code false}
     */
    public boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(getString(key, "false"));
    }

    /**
     * @param key an input key
     * @return the number, or {@code 0}
     */
    public double getDouble(String key) {
        try {
            return Double.parseDouble(getString(key, "0"));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    /**
     * @param key an input key
     * @return the number, or {@code 0}
     */
    public int getInt(String key) {
        return (int) getDouble(key);
    }

    @Override
    public String toString() {
        return "DialogResponse" + values;
    }
}
