package net.blueva.foundation.hologram;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Lifecycle manager for the hologram module.
 *
 * <p>No listeners of its own - unlike the NPC module, distance-based visibility is entirely
 * driven by the consuming plugin ({@code showTo}/{@code hideFrom}), same as it already drives its
 * own NPC visibility.</p>
 */
final class HologramManager {

    private final Plugin plugin;

    HologramManager(Plugin plugin) {
        this.plugin = plugin;
    }

    Plugin getPlugin() {
        return plugin;
    }

    void enable() {
        // Nothing to register yet.
    }

    void disable() {
        List<HologramImpl> copy = new ArrayList<>(HologramRegistry.all());
        for (HologramImpl hologram : copy) {
            hologram.destroy();
        }
        HologramRegistry.clear();
    }
}
