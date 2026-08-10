package net.blueva.foundation.entities;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Multi-version entity type helpers. */
public class Entities {

    /**
     * Entity types renamed across Bukkit versions. Each group lists every known
     * name for the same entity type, most recent name first. Matching is
     * bidirectional: any name in a group resolves on any server version.
     */
    private static final String[][] RENAMED_GROUPS = {
            // Renamed in 1.20.5 / 1.21
            {"TNT", "PRIMED_TNT"},
            {"ITEM", "DROPPED_ITEM"},
            {"CHEST_MINECART", "MINECART_CHEST"},
            {"COMMAND_BLOCK_MINECART", "MINECART_COMMAND"},
            {"FURNACE_MINECART", "MINECART_FURNACE"},
            {"HOPPER_MINECART", "MINECART_HOPPER"},
            {"SPAWNER_MINECART", "MINECART_MOB_SPAWNER"},
            {"TNT_MINECART", "MINECART_TNT"},
            {"LEASH_KNOT", "LEASH_HITCH"},
            {"EYE_OF_ENDER", "ENDER_SIGNAL"},
            {"FIREWORK_ROCKET", "FIREWORK"},
            {"EXPERIENCE_BOTTLE", "THROWN_EXP_BOTTLE"},
            // Renamed in 1.20.2
            {"POTION", "SPLASH_POTION"},
            // Renamed in 1.16
            {"ZOMBIFIED_PIGLIN", "PIG_ZOMBIE"},
            // Renamed in 1.14
            {"MOOSHROOM", "MUSHROOM_COW"},
    };

    private static final Map<String, String[]> ALIASES = new HashMap<>();

    static {
        for (String[] group : RENAMED_GROUPS) {
            for (String name : group) {
                ALIASES.put(name, group);
            }
        }
    }

    protected Entities() {
    }

    public static EntityType match(String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            EntityType type = matchOne(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    public static EntityType require(String... names) {
        EntityType type = match(names);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported entity type names: " + Arrays.toString(names));
        }
        return type;
    }

    public static boolean isSupported(String... names) {
        return match(names) != null;
    }

    public static Entity spawn(Location location, String... names) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        EntityType type = match(names);
        if (type == null) {
            return null;
        }
        return location.getWorld().spawnEntity(location, type);
    }

    public static <T extends Entity> T spawn(Location location, Class<T> entityClass, String... names) {
        if (entityClass == null) {
            return null;
        }
        Entity entity = spawn(location, names);
        if (!entityClass.isInstance(entity)) {
            return null;
        }
        return entityClass.cast(entity);
    }

    /**
     * Creates a lifecycle-aware manager for per-viewer colored entity glow.
     *
     * @param plugin plugin that owns the manager and its listeners
     * @return a new glow manager
     */
    public static GlowManager createGlowManager(Plugin plugin) {
        return new EntityGlowManager(plugin);
    }

    private static EntityType matchOne(String name) {
        if (isBlank(name)) {
            return null;
        }

        String normalized = normalize(name);
        for (String candidate : candidates(normalized)) {
            try {
                return EntityType.valueOf(candidate);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Set<String> candidates(String normalized) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);

        String[] aliases = ALIASES.get(normalized);
        if (aliases != null) {
            candidates.addAll(Arrays.asList(aliases));
        }
        return candidates;
    }

    private static String normalize(String name) {
        String normalized = name.trim();
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return normalized.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('.', '_');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Sets a boolean flag on an entity when the running server has that setter.
     *
     * <p>Several entity flags arrived after 1.8 ({@code setSilent} 1.9, {@code setAI} 1.10,
     * {@code setCollidable} 1.9, {@code setInvulnerable} 1.9, {@code setPersistent} 1.9), so this
     * resolves the setter reflectively and quietly does nothing where it is missing.</p>
     *
     * @param entity the entity (may be {@code null})
     * @param setter the setter name, e.g. {@code setSilent}
     * @param value  the value to apply
     * @return {@code true} when the flag was applied
     */
    public static boolean setFlag(Entity entity, String setter, boolean value) {
        if (entity == null || setter == null) {
            return false;
        }
        try {
            Method method = entity.getClass().getMethod(setter, boolean.class);
            method.setAccessible(true);
            method.invoke(entity, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** @see #setFlag(Entity, String, boolean) */
    public static boolean setInvulnerable(Entity entity, boolean value) {
        return setFlag(entity, "setInvulnerable", value);
    }

    /** @see #setFlag(Entity, String, boolean) */
    public static boolean setSilent(Entity entity, boolean value) {
        return setFlag(entity, "setSilent", value);
    }

    /** @see #setFlag(Entity, String, boolean) */
    public static boolean setAI(Entity entity, boolean value) {
        return setFlag(entity, "setAI", value);
    }

    /** @see #setFlag(Entity, String, boolean) */
    public static boolean setCollidable(Entity entity, boolean value) {
        return setFlag(entity, "setCollidable", value);
    }

    /** @see #setFlag(Entity, String, boolean) */
    public static boolean setPersistent(Entity entity, boolean value) {
        return setFlag(entity, "setPersistent", value);
    }

    /** @see #setFlag(Entity, String, boolean) */
    public static boolean setGlowing(Entity entity, boolean value) {
        return setFlag(entity, "setGlowing", value);
    }

    /**
     * Whether a boolean entity flag is set, for getters that only exist on newer servers.
     *
     * @param entity   the entity (may be {@code null})
     * @param getter   the getter name, e.g. {@code isGlowing}
     * @param fallback returned when the getter is missing
     * @return the flag value, or {@code fallback}
     */
    public static boolean getFlag(Entity entity, String getter, boolean fallback) {
        if (entity == null || getter == null) {
            return fallback;
        }
        try {
            Method method = entity.getClass().getMethod(getter);
            method.setAccessible(true);
            Object value = method.invoke(entity);
            return value instanceof Boolean ? (Boolean) value : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    /**
     * Mounts a passenger, using {@code addPassenger} on 1.11+ and {@code setPassenger} on older
     * servers, which only ever supported a single passenger.
     *
     * @param vehicle   the carrying entity (may be {@code null})
     * @param passenger the entity to mount (may be {@code null})
     * @return {@code true} when the passenger was mounted
     */
    public static boolean addPassenger(Entity vehicle, Entity passenger) {
        if (vehicle == null || passenger == null) {
            return false;
        }
        try {
            Method add = vehicle.getClass().getMethod("addPassenger", Entity.class);
            add.setAccessible(true);
            Object result = add.invoke(vehicle, passenger);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable ignored) {
        }
        try {
            Method set = vehicle.getClass().getMethod("setPassenger", Entity.class);
            set.setAccessible(true);
            Object result = set.invoke(vehicle, passenger);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
