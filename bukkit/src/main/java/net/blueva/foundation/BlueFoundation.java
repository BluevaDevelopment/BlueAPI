package net.blueva.foundation;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Master entry point for BlueFoundation.
 *
 * <p>This class intentionally stays small. Real implementations live in
 * dedicated packages, while these nested aliases keep the public API reachable
 * from a single namespace: {@code BlueFoundation.*}.</p>
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

        try (InputStream input = BlueFoundation.class.getClassLoader().getResourceAsStream(
                "META-INF/maven/net.blueva.foundation/BlueFoundation/pom.properties")) {
            if (input == null) {
                return UNKNOWN_VERSION;
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("version", UNKNOWN_VERSION);
        } catch (IOException ignored) {
            return UNKNOWN_VERSION;
        }
    }

    public static class Dependencies extends net.blueva.foundation.dependencies.Dependencies {
        private Dependencies() {
        }

        public static Loader loader(JavaPlugin plugin) {
            return new Loader(plugin.getDataFolder().toPath(), plugin.getClass().getClassLoader(), plugin.getLogger());
        }

        public static void load(JavaPlugin plugin, Collection<? extends RuntimeDependency> dependencies) {
            loader(plugin).loadAll(dependencies);
        }

        public static void load(JavaPlugin plugin, RuntimeDependency dependency) {
            loader(plugin).load(dependency);
        }

        /**
         * Previously loaded the Adventure stack required by BlueFoundation text/message
         * utilities. This is now a no-op: BlueFoundation provides its own MiniMessage
         * parser and legacy serializers that do not require Adventure at runtime.
         *
         * @deprecated kept for backwards compatibility; does nothing.
         */
        @Deprecated
        public static void loadAdventure(JavaPlugin plugin) {
            // Adventure is no longer required at runtime.
        }

        /**
         * @return an empty list; Adventure is no longer required at runtime.
         * @deprecated BlueFoundation no longer needs Adventure on Spigot/Bukkit.
         */
        @Deprecated
        public static List<RuntimeDependency> adventureDependencies() {
            return new ArrayList<>();
        }
    }

    public static class Version extends net.blueva.foundation.version.Version {
        private Version() {
        }
    }

    public static class Reflection extends net.blueva.foundation.reflection.Reflection {
        private Reflection() {
        }
    }

    public static class Materials extends net.blueva.foundation.materials.Materials {
        private Materials() {
        }
    }

    public static class Items extends net.blueva.foundation.items.Items {
        private Items() {
        }
    }

    public static class Sounds extends net.blueva.foundation.sounds.Sounds {
        private Sounds() {
        }
    }

    public static class Music {
        private Music() {
        }

        public static net.blueva.foundation.music.MusicManager create(
                org.bukkit.plugin.Plugin plugin) {
            return new net.blueva.foundation.music.MusicManager(plugin);
        }
    }

    public static class Entities extends net.blueva.foundation.entities.Entities {
        private Entities() {
        }
    }

    public static class Scheduler extends net.blueva.foundation.scheduler.Scheduler {
        private Scheduler() {
        }
    }

    public static class Commands extends net.blueva.foundation.commands.Commands {
        private Commands() {
        }
    }

    public static class Messages extends net.blueva.foundation.messages.Messages {
        private Messages() {
        }
    }

    public static class Text extends net.blueva.foundation.text.Text {
        private Text() {
        }
    }

    public static class Events extends net.blueva.foundation.events.Events {
        private Events() {
        }
    }

    public static class Configs extends net.blueva.foundation.config.Configs {
        private Configs() {
        }

        public static net.blueva.foundation.config.ConfigFile yaml(JavaPlugin plugin, String resourceName) {
            return load(plugin, resourceName, net.blueva.foundation.config.ConfigFormat.YAML);
        }

        public static net.blueva.foundation.config.ConfigFile toml(JavaPlugin plugin, String resourceName) {
            return load(plugin, resourceName, net.blueva.foundation.config.ConfigFormat.TOML);
        }

        public static net.blueva.foundation.config.ConfigFile load(JavaPlugin plugin, String resourceName, net.blueva.foundation.config.ConfigFormat format) {
            return load(plugin, resourceName, format, net.blueva.foundation.config.ConfigUpdatePolicy.MERGE_DEFAULTS);
        }

        public static net.blueva.foundation.config.ConfigFile load(JavaPlugin plugin, String resourceName, net.blueva.foundation.config.ConfigFormat format, net.blueva.foundation.config.ConfigUpdatePolicy updatePolicy) {
            if (plugin == null) {
                throw new IllegalArgumentException("plugin cannot be null");
            }
            return load(plugin.getDataFolder().toPath(), plugin.getClass().getClassLoader(), resourceName, format, updatePolicy);
        }
    }

    public static class NPCs extends net.blueva.foundation.npc.NPCs {
        private NPCs() {
        }
    }

    public static class Players extends net.blueva.foundation.players.Players {
        private Players() {
        }
    }

    public static class Worlds extends net.blueva.foundation.worlds.Worlds {
        private Worlds() {
        }
    }

    public static class Blocks extends net.blueva.foundation.blocks.Blocks {
        private Blocks() {
        }
    }

    public static class Particles extends net.blueva.foundation.particles.Particles {
        private Particles() {
        }
    }

    public static class Attributes extends net.blueva.foundation.attributes.Attributes {
        private Attributes() {
        }
    }

    public static class GameRules extends net.blueva.foundation.gamerules.GameRules {
        private GameRules() {
        }
    }

    public static class BossBars extends net.blueva.foundation.bossbar.BossBars {
        private BossBars() {
        }
    }

    public static class Inventories extends net.blueva.foundation.inventories.Inventories {
        private Inventories() {
        }
    }

    public static class Menus extends net.blueva.foundation.menus.Menus {
        private Menus() {
        }
    }

    public static class Forms extends net.blueva.foundation.menus.bedrock.Forms {
        private Forms() {
        }
    }

    public static class Bedrock extends net.blueva.foundation.menus.bedrock.Bedrock {
        private Bedrock() {
        }
    }

    public static class Dialogs extends net.blueva.foundation.menus.dialogs.Dialogs {
        private Dialogs() {
        }
    }

    public static class Scoreboards {
        private Scoreboards() {
        }

        public static net.blueva.foundation.scoreboard.BfScoreboard create(org.bukkit.entity.Player player) {
            return net.blueva.foundation.scoreboard.Scoreboards.create(player);
        }

        public static net.blueva.foundation.scoreboard.BfScoreboardHandle createHandle(
                org.bukkit.entity.Player player) {
            return net.blueva.foundation.scoreboard.Scoreboards.createHandle(player);
        }
    }
}
