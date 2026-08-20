package net.blueva.foundation.music;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.blueva.foundation.scheduler.Scheduler;
import org.joml.Vector3d;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Plays MIDI melodies to individual players using Hytale's sound-event
 * pipeline, one native sound event per note.
 *
 * <p>Unlike Bukkit, Hytale has no built-in note-block instrument sounds to
 * fall back on: each note is looked up as a sound event via the
 * {@link SoundEventIdResolver} supplied at construction, so this class makes
 * no assumption about which asset pack (or naming convention) actually
 * provides those events - a note whose event id isn't registered is silently
 * skipped, with a warning logged.</p>
 */
@SuppressWarnings("removal") // Entity#getUuid()/Player#getPlayerRef() are marked for removal in this
// server build with no documented replacement yet - Hytale's plugin API is still young (see README).
public final class MusicManager {

    private static final String DEFAULT_INSTRUMENT = "Piano";
    private static final long TICK_PERIOD_MS = 50L;
    private static final int[] NOTE_LENGTHS_MS = {
            125, 250, 375, 500, 625, 750, 875,
            1000, 1250, 1500, 1750, 2000, 2500, 3000, 4000
    };

    private final HytaleLogger logger;
    private final MidiLibrary library;
    private final SoundEventIdResolver soundEventIdResolver;
    private final Map<UUID, PlaybackSession> sessions = new ConcurrentHashMap<>();

    public MusicManager(HytaleLogger logger, MidiLibrary library, SoundEventIdResolver soundEventIdResolver) {
        this.logger = logger;
        this.library = library;
        this.soundEventIdResolver = Objects.requireNonNull(soundEventIdResolver, "soundEventIdResolver");
    }

    /** Starts playing a MIDI melody to {@code player} with the default instrument (Piano). */
    public void play(Player player, String melodyId) {
        play(player, melodyId, null);
    }

    /**
     * Starts playing a MIDI melody to {@code player}.
     *
     * @param instrument instrument name (e.g. {@code "Lute"}, {@code "Flute"}), or {@code null} for Piano
     */
    public void play(Player player, String melodyId, String instrument) {
        library.get(melodyId).ifPresentOrElse(
                melody -> playMelody(player, melody, 0L, resolveInstrument(instrument)),
                () -> logger.atWarning().log("Cannot play MIDI '%s': melody not found", melodyId)
        );
    }

    /** Stops any active MIDI playback for {@code player}. */
    public void stop(Player player) {
        stop(player.getUuid());
    }

    public void stop(UUID playerId) {
        PlaybackSession session = sessions.remove(playerId);
        if (session != null && session.future != null) {
            session.future.cancel(false);
        }
    }

    /** Pauses playback without losing position; resume with {@link #resume(Player)}. */
    public void pause(Player player) {
        PlaybackSession session = sessions.get(player.getUuid());
        if (session == null || session.paused) {
            return;
        }
        session.paused = true;
        session.pausedAtMs = System.currentTimeMillis() - session.startWallMs;
        if (session.future != null) {
            session.future.cancel(false);
        }
    }

    /** @return {@code true} if a paused session was actually resumed. */
    public boolean resume(Player player) {
        PlaybackSession session = sessions.get(player.getUuid());
        if (session == null || !session.paused) {
            return false;
        }
        playMelody(player, session.melody, session.pausedAtMs, session.instrument);
        return true;
    }

