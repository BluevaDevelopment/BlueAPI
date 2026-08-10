package net.blueva.foundation.attributes;

import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Multi-version attribute helpers.
 *
 * <p>{@code org.bukkit.attribute.Attribute} only exists from 1.9 onwards, and its constants were
 * renamed twice: {@code GENERIC_MAX_HEALTH} through 1.21.2, {@code MAX_HEALTH} afterwards, when the
 * type also stopped being an enum and became a registry-backed {@code Keyed}. Everything here is
 * resolved reflectively so callers pass names instead of constants, and 1.8 falls back to the
 * pre-attribute {@code LivingEntity} health methods.</p>
 */
public class Attributes {

    private static volatile Class<?> attributeClass;
    private static volatile Method getAttribute;     // LivingEntity#getAttribute(Attribute) (1.9+)
    private static volatile Method getBaseValue;     // AttributeInstance#getBaseValue()
    private static volatile Method setBaseValue;     // AttributeInstance#setBaseValue(double)
    private static volatile boolean resolved;

    protected Attributes() {
    }

    /**
     * Returns whether the running server knows any of the given attribute names.
     *
     * @param names candidate names, tried in order, e.g. {@code "MAX_HEALTH", "GENERIC_MAX_HEALTH"}
     * @return {@code true} when one of them resolves
     */
    public static boolean isSupported(String... names) {
        return match(names) != null;
    }

    /**
     * Reads an attribute's base value.
     *
     * @param entity the entity (may be {@code null})
     * @param names  candidate attribute names, tried in order
     * @return the base value, or {@code null} when the entity or attribute is unavailable
     */
    public static Double getBaseValue(LivingEntity entity, String... names) {
        Object instance = instance(entity, names);
        if (instance == null || getBaseValue == null) {
            return null;
        }
        try {
            Object value = getBaseValue.invoke(instance);
            return value instanceof Number ? ((Number) value).doubleValue() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Writes an attribute's base value.
     *
     * @param entity the entity (may be {@code null})
     * @param value  the base value to apply
     * @param names  candidate attribute names, tried in order
     * @return {@code true} when the value was applied
     */
    public static boolean setBaseValue(LivingEntity entity, double value, String... names) {
        Object instance = instance(entity, names);
        if (instance == null || setBaseValue == null) {
            return false;
        }
        try {
            setBaseValue.invoke(instance, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Reads an entity's maximum health on any server version, using the max-health attribute when
     * present and the legacy {@code LivingEntity#getMaxHealth()} on 1.8.
     *
     * @param entity the entity (may be {@code null})
     * @return the maximum health, or {@code 0} when unavailable
     */
    @SuppressWarnings("deprecation")
    public static double getMaxHealth(LivingEntity entity) {
        if (entity == null) {
            return 0.0D;
        }
        Double value = getBaseValue(entity, "MAX_HEALTH", "GENERIC_MAX_HEALTH");
        if (value != null) {
            return value;
        }
        try {
            return entity.getMaxHealth();
        } catch (Throwable ignored) {
            return 0.0D;
        }
    }

    /**
     * Writes an entity's maximum health on any server version, using the max-health attribute when
     * present and the legacy {@code LivingEntity#setMaxHealth(double)} on 1.8.
     *
     * @param entity the entity (may be {@code null})
     * @param value  the maximum health to apply
     * @return {@code true} when the value was applied
     */
    @SuppressWarnings("deprecation")
    public static boolean setMaxHealth(LivingEntity entity, double value) {
        if (entity == null) {
            return false;
        }
        if (setBaseValue(entity, value, "MAX_HEALTH", "GENERIC_MAX_HEALTH")) {
            return true;
        }
        try {
            entity.setMaxHealth(value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Resolves an attribute constant by name, returning it as an opaque {@code Object} because the
     * type does not exist on the 1.8 API this library compiles against.
     *
     * @param names candidate names, tried in order
     * @return the attribute constant, or {@code null} when none resolve
     */
    public static Object match(String... names) {
        if (names == null) {
            return null;
        }
        if (!resolved) {
            resolve();
        }
        if (attributeClass == null) {
            return null;
        }
        for (String name : names) {
            Object attribute = matchOne(name);
            if (attribute != null) {
                return attribute;
            }
        }
        return null;
    }

    private static Object matchOne(String name) {
        if (isBlank(name)) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_').replace('.', '_');

        // Covers both eras in one lookup: the constants are public static fields whether Attribute
        // is an enum (1.9-1.21.2) or a registry-backed Keyed interface (1.21.3+).
        try {
            Object attribute = attributeClass.getField(normalized).get(null);
            if (attribute != null) {
                return attribute;
            }
        } catch (Throwable ignored) {
        }

        return matchRegistry(normalized);
    }

    /**
     * Registry fallback for servers where the constant was renamed but the key still resolves,
     * trying both the flat key ({@code max_health}) and the namespaced legacy spelling
     * ({@code generic.max_health}).
     */
    private static Object matchRegistry(String normalized) {
        String key = normalized.toLowerCase(Locale.ROOT);
        String[] candidates;
        int separator = key.indexOf('_');
        if (separator > 0) {
            candidates = new String[]{key, key.substring(0, separator) + "." + key.substring(separator + 1)};
        } else {
            candidates = new String[]{key};
        }

        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
            Object registry = registryClass.getField("ATTRIBUTE").get(null);
            Method get = registryClass.getMethod("get", namespacedKeyClass);
            Method minecraft = namespacedKeyClass.getMethod("minecraft", String.class);

            for (String candidate : candidates) {
                Object namespacedKey = minecraft.invoke(null, candidate);
                if (namespacedKey == null) {
                    continue;
                }
                Object attribute = get.invoke(registry, namespacedKey);
                if (attribute != null) {
                    return attribute;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object instance(LivingEntity entity, String... names) {
        if (entity == null) {
            return null;
        }
        Object attribute = match(names);
        if (attribute == null || getAttribute == null) {
            return null;
        }
        try {
            return getAttribute.invoke(entity, attribute);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void resolve() {
        synchronized (Attributes.class) {
            if (resolved) {
                return;
            }
            try {
                attributeClass = Class.forName("org.bukkit.attribute.Attribute");
                getAttribute = LivingEntity.class.getMethod("getAttribute", attributeClass);

                Class<?> instanceClass = Class.forName("org.bukkit.attribute.AttributeInstance");
                getBaseValue = instanceClass.getMethod("getBaseValue");
                setBaseValue = instanceClass.getMethod("setBaseValue", double.class);
            } catch (Throwable ignored) {
                // 1.8: no attribute API at all, callers fall back to the health methods
            }
            resolved = true;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
