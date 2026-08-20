package net.blueva.foundation.bossbar;

import net.blueva.foundation.text.TextAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-version boss bars.
 *
 * <p>{@code org.bukkit.boss.BossBar} arrived in 1.9, so nothing here may reference it at compile
 * time. {@code create} hands back a {@link BfBossBar} that owns the real bar when the server has
 * one, which keeps callers free of version branches.</p>
 *
 * <p>Older servers can still show a bar through {@link #create(Plugin, String, String, String,
 * double)}, which falls back to the pre-1.9 wither trick (see {@link LegacyBossBar}). The
 * plugin-less overload cannot do that, because the fallback needs a repeating task, so it
 * degrades to a no-op there instead.</p>
 *
 * <p>Unlike {@code Messages#bossBar}, which builds a throwaway bar per call, the returned handle
 * is meant to be kept and reused, since repeatedly creating bars for the same player stacks them.</p>
 */
public class BossBars {

    private static final Map<String, Method> METHODS = new ConcurrentHashMap<String, Method>();
    private static volatile Class<?> bossBarClass;
    private static volatile Method createBossBar;
    private static volatile boolean resolved;

    protected BossBars() {
    }

    /**
     * Creates a boss bar, falling back to the wither trick on servers with no boss bar API.
     *
     * <p>Behaves exactly like {@link #create(String, String, String, double)} on 1.9+. The
     * difference is on older servers: passing a plugin lets them show a real bar rather than
     * nothing, because keeping the fake entity in front of the viewer needs a repeating task.</p>
     *
     * @param plugin   the plugin that owns the tracking task
     * @param title    bar text
     * @param color    a {@code BarColor} constant name; ignored on 1.8
     * @param style    a {@code BarStyle} constant name; ignored on 1.8
     * @param progress fill level, clamped to 0..1
     * @return a handle; never {@code null}
     */
    public static BfBossBar create(Plugin plugin, String title, String color, String style, double progress) {
        if (!resolved) {
            resolve();
        }
        if (createBossBar == null) {
            if (plugin != null && LegacyBossBar.isAvailable()) {
                return new BfBossBar(new LegacyBossBar(plugin, title, progress));
            }
            return new BfBossBar((Object) null);
        }
        return create(title, color, style, progress);
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
            return new BfBossBar((Object) null);
        }

        Object barColor = enumConstant("org.bukkit.boss.BarColor", color);
        Object barStyle = enumConstant("org.bukkit.boss.BarStyle", style);
        if (barColor == null || barStyle == null) {
            return new BfBossBar((Object) null);
        }

        try {
            Object handle = createBossBar.invoke(null, TextAdapter.legacySection(title), barColor, barStyle);
            BfBossBar bar = new BfBossBar(handle);
            bar.setProgress(progress);
            return bar;
        } catch (Throwable ignored) {
            return new BfBossBar((Object) null);
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