    private void playMelody(Player player, Melody melody, long offsetMs, String instrument) {
        UUID uuid = player.getUuid();
        stop(uuid);

        long startWall = System.currentTimeMillis() - offsetMs;
        PlaybackSession session = new PlaybackSession(melody, startWall, offsetMs, instrument);
        sessions.put(uuid, session);
        session.future = Scheduler.runTimer(() -> tick(player, uuid), 0L, TICK_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    private void tick(Player player, UUID uuid) {
        PlaybackSession session = sessions.get(uuid);
        if (session == null || session.paused) {
            return;
        }

        long now = System.currentTimeMillis();
        long playbackMs = now - session.startWallMs;
        long prevMs = session.lastTickMs;
        session.lastTickMs = playbackMs;

        if (playbackMs >= session.melody.durationMs()) {
            stop(uuid);
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            return;
        }

        // Store/SoundUtil calls must run on the world thread; the time window
        // above is cheap enough to compute off it.
        String instrument = session.instrument;
        world.execute(() -> {
            EntityStore entityStore = world.getEntityStore();
            PlayerRef playerRef = player.getPlayerRef();
            Ref<EntityStore> playerEntityRef = playerRef == null ? null : playerRef.getReference();
            for (Melody.Track track : session.melody.tracks()) {
                for (Melody.Note note : track.notes()) {
                    if (note.timeMs() >= prevMs && note.timeMs() < playbackMs) {
                        playNote(note, instrument, entityStore, playerEntityRef);
                    }
                }
            }
        });
    }

    private void playNote(Melody.Note note, String instrument, EntityStore entityStore, Ref<EntityStore> playerRef) {
        if (playerRef == null) {
            // Skip rather than broadcasting to everyone nearby if we can't resolve the player.
            return;
        }

        float pitch = (float) Math.pow(2.0, (note.midiNote() - 24) / 12.0);
        int octave = 1;
        while (octave < 8 && pitch > 4.0f / 3.0f) {
            pitch /= 2f;
            octave++;
        }

        float volume = note.velocity() / 64.0f;
        // Perceived loudness falls off as pitch/octave rise; compensate so
        // higher notes don't sound artificially louder than lower ones.
        float factor = 0.5f;
        float adjustedVolume = (float) (volume / Math.sqrt(pitch * Math.pow(2.0, octave - 4.0)));
        volume = volume * (1.0f - factor) + adjustedVolume * factor;

        int lengthMs = closestNoteLength(note.lengthMs());
        String soundEventId = soundEventIdResolver.resolve(instrument, octave, lengthMs);

        int soundIndex;
        try {
            soundIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        } catch (RuntimeException e) {
            logger.atWarning().log("MIDI sound event not found: %s", soundEventId);
            return;
        }
        if (soundIndex < 0) {
            return;
        }

        Vector3d position = resolvePosition(entityStore, playerRef);
        SoundUtil.playSoundEvent3dToPlayer(playerRef, soundIndex, SoundCategory.SFX,
                position.x, position.y, position.z, volume, pitch, entityStore.getStore());
    }

    private static Vector3d resolvePosition(EntityStore entityStore, Ref<EntityStore> ref) {
        TransformComponent transform = entityStore.getStore().getComponent(ref, TransformComponent.getComponentType());
        return transform != null ? transform.getPosition() : new Vector3d(0.0, 0.0, 0.0);
    }

    private static int closestNoteLength(int lengthMs) {
        int closest = NOTE_LENGTHS_MS[0];
        for (int candidate : NOTE_LENGTHS_MS) {
            if (Math.abs(lengthMs - candidate) < Math.abs(lengthMs - closest)) {
                closest = candidate;
            }
        }
        return closest;
    }

    private static String resolveInstrument(String instrument) {
        if (instrument == null || instrument.trim().isEmpty()) {
            return DEFAULT_INSTRUMENT;
        }
        String trimmed = instrument.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static final class PlaybackSession {
        private final Melody melody;
        private final String instrument;
        private long startWallMs;
        private long lastTickMs;
        private boolean paused;
        private long pausedAtMs;
        private volatile ScheduledFuture<?> future;

        private PlaybackSession(Melody melody, long startWallMs, long lastTickMs, String instrument) {
            this.melody = melody;
            this.startWallMs = startWallMs;
            this.lastTickMs = lastTickMs;
            this.instrument = instrument;
        }
    }

    /**
     * Resolves the sound-event id to look up for one note. BlueFoundation
     * ships no default: which asset pack provides per-instrument,
     * per-octave, per-note-length sound events (and what it names them) is
     * entirely up to the server, so the caller supplies this when building a
     * {@link MusicManager}.
     */
    @FunctionalInterface
    public interface SoundEventIdResolver {
        /**
         * @param instrument capitalized instrument name (e.g. {@code "Piano"}, {@code "Lute"})
         * @param octave     1-8
         * @param lengthMs   note length in milliseconds, snapped to the nearest of a fixed set of buckets
         */
        String resolve(String instrument, int octave, int lengthMs);
    }
}
