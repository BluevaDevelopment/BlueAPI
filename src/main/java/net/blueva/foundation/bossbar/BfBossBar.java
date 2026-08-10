package net.blueva.foundation.bossbar;

import net.blueva.foundation.text.TextAdapter;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * A single boss bar that can be updated and removed, unlike the fire-and-forget
 * {@code Messages#bossBar}.
 *
 * <p>Backed by {@code org.bukkit.boss.BossBar} (1.9+) through reflection. On 1.8 the Bukkit boss
 * bar API does not exist at all, so every method is a silent no-op and {@link #isSupported()}
 * returns {@code false}; callers keep working without a version check.</p>
 */
public final class BfBossBar {

    private final Object handle;

    BfBossBar(Object handle) {
        this.handle = handle;
    }

    /**
     * @return {@code true} when the running server actually has a boss bar API
     */
    public boolean isSupported() {
        return handle != null;
    }

    /**
     * Replaces the bar's text. Accepts MiniMessage or legacy input.
     *
     * @param title new title
     */
    public void setTitle(String title) {
        invoke("setTitle", String.class, TextAdapter.legacySection(title));
    }

    /**
     * @param progress fill level, clamped to 0..1
     */
    public void setProgress(double progress) {
        double clamped = Math.max(0.0D, Math.min(1.0D, progress));
        invoke("setProgress", double.class, clamped);
    }

    /**
     * @param name a {@code BarColor} constant name, e.g. {@code YELLOW}; unknown names are ignored
     */
    public void setColor(String name) {
        Class<?> type = BossBars.findClass("org.bukkit.boss.BarColor");
        Object color = BossBars.enumConstant("org.bukkit.boss.BarColor", name);
        if (type != null && color != null) {
            invoke("setColor", type, color);
        }
    }

    /**
     * @param name a {@code BarStyle} constant name, e.g. {@code SEGMENTED_6}; unknown names are ignored
     */
    public void setStyle(String name) {
        Class<?> type = BossBars.findClass("org.bukkit.boss.BarStyle");
        Object style = BossBars.enumConstant("org.bukkit.boss.BarStyle", name);
        if (type != null && style != null) {
            invoke("setStyle", type, style);
        }
    }

    /**
     * @param player player to show the bar to
     */
    public void addPlayer(Player player) {
        if (player != null) {
            invoke("addPlayer", Player.class, player);
        }
    }

    /**
     * @param player player to hide the bar from
     */
    public void removePlayer(Player player) {
        if (player != null) {
            invoke("removePlayer", Player.class, player);
        }
    }

    /**
     * Detaches every viewer, which is what actually makes the bar disappear.
     */
    public void removeAll() {
        if (handle == null) {
            return;
        }
        try {
            Method method = handle.getClass().getMethod("removeAll");
            method.setAccessible(true);
            method.invoke(handle);
        } catch (Throwable ignored) {
        }
    }

    private void invoke(String name, Class<?> parameter, Object argument) {
        if (handle == null) {
            return;
        }
        try {
            Method method = BossBars.bossBarMethod(name, parameter);
            if (method != null) {
                method.invoke(handle, argument);
            }
        } catch (Throwable ignored) {
        }
    }
}
