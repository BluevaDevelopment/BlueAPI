package net.blueva.foundation.version;

import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.server.core.plugin.PluginBase;

/**
 * Version helpers for a plugin's own manifest.
 *
 * <p>Unlike Bukkit's {@code Version}, this has nothing to do with the running
 * server: Hytale plugins are versioned with real semver (see
 * {@code manifest.json}'s {@code Version} field), and the shared Hytale server
 * jar exposes no public "server version" accessor to compare against.</p>
 */
public class Version {

    protected Version() {
    }

    public static Semver pluginVersion(PluginBase plugin) {
        return plugin.getManifest().getVersion();
    }
}
