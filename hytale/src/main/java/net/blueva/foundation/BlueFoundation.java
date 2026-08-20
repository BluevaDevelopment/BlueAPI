package net.blueva.foundation;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import net.blueva.foundation.music.InstrumentSounds;
import net.blueva.foundation.music.MusicManager;

import java.util.Collection;

/**
 * Master entry point for BlueFoundation on Hytale.
 *
 * <p>This class intentionally stays small. Real implementations live in
 * dedicated packages, while these nested aliases keep the public API reachable
 * from a single namespace: {@code BlueFoundation.*}. Only the facades that map
 * onto something real in Hytale's plugin API exist here - see the project
 * README for the full per-platform feature matrix and what is intentionally
 * not attempted yet (items, blocks, entities proper, worlds - Hytale's
 * ECS-based world model has no equivalent to port from Bukkit).</p>
 */
public final class BlueFoundation {

    private static final String UNKNOWN_VERSION = "unknown";

    private BlueFoundation() {
    }

    public static String version() {
        // Baked in at build time, so it survives being shaded into a consumer jar.
        if (!BuildInfo.VERSION.isEmpty() && !BuildInfo.VERSION.startsWith("${")) {
            return BuildInfo.VERSION;
        }

        Package foundationPackage = BlueFoundation.class.getPackage();
        if (foundationPackage != null && foundationPackage.getImplementationVersion() != null) {
            return foundationPackage.getImplementationVersion();
        }

        return UNKNOWN_VERSION;
    }

    public static class Dependencies extends net.blueva.foundation.dependencies.Dependencies {
        private Dependencies() {
        }

        /**
         * @param plugin source of the data directory and classloader to install
         *               downloaded jars into. Hytale's own logger type isn't a
         *               {@code java.util.logging.Logger}, so download progress
         *               is not logged through it - pass a {@link Loader}
         *               built directly if you need that.
         */
        public static Loader loader(PluginBase plugin) {
            return new Loader(plugin.getDataDirectory(), plugin.getClass().getClassLoader(), null);
        }

        public static void load(PluginBase plugin, Collection<? extends RuntimeDependency> dependencies) {
            loader(plugin).loadAll(dependencies);
        }

        public static void load(PluginBase plugin, RuntimeDependency dependency) {
            loader(plugin).load(dependency);
        }
    }

    public static class Reflection extends net.blueva.foundation.reflection.Reflection {
        private Reflection() {
        }
    }

    public static class Version extends net.blueva.foundation.version.Version {
        private Version() {
        }
    }

    public static class Configs extends net.blueva.foundation.config.Configs {
        private Configs() {
        }

        /**
         * Loads a plugin's own config file with BlueFoundation's config engine,
         * standalone from Hytale's Codec/{@code BuilderCodec} system.
         */
        public static net.blueva.foundation.config.ConfigFile yaml(PluginBase plugin, String resourceName) {
            return load(plugin, resourceName, net.blueva.foundation.config.ConfigFormat.YAML);
        }

        public static net.blueva.foundation.config.ConfigFile toml(PluginBase plugin, String resourceName) {
            return load(plugin, resourceName, net.blueva.foundation.config.ConfigFormat.TOML);
        }

        public static net.blueva.foundation.config.ConfigFile load(PluginBase plugin, String resourceName, net.blueva.foundation.config.ConfigFormat format) {
            return load(plugin, resourceName, format, net.blueva.foundation.config.ConfigUpdatePolicy.MERGE_DEFAULTS);
        }

        public static net.blueva.foundation.config.ConfigFile load(PluginBase plugin, String resourceName, net.blueva.foundation.config.ConfigFormat format, net.blueva.foundation.config.ConfigUpdatePolicy updatePolicy) {
            if (plugin == null) {
                throw new IllegalArgumentException("plugin cannot be null");
            }
            return load(plugin.getDataDirectory(), plugin.getClass().getClassLoader(), resourceName, format, updatePolicy);
        }
    }

    public static class Text extends net.blueva.foundation.text.Text {
        private Text() {
        }
    }

    public static class Messages extends net.blueva.foundation.messages.Messages {
        private Messages() {
        }
    }

    public static class Commands extends net.blueva.foundation.commands.Commands {
        private Commands() {
        }
    }

    public static class Scheduler extends net.blueva.foundation.scheduler.Scheduler {
        private Scheduler() {
        }
    }

    /**
     * Experimental - see {@link net.blueva.foundation.scoreboard.Scoreboards}.
     * Its UI library is compileOnly, not shaded - downloaded into the plugin's
     * own classloader at runtime on first use instead.
     */
    public static class Scoreboards extends net.blueva.foundation.scoreboard.Scoreboards {
        private Scoreboards() {
        }
    }

    public static class Sounds extends net.blueva.foundation.sounds.Sounds {
        private Sounds() {
        }
    }

    /**
     * Injects files directly into the calling plugin's own running jar - the
     * one mechanism Hytale asset registration is known to actually work
     * through in practice (proven in production by BlueArcade's item sync).
     * Changes only take effect after a server restart. Generic on purpose:
     * BlueFoundation itself has no opinion on what gets written where - see
     * {@link net.blueva.foundation.music.InstrumentSounds} for one
     * concrete use (instrument sounds).
     */
    public static class Assets {
        private Assets() {
        }

        /** @return {@code true} if {@code entryPath} already exists inside {@code plugin}'s jar. */
        public static boolean hasEntry(PluginBase plugin, String entryPath) {
            return net.blueva.foundation.assets.JarAssetInjector.hasEntry(plugin.getClass(), entryPath);
        }

        /** Writes (creating or overwriting) a single entry inside {@code plugin}'s own jar. */
        public static void writeEntry(PluginBase plugin, String entryPath, byte[] content) {
            net.blueva.foundation.assets.JarAssetInjector.writeEntry(plugin.getClass(), entryPath, content);
        }

        /**
         * Copies every file under {@code sourceDirectory} into {@code plugin}'s
         * own jar, under {@code entryPrefix}, preserving relative structure.
         */
        public static void writeTree(PluginBase plugin, java.nio.file.Path sourceDirectory, String entryPrefix) {
            net.blueva.foundation.assets.JarAssetInjector.writeTree(plugin.getClass(), sourceDirectory, entryPrefix);
        }
    }

    /**
     * MIDI melody playback - see {@link net.blueva.foundation.music.MusicManager}.
     */
    public static class Music {
        private Music() {
        }

        /**
         * @param plugin              source of the data directory (MIDI files are read from
         *                            {@code <dataDirectory>/sounds/<melodyId>.midi}) and logger.
         * @param soundEventIdResolver resolves an instrument/octave/note-length to the id of the
         *                            sound event that plays it - see {@link net.blueva.foundation.music.MusicManager.SoundEventIdResolver}.
         */
        public static MusicManager create(PluginBase plugin, MusicManager.SoundEventIdResolver soundEventIdResolver) {
            net.blueva.foundation.music.MidiLibrary library = new net.blueva.foundation.music.MidiLibrary(plugin.getLogger());
            library.setSoundsDirectory(plugin.getDataDirectory().resolve("sounds"));
            return new MusicManager(plugin.getLogger(), library, soundEventIdResolver);
        }

        /**
         * Convenience for {@link #create} that installs and uses the bundled
         * instrument sound pack automatically - see {@link InstrumentSounds}.
         * A restart is required after the first install before any sound plays.
         */
        public static MusicManager createWithInstrumentSounds(PluginBase plugin) {
            InstrumentSounds.installIfNeeded(plugin, null);
            return create(plugin, InstrumentSounds.resolver());
        }
    }
}
