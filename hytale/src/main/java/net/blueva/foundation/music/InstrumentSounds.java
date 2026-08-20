package net.blueva.foundation.music;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import net.blueva.foundation.assets.JarAssetInjector;
import net.blueva.foundation.scheduler.Scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads the bundled instrument sound pack from
 * <a href="https://github.com/BluevaDevelopment/HytaleSounds">HytaleSounds</a>
 * (GPL-3.0) and injects it into the calling plugin's own jar via
 * {@link JarAssetInjector}, so {@link MusicManager} has real per-instrument
 * sound events to play without BlueFoundation itself bundling ~19MB of audio
 * for every consumer, whether they use {@code Music} or not.
 *
 * <p>Like any other jar-injection change, this only takes effect after a
 * server restart - and the consuming plugin's own {@code manifest.json} must
 * set {@code "IncludesAssetPack": true} for Hytale to load the injected
 * {@code Common/Sounds} and {@code Server/Audio} entries at all.</p>
 */
public final class InstrumentSounds {

    /** GitHub's "download zip" archive link for the sound pack repository's default branch. */
    private static final String ARCHIVE_URL = "https://github.com/BluevaDevelopment/HytaleSounds/archive/refs/heads/main.zip";

    /** Only exists once the pack has actually been injected - doubles as the "already installed" check. */
    private static final String MARKER_ENTRY = "Server/Audio/AudioCategories/Ymmersive_Melodies_Instrument.json";

    private static final int TIMEOUT_MILLIS = 30_000;
    private static final long MAX_ARCHIVE_BYTES = 64L * 1024 * 1024;

    private InstrumentSounds() {
    }

    /** A {@link MusicManager.SoundEventIdResolver} matching the bundled sound pack's event naming convention. */
    public static MusicManager.SoundEventIdResolver resolver() {
        return (instrument, octave, lengthMs) ->
                String.format(Locale.ROOT, "SFX_Ymmersive_Melodies_%s_C%d_%dms", instrument, octave, lengthMs);
    }

    /** @return {@code true} if the sound assets are already injected into {@code plugin}'s jar. */
    public static boolean isInstalled(PluginBase plugin) {
        return JarAssetInjector.hasEntry(plugin.getClass(), MARKER_ENTRY);
    }

    /**
     * Downloads and injects the sound assets into {@code plugin}'s own jar,
     * unless they are already present. Runs off the main thread; safe to call
     * from {@code onEnable}. {@code onComplete} - if given - is called with
     * {@code true} on success (including "was already installed"), or
     * {@code false} on failure, on no particular thread.
     */
    public static void installIfNeeded(PluginBase plugin, Consumer<Boolean> onComplete) {
        if (isInstalled(plugin)) {
            if (onComplete != null) {
                onComplete.accept(true);
            }
            return;
        }
        HytaleLogger logger = plugin.getLogger();
        Scheduler.runLater(() -> {
            boolean ok;
            try {
                Path cacheDirectory = plugin.getDataDirectory().resolve("cache").resolve("hytale-sounds");
                Files.createDirectories(cacheDirectory);
                download(cacheDirectory);
                JarAssetInjector.writeTree(plugin.getClass(), cacheDirectory, "");
                logger.atWarning().log("Installed HytaleSounds instrument assets into the plugin jar - restart the server for them to take effect.");
                ok = true;
            } catch (Exception e) {
                logger.atWarning().log("Could not install HytaleSounds instrument assets: %s", e.getMessage());
                ok = false;
            }
            if (onComplete != null) {
                onComplete.accept(ok);
            }
        }, 0L, TimeUnit.MILLISECONDS);
    }

    private static void download(Path cacheDirectory) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(ARCHIVE_URL).toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "BlueFoundation");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HytaleSounds download failed: HTTP " + status);
            }
            try (InputStream in = connection.getInputStream(); ZipInputStream zip = new ZipInputStream(in)) {
                long total = 0L;
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String relative = stripArchiveRoot(entry.getName());
                    if (entry.isDirectory() || relative == null || !isWantedPath(relative)) {
                        continue;
                    }
                    Path target = cacheDirectory.resolve(relative);
                    if (!target.normalize().startsWith(cacheDirectory.normalize())) {
                        continue; // zip-slip guard
                    }
                    Files.createDirectories(target.getParent());
                    total += copyBounded(zip, target, MAX_ARCHIVE_BYTES - total);
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    /** Strips the top-level "HytaleSounds-&lt;branch&gt;/" directory GitHub wraps archive entries in. */
    private static String stripArchiveRoot(String entryName) {
        String normalized = entryName.replace('\\', '/');
        int slash = normalized.indexOf('/');
        return slash < 0 ? null : normalized.substring(slash + 1);
    }

    private static boolean isWantedPath(String relative) {
        return relative.startsWith("Common/Sounds/") || relative.startsWith("Server/Audio/");
    }

    private static long copyBounded(InputStream source, Path target, long remaining) throws IOException {
        if (remaining <= 0) {
            throw new IOException("HytaleSounds archive exceeded the size limit");
        }
        try (OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            long written = 0L;
            int read;
            while ((read = source.read(buffer)) > 0) {
                written += read;
                if (written > remaining) {
                    throw new IOException("HytaleSounds archive exceeded the size limit");
                }
                out.write(buffer, 0, read);
            }
            return written;
        }
    }
}
