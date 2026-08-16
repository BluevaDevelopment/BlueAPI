package net.blueva.foundation.hologram;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Public entry point for the BlueFoundation hologram module.
 *
 * <p>Own protocol implementation (one fake {@code TextDisplay} entity per line) instead of
 * depending on a third-party hologram plugin - built to replace a FancyHolograms integration that
 * had an unreproducible/undiagnosable line-overlap bug in its internal renderer (no sources
 * available for that part of the plugin). See {@link Hologram} for the design rationale.</p>
 */
public class Holograms {

    private static HologramManager manager;

    protected Holograms() {
    }

    /**
     * Initializes the hologram module.
     *
     * @param plugin the plugin using the module
     */
    public static synchronized void init(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (manager != null) {
            return;
        }
        manager = new HologramManager(plugin);
        manager.enable();
    }

    /**
     * Shuts down the hologram module, destroying every active hologram.
     */
    public static synchronized void close() {
        if (manager == null) {
            return;
        }
        manager.disable();
        manager = null;
    }

    /**
     * True if this server version supports fake {@code TextDisplay} entities (Minecraft 1.19.4+).
     * A {@link #create} call on an unsupported version returns a hologram with zero lines ever
     * spawned - harmless, but nothing will show up.
     */
    public static boolean isSupported() {
        return HologramEntityFactory.isSupported();
    }

    /**
     * Creates a new hologram anchored at the given location - {@code lines.get(0)} is the
     * topmost line, subsequent lines stack downward.
     *
     * @param anchor the location of the first (topmost) line
     * @param lines  the initial lines, top to bottom
     * @return the created hologram
     */
    public static Hologram create(Location anchor, List<String> lines) {
        ensureInitialized();
        HologramImpl hologram = new HologramImpl(anchor, lines);
        HologramRegistry.register(hologram);
        return hologram;
    }

    private static void ensureInitialized() {
        if (manager == null) {
            throw new IllegalStateException("BlueFoundation.Holograms is not initialized. Call BlueFoundation.Holograms.init(plugin) first.");
        }
    }
}
