package net.blueva.foundation.music;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Loads and caches {@link Melody} objects from a directory of {@code .midi}
 * files.
 */
public final class MidiLibrary {

    private final HytaleLogger logger;
    private final ConcurrentMap<String, Melody> cache = new ConcurrentHashMap<>();
    private volatile Path soundsDirectory;

    public MidiLibrary(HytaleLogger logger) {
        this.logger = logger;
    }

    /** Sets (or updates) the base directory .midi files are resolved against. */
    public void setSoundsDirectory(Path soundsDirectory) {
        this.soundsDirectory = soundsDirectory;
    }

    /**
     * Loads a melody by id (a path relative to the sounds directory, without
     * extension, e.g. {@code "effect/next_game"}). Empty if the file is
     * missing or fails to parse.
     */
    public Optional<Melody> get(String melodyId) {
        Melody melody = cache.computeIfAbsent(melodyId, this::load);
        return Optional.ofNullable(melody);
    }

    /** Clears the melody cache - call this on plugin reload. */
    public void clear() {
        cache.clear();
    }

    private Melody load(String melodyId) {
        Path directory = soundsDirectory;
        if (directory == null) {
            logger.atWarning().log("MIDI sounds directory not initialised - cannot load '%s'", melodyId);
            return null;
        }
        Path file = directory.resolve(melodyId + ".midi");
        if (!Files.exists(file)) {
            logger.atWarning().log("MIDI file not found: %s", file);
            return null;
        }
        try (InputStream stream = Files.newInputStream(file)) {
            return MidiParser.parse(melodyId, stream);
        } catch (IOException e) {
            logger.atWarning().log("Failed to parse MIDI %s: %s", melodyId, e.getMessage());
            return null;
        }
    }
}
