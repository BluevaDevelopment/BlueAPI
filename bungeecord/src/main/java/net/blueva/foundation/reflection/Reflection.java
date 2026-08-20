package net.blueva.foundation.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Generic reflection helpers. No BungeeCord-specific lookups here. */
public class Reflection {

    protected Reflection() {
    }

    public static boolean classExists(String className) {
        return findClass(className) != null;
    }

    public static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Method method(Class<?> type, String name, Class<?>... parameters) {
        if (type == null) {
            return null;
        }
        try {
            Method method = type.getMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Field field(Class<?> type, String name) {
        if (type == null) {
            return null;
        }
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
