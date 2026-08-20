package net.blueva.foundation.scoreboard;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.blueva.foundation.dependencies.Dependencies;
import net.blueva.foundation.reflection.Reflection;

import java.util.List;

/**
 * Experimental sidebar-style scoreboard. Hytale's real server API has no
 * scoreboard/sidebar/boss-bar concept at all (verified against the
 * hytale-shared-source-pre-release reference source); this builds one on top
 * of a persistent HUD page with one label per line, using a third-party
 * HTML/CSS-like UI library ({@code compileOnly} - see this module's build
 * file for the exact coordinates).
 *
 * <p>That library is not bundled: it's not Hytale's own long-term UI system,
 * so shading it into every consumer's jar "just in case" would be wasteful.
 * Instead it's downloaded straight into the plugin's own classloader at
 * runtime, via {@link Dependencies}, the first time one of these methods is
 * actually called - the same way BlueFoundation itself bootstraps its own
 * optional runtime dependencies elsewhere. The actual typed usage lives in
 * {@link HyUiBridge}, kept separate so this class can always load, whether
 * or not the library is present, and only touches {@code HyUiBridge} - which
 * does reference it - after the download above has already happened.</p>
 */
public class Scoreboards {

    private static final String LIBRARY_GROUP_ID = "curse.maven";
    private static final String LIBRARY_ARTIFACT_ID = "hyui-1431415";
    private static final String LIBRARY_VERSION = "8363317"; // CurseForge file id, not a semantic version
    private static final String LIBRARY_REPOSITORY_URL = "https://www.cursemaven.com/";
    private static final String PROBE_CLASS = "au.ellie.hyui.builders.HudBuilder";

    private static volatile boolean libraryReady;

    protected Scoreboards() {
    }

    /**
     * Shows (or replaces) a sidebar scoreboard for one player: a title label
     * followed by one label per line, anchored to the top-right of the screen.
     */
    public static ScoreboardHandle show(PluginBase plugin, PlayerRef playerRef, String title, List<String> lines) {
        ensureLibraryLoaded(plugin);
        return HyUiBridge.show(playerRef, title, lines);
    }

    /** Updates an existing scoreboard's title and lines in place, without rebuilding the HUD. */
    public static void update(ScoreboardHandle handle, String title, List<String> lines) {
        if (handle == null) {
            return;
        }
        HyUiBridge.update(handle, title, lines);
    }

    public static void hide(ScoreboardHandle handle) {
        if (handle == null) {
            return;
        }
        HyUiBridge.hide(handle);
    }

    /**
     * Downloads the UI library into the plugin's own classloader, unless
     * it's already present (e.g. the consumer shaded it in themselves).
     */
    private static void ensureLibraryLoaded(PluginBase plugin) {
        if (libraryReady || Reflection.classExists(PROBE_CLASS)) {
            libraryReady = true;
            return;
        }
        synchronized (Scoreboards.class) {
            if (libraryReady || Reflection.classExists(PROBE_CLASS)) {
                libraryReady = true;
                return;
            }
            Dependencies.loader(plugin.getDataDirectory(), plugin.getClass().getClassLoader(), null)
                    .load(Dependencies.of(LIBRARY_GROUP_ID, LIBRARY_ARTIFACT_ID, LIBRARY_VERSION, LIBRARY_REPOSITORY_URL));
            libraryReady = true;
        }
    }

    /**
     * Opaque handle to a shown scoreboard - pass it to {@link #update} or
     * {@link #hide}. Holds its library-specific handle as {@code Object} so
     * that this class never references the UI library's types itself.
     */
    public static final class ScoreboardHandle {
        final Object hud;

        ScoreboardHandle(Object hud) {
            this.hud = hud;
        }
    }
}
