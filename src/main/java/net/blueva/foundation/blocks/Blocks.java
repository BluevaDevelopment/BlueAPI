package net.blueva.foundation.blocks;

import net.blueva.foundation.reflection.Reflection;
import org.bukkit.block.Block;

import java.lang.reflect.Method;

/**
 * Block-related helpers that smooth over Bukkit API differences between
 * server versions, so plugins can call them without version guards.
 */
public class Blocks {

    private static volatile Method bukkitIsPassable; // Block#isPassable (1.13+)
    private static volatile boolean passableResolved;

    /**
     * Returns whether a block can be passed through (has no collision) on any
     * server version.
     * <p>Uses {@code Block#isPassable()} when present (1.13+) and falls back to
     * {@code !type.isSolid()} on older servers, which is a close approximation
     * for movement and ground detection.</p>
     *
     * @param block the block (may be {@code null}, treated as passable)
     * @return {@code true} if the block can be passed through
     */
    public static boolean isPassable(Block block) {
        if (block == null) {
            return true;
        }
        if (!passableResolved) {
            resolvePassable();
        }
        if (bukkitIsPassable != null) {
            try {
                Object value = bukkitIsPassable.invoke(block);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            } catch (Throwable ignored) {
                // fall through to the legacy approximation
            }
        }
        return !block.getType().isSolid();
    }

    private static void resolvePassable() {
        synchronized (Blocks.class) {
            if (passableResolved) {
                return;
            }
            bukkitIsPassable = Reflection.method(Block.class, "isPassable");
            passableResolved = true;
        }
    }

    /**
     * Serialises a block's state to a string, on any server version.
     *
     * <p>1.13+ returns {@code BlockData#getAsString()}, the canonical form the matching restore
     * path expects. Older servers have no BlockData at all, so this falls back to the namespaced
     * material name, which is the most a legacy snapshot can carry.</p>
     *
     * @param block the block (may be {@code null})
     * @return the serialised state, or {@code null} when there is no block
     */
    public static String serialize(Block block) {
        if (block == null) {
            return null;
        }
        try {
            Object data = Block.class.getMethod("getBlockData").invoke(block);
            if (data != null) {
                Object value = data.getClass().getMethod("getAsString").invoke(data);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Throwable ignored) {
            // pre-1.13
        }
        return "minecraft:" + block.getType().name().toLowerCase(java.util.Locale.ROOT);
    }
}
