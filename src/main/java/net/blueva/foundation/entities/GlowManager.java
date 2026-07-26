package net.blueva.foundation.entities;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Controls client-side entity glowing independently for each viewer.
 */
public interface GlowManager extends AutoCloseable {

    /**
     * Makes an entity glow white for one viewer.
     *
     * @param entity entity to highlight
     * @param viewer player who should see the highlight
     * @return {@code true} when the update was sent
     */
    boolean setGlowing(Entity entity, Player viewer);

    /**
     * Makes an entity glow with a color for one viewer.
     *
     * @param entity entity to highlight
     * @param viewer player who should see the highlight
     * @param color glow color, or {@code null} for the entity's existing team color
     * @return {@code true} when the update was sent
     */
    boolean setGlowing(Entity entity, Player viewer, ChatColor color);

    /**
     * Removes the highlight previously applied by this manager.
     *
     * @param entity highlighted entity
     * @param viewer player who should stop seeing the highlight
     * @return {@code true} when an existing highlight was removed
     */
    boolean unsetGlowing(Entity entity, Player viewer);

    /**
     * Removes every highlight and packet listener owned by this manager.
     */
    @Override
    void close();
}
