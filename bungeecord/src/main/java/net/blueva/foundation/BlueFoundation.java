package net.blueva.foundation;

import net.md_5.bungee.api.plugin.Plugin;

import java.util.Collection;

/**
 * Master entry point for BlueFoundation on BungeeCord.
 *
 * <p>This class intentionally stays small. Real implementations live in
 * dedicated packages, while these nested aliases keep the public API reachable
 * from a single namespace: {@code BlueFoundation.*}. Only the facades that make
 * sense on a proxy exist here - see the project README for the full per-platform
 * feature matrix.</p>
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

        public static Loader loader(Plugin plugin) {
            return new Loader(plugin.getDataFolder().toPath(), plugin.getClass().getClassLoader(), plugin.getLogger());
        }

        public static void load(Plugin plugin, Collection<? extends RuntimeDependency> dependencies) {
            loader(plugin).loadAll(dependencies);
        }

        public static void load(Plugin plugin, RuntimeDependency dependency) {
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

        public static net.blueva.foundation.config.ConfigFile yaml(Plugin plugin, String resourceName) {
            return load(plugin, resourceName, net.blueva.foundation.config.ConfigFormat.YAML);
        }

        public static net.blueva.foundation.config.ConfigFile toml(Plugin plugin, String resourceName) {
            return load(plugin, resourceName, net.blueva.foundation.config.ConfigFormat.TOML);
        }

        public static net.blueva.foundation.config.ConfigFile load(Plugin plugin, String resourceName, net.blueva.foundation.config.ConfigFormat format) {
            return load(plugin, resourceName, format, net.blueva.foundation.config.ConfigUpdatePolicy.MERGE_DEFAULTS);
        }

        public static net.blueva.foundation.config.ConfigFile load(Plugin plugin, String resourceName, net.blueva.foundation.config.ConfigFormat format, net.blueva.foundation.config.ConfigUpdatePolicy updatePolicy) {
            if (plugin == null) {
                throw new IllegalArgumentException("plugin cannot be null");
            }
            return load(plugin.getDataFolder().toPath(), plugin.getClass().getClassLoader(), resourceName, format, updatePolicy);
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

    public static class Players extends net.blueva.foundation.players.Players {
        private Players() {
        }
    }
}
