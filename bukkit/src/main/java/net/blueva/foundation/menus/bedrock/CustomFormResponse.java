package net.blueva.foundation.menus.bedrock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a player filled into a {@link CustomForm}.
 *
 * <p>Values can be read by the component's key or by its index. Reading a
 * component as the wrong type, or reading one the player never touched,
 * returns the component's default rather than throwing: a form response is
 * client input, and client input is not a place to be strict.</p>
 */
public final class CustomFormResponse {

    private final List<FormComponent> components;
    private final Map<String, Object> byKey;
    private final List<Object> byIndex;

    CustomFormResponse(List<FormComponent> components, List<Object> values) {
        this.components = components;
        this.byIndex = values;
        Map<String, Object> keyed = new LinkedHashMap<String, Object>();
        for (int i = 0; i < components.size(); i++) {
            FormComponent component = components.get(i);
            if (component.key() != null && !component.key().isEmpty()) {
                keyed.put(component.key(), i < values.size() ? values.get(i) : component.fallbackValue());
            }
        }
        this.byKey = Collections.unmodifiableMap(keyed);
    }

    /**
     * @return every value, in component order, with {@code null} for labels
     */
    public List<Object> values() {
        return Collections.unmodifiableList(byIndex);
    }

    /**
     * @return every keyed value
     */
    public Map<String, Object> asMap() {
        return byKey;
    }

    /**
     * @param key a component key
     * @return the raw value, or {@code null}
     */
    public Object get(String key) {
        return byKey.get(key);
    }

    /**
     * @param index a component index
     * @return the raw value, or {@code null}
     */
    public Object get(int index) {
        return index >= 0 && index < byIndex.size() ? byIndex.get(index) : null;
    }

    /**
     * @param key a text box's key
     * @return what was typed, or an empty string
     */
    public String getString(String key) {
        Object value = byKey.get(key);
        return value == null ? "" : value.toString();
    }

    /**
     * @param key a toggle's key
     * @return its position, or {@code false}
     */
    public boolean getBoolean(String key) {
        Object value = byKey.get(key);
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return value != null && "true".equalsIgnoreCase(value.toString());
    }

    /**
     * @param key a slider's key
     * @return where the handle ended up, or {@code 0}
     */
    public float getFloat(String key) {
        Object value = byKey.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return value == null ? 0f : Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /**
     * @param key a dropdown's or stepped slider's key
     * @return the selected index, or {@code 0}
     */
    public int getInt(String key) {
        Object value = byKey.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? 0 : (int) Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Read a dropdown or stepped slider as the label the player picked rather
     * than its index.
     *
     * @param key the component's key
     * @return the selected option's text, or an empty string
     */
    public String getOption(String key) {
        int index = getInt(key);
        for (FormComponent component : components) {
            if (!key.equals(component.key())) {
                continue;
            }
            if (component instanceof FormComponent.Dropdown) {
                List<String> options = ((FormComponent.Dropdown) component).options();
                return index >= 0 && index < options.size() ? options.get(index) : "";
            }
            if (component instanceof FormComponent.StepSlider) {
                List<String> steps = ((FormComponent.StepSlider) component).steps();
                return index >= 0 && index < steps.size() ? steps.get(index) : "";
            }
        }
        return "";
    }
}
