package net.blueva.foundation.hologram;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.version.Version;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Creates a fake NMS {@code TextDisplay} entity handle for hologram lines.
 *
 * <p>Deliberately a close copy of {@code net.blueva.foundation.npc.NpcEntityFactory} rather than
 * a shared/refactored dependency - the NPC system is already confirmed working in production and
 * this module avoids touching it at all. The only real difference from that factory is how the
 * entity type is resolved: {@code EntityType.TEXT_DISPLAY} does not exist as a compile-time
 * constant in the {@code spigot-api:1.8.8} this project compiles against, so it is resolved via
 * {@link EntityType#valueOf(String)} instead of a static field reference - that call is resolved
 * against whatever constants the REAL server's {@code EntityType} class has at runtime, so it
 * works fine on any modern server even though the symbol does not exist at compile time here.</p>
 */
final class HologramEntityFactory {

    private static final EntityType TEXT_DISPLAY_TYPE = resolveTextDisplayType();

    private HologramEntityFactory() {
    }

    /** True if this server exposes {@code TEXT_DISPLAY} (real Minecraft version 1.19.4+). */
    static boolean isSupported() {
        return TEXT_DISPLAY_TYPE != null;
    }

    private static EntityType resolveTextDisplayType() {
        try {
            return EntityType.valueOf("TEXT_DISPLAY");
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Creates an NMS {@code TextDisplay} entity handle at the given location. Not registered in
     * the world - only exists in memory so packets can be built from it.
     *
     * @param location the spawn location
     * @return the NMS entity handle, or null if it could not be created (unsupported version, or
     * every creation strategy failed)
     */
    static Object create(Location location) {
        if (!isSupported() || location == null || location.getWorld() == null) {
            return null;
        }

        Object handle;
        if (Version.isAtLeast(1, 21, 2)) {
            handle = createModern(location);
        } else if (Version.isAtLeast(1, 20, 6)) {
            handle = create1206(location);
        } else {
            handle = null;
        }

        if (handle == null) {
            handle = createViaBukkitSpawn(location);
        }

        return handle;
    }

    private static Object createModern(Location location) {
        try {
            Object nmsType = toNmsEntityType();
            Object level = getServerLevel(location.getWorld());
            if (nmsType == null || level == null) {
                return null;
            }

            Object spawnReason = enumConstant("net.minecraft.world.entity.EntitySpawnReason", "LOAD");
            if (spawnReason == null) {
                spawnReason = enumConstant("net.minecraft.world.entity.MobSpawnType", "LOAD");
            }

            Object handle = createDirect(nmsType, level, spawnReason);
            if (handle == null) {
                Object compound = newCompoundTag();
                putString(compound, "id", "text_display");
                Object processor = staticField("net.minecraft.world.entity.EntityProcessor", "NOP");

                if (Version.isAtLeast(1, 21, 11)) {
                    handle = invokeStatic("net.minecraft.world.entity.EntityType", "loadEntityRecursive",
                            nmsType, compound, level, spawnReason, processor).orElse(null);
                } else {
                    handle = invokeStatic("net.minecraft.world.entity.EntityTypes", "a",
                            nmsType, compound, level, spawnReason, processor).orElse(null);
                }
            }
            if (handle != null) {
                setPositionAndRotation(handle, location);
            }
            return handle;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createDirect(Object nmsType, Object level, Object spawnReason) {
        if (nmsType == null || level == null || spawnReason == null) {
            return null;
        }
        try {
            Method create = findMethodByParams(nmsType.getClass(), "create",
                    new Class<?>[]{level.getClass(), spawnReason.getClass()});
            if (create == null) {
                return null;
            }
            create.setAccessible(true);
            return create.invoke(nmsType, level, spawnReason);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object create1206(Location location) {
        try {
            Object nmsType = toNmsEntityType();
            Object level = getServerLevel(location.getWorld());
            if (nmsType == null || level == null) {
                return null;
            }

            Object spawnReason = enumConstant("net.minecraft.world.entity.EntitySpawnReason", "LOAD");
            if (spawnReason == null) {
                spawnReason = enumConstant("net.minecraft.world.entity.MobSpawnType", "LOAD");
            }

            Object handle = createDirect(nmsType, level, spawnReason);
            if (handle == null) {
                Object compound = newCompoundTag();
                putString(compound, "id", "text_display");
                handle = invokeStatic("net.minecraft.world.entity.EntityTypes", "a",
                        compound, level, spawnReason, identityFunction()).orElse(null);
            }
            if (handle != null) {
                setPositionAndRotation(handle, location);
            }
            return handle;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createViaBukkitSpawn(Location location) {
        if (TEXT_DISPLAY_TYPE == null) {
            return null;
        }
        try {
            Entity entity = location.getWorld().spawnEntity(location, TEXT_DISPLAY_TYPE);
            if (entity == null) {
                return null;
            }
            Object handle = Reflection.getHandle(entity);
            entity.remove();
            return handle;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object toNmsEntityType() {
        try {
            Class<?> craftEntityType = Reflection.findClass("org.bukkit.craftbukkit.entity.CraftEntityType");
            if (craftEntityType != null) {
                Method method = craftEntityType.getDeclaredMethod("bukkitToMinecraft", EntityType.class);
                method.setAccessible(true);
                return method.invoke(null, TEXT_DISPLAY_TYPE);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object newCompoundTag() {
        try {
            Class<?> clazz = Reflection.findClass("net.minecraft.nbt.CompoundTag");
            if (clazz == null) {
                clazz = Reflection.findClass("net.minecraft.nbt.NBTTagCompound");
            }
            if (clazz == null) {
                return null;
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void putString(Object compound, String key, String value) {
        try {
            Method method = findMethodByName(compound.getClass(), "putString", "a");
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            method.invoke(compound, key, value);
        } catch (Throwable ignored) {
        }
    }

    private static Object getServerLevel(org.bukkit.World world) {
        try {
            Method method = world.getClass().getMethod("getHandle");
            method.setAccessible(true);
            return method.invoke(world);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setPositionAndRotation(Object handle, Location location) {
        try {
            Method setPos = findMethodByParams(handle.getClass(), "setPos",
                    new Class<?>[]{double.class, double.class, double.class});
            if (setPos != null) {
                setPos.setAccessible(true);
                setPos.invoke(handle, location.getX(), location.getY(), location.getZ());
            }
            Method setRot = findMethodByParams(handle.getClass(), "setRot",
                    new Class<?>[]{float.class, float.class});
            if (setRot != null) {
                setRot.setAccessible(true);
                setRot.invoke(handle, location.getYaw(), location.getPitch());
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object identityFunction() {
        try {
            Class<?> functionClass = Class.forName("java.util.function.Function");
            Method identity = functionClass.getMethod("identity");
            return identity.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static java.util.Optional<Object> invokeStatic(String className, String methodName, Object... args) {
        try {
            Class<?> clazz = Reflection.findClass(className);
            if (clazz == null) {
                return java.util.Optional.empty();
            }
            Method method = findMethodByName(clazz, methodName);
            if (method == null) {
                return java.util.Optional.empty();
            }
            method.setAccessible(true);
            return java.util.Optional.ofNullable(method.invoke(null, args));
        } catch (Throwable e) {
            return java.util.Optional.empty();
        }
    }

    private static Object enumConstant(String className, String constant) {
        try {
            Class<?> clazz = Reflection.findClass(className);
            if (clazz == null) {
                return null;
            }
            return Enum.valueOf(clazz.asSubclass(Enum.class), constant);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object staticField(String className, String fieldName) {
        try {
            Class<?> clazz = Reflection.findClass(className);
            if (clazz == null) {
                return null;
            }
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethodByName(Class<?> clazz, String... names) {
        for (String name : names) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Method findMethodByParams(Class<?> clazz, String name, Class<?>[] params) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                Class<?>[] methodParams = method.getParameterTypes();
                if (methodParams.length != params.length) {
                    continue;
                }
                boolean matches = true;
                for (int i = 0; i < params.length; i++) {
                    if (!methodParams[i].isAssignableFrom(params[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return method;
                }
            }
        }
        return null;
    }
}
