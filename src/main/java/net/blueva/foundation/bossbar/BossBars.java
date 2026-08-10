package net.blueva.foundation.bossbar;

import net.blueva.foundation.text.TextAdapter;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-version boss bars.
 *
 * <p>{@code org.bukkit.boss.BossBar} arrived in 1.9, so nothing here may reference it at compile
 * time. {@link #create} hands back a {@link BfBossBar} that owns the real bar when the server has
 * one and silently no-ops when it does not, which keeps callers free of version branches.</p>
 *
 * <p>Unlike {@code Messages#bossBar}, which builds a throwaway bar per call, the returned handle
 * is meant to be kept and reused - repeatedly creating bars for the same player stacks them.</p>
 */
public class BossBars {

    private static final Map<String, Method> METHODS = new ConcurrentHashMap<String, Method>();
    private static volatile Class<?> bossBarClass;
    private static volatile Method createBossBar;
    private static volatile boolean resolved;

    protected BossBars() {
    }

    /**
     * @return {@code true} when the running server has the Bukkit boss bar API (1.9+)
     */
    public static boolean isSupported() {
        if (!resolved) {
            resolve();
        }
        return createBossBar != null;
    }

    /**
     * Creates a boss bar. Accepts MiniMessage or legacy text for the title.
     *
     * @param title    bar text
     * @param color    a {@code BarColor} constant name, e.g. {@code YELLOW}
     * @param style    a {@code BarStyle} constant name, e.g. {@code SEGMENTED_6}
     * @param progress fill level, clamped to 0..1
     * @return a handle; never {@code null}, but a no-op one on servers without boss bars
     */
    public static BfBossBar create(String title, String color, String style, double progress) {
        if (!resolved) {
            resolve();
        }
        if (createBossBar == null) {
            return new BfBossBar(null);
        }

        Object barColor = enumConstant("org.bukkit.boss.BarColor", color);
        Object barStyle = enumConstant("org.bukkit.boss.BarStyle", style);
        if (barColor == null || barStyle == null) {
            return new BfBossBar(null);
        }

        try {
            Object handle = createBossBar.invoke(null, TextAdapter.legacySection(title), barColor, barStyle);
            BfBossBar bar = new BfBossBar(handle);
            bar.setProgress(progress);
            return bar;
        } catch (Throwable ignored) {
            return new BfBossBar(null);
        }
    }

    static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object enumConstant(String className, String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Class<?> type = findClass(className);
        if (type == null || !type.isEnum()) {
            return null;
        }
        try {
            return Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class),
                    name.trim().toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Method bossBarMethod(String name, Class<?> parameter) {
        if (bossBarClass == null) {
            return null;
        }
        String key = name + '/' + parameter.getName();
        Method cached = METHODS.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Method method = bossBarClass.getMethod(name, parameter);
            method.setAccessible(true);
            METHODS.put(key, method);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void resolve() {
        synchronized (BossBars.class) {
            if (resolved) {
                return;
            }
            try {
                bossBarClass = Class.forName("org.bukkit.boss.BossBar");
                Class<?> barColorClass = Class.forName("org.bukkit.boss.BarColor");
                Class<?> barStyleClass = Class.forName("org.bukkit.boss.BarStyle");
                createBossBar = Bukkit.class.getMethod("createBossBar",
                        String.class, barColorClass, barStyleClass);
            } catch (Throwable ignored) {
                // 1.8: no boss bar API, handles become no-ops
            }
            resolved = true;
        }
    }
}
