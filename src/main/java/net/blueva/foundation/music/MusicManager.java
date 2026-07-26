package net.blueva.foundation.music;

import net.blueva.foundation.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and plays NBS and MIDI music without an additional server plugin.
 *
 * <p>Files are parsed away from the server thread and cached until their size
 * or modification date changes. Playback follows elapsed wall-clock time and
 * emits every note due during the current server tick, so high-tempo songs do
 * not progressively fall behind.</p>
 */
public final class MusicManager implements AutoCloseable {

    private final Plugin plugin;
    private final Map<UUID, Playback> active = new ConcurrentHashMap<>();
    private final Map<String, CachedTrack> cache = new ConcurrentHashMap<>();
    private volatile Scheduler.Task sharedTask;
    private volatile boolean closed;

    public MusicManager(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
    }

    public MusicPlayback play(Player player, File file) {
        return play(player, file, MusicOptions.defaults());
    }

    public MusicPlayback play(Player player, File file, MusicOptions options) {
        if (closed) {
            throw new IllegalStateException("Music manager is closed");
        }
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Music file does not exist: " + file);
        }
        MusicOptions safeOptions = options == null ? MusicOptions.defaults() : options;
        Playback playback = new Playback(player, safeOptions);
        Playback previous = active.put(player.getUniqueId(), playback);
        if (previous != null) {
            previous.stop();
        }
        ensureSharedTask();

        Scheduler.async(plugin, () -> {
            try {
                MusicTrack track = load(file);
                if (!playback.isCurrent()) {
                    return;
                }
                Scheduler.runAtEntityWithFallback(plugin, player,
                        ignored -> playback.start(track), playback::stop);
            } catch (Exception exception) {
                playback.stop();
                plugin.getLogger().warning("Unable to play music file '" + file.getName()
                        + "': " + exception.getMessage());
            }
        });
        return playback;
    }

    public void stop(Player player) {
        Playback playback = player == null ? null : active.remove(player.getUniqueId());
        if (playback != null) {
            playback.stop();
        }
    }

    public void pause(Player player) {
        Playback playback = player == null ? null : active.get(player.getUniqueId());
        if (playback != null) {
            playback.pause();
        }
    }

    public void resume(Player player) {
        Playback playback = player == null ? null : active.get(player.getUniqueId());
        if (playback != null) {
            playback.resume();
        }
    }

    public void stopAll() {
        for (Playback playback : active.values()) {
            playback.stop();
        }
        active.clear();
    }

    public void clearCache() {
        cache.clear();
    }

    @Override
    public void close() {
        closed = true;
        stopAll();
        clearCache();
        stopSharedTask();
    }

    private void tickAll() {
        for (Playback playback : active.values()) {
            playback.tick();
        }
    }

    private synchronized void ensureSharedTask() {
        if (closed || active.isEmpty() || Scheduler.isFoliaRuntime()) {
            return;
        }
        if (sharedTask == null || sharedTask.cancelled()) {
            sharedTask = Scheduler.syncTimer(plugin, this::tickAll, 1L, 1L);
        }
    }

    private synchronized void stopSharedTaskIfIdle() {
        if (active.isEmpty()) {
            stopSharedTask();
        }
    }

    private synchronized void stopSharedTask() {
        if (sharedTask != null && !sharedTask.cancelled()) {
            sharedTask.cancel();
        }
        sharedTask = null;
    }

    private MusicTrack load(File file) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        long modified = file.lastModified();
        long size = file.length();
        CachedTrack cached = cache.get(canonicalPath);
        if (cached != null && cached.matches(modified, size)) {
            return cached.track;
        }

        String name = file.getName().toLowerCase(Locale.ROOT);
        MusicTrack track;
        if (name.endsWith(".nbs")) {
            track = new NbsTrackLoader().load(file);
        } else if (name.endsWith(".mid") || name.endsWith(".midi")) {
            track = new MidiTrackLoader().load(file);
        } else {
            throw new MusicFormatException("Supported music formats are .nbs, .mid and .midi");
        }
        cache.put(canonicalPath, new CachedTrack(modified, size, track));
        return track;
    }

    private final class Playback implements MusicPlayback {

        private final Player player;
        private final float volume;
        private volatile double speed;
        private volatile boolean paused;
        private volatile boolean stopped;
        private MusicTrack track;
        private Scheduler.Task task;
        private int noteIndex;
        private int completedLoops;
        private double basePositionMillis;
        private long anchorNanos;

        private Playback(Player player, MusicOptions options) {
            this.player = player;
            this.speed = options.getSpeed();
            this.volume = options.getVolume();
        }

        private synchronized void start(MusicTrack track) {
            if (stopped || !isCurrent() || !player.isOnline()) {
                stop();
                return;
            }
            this.track = track;
            this.anchorNanos = System.nanoTime();
            if (Scheduler.isFoliaRuntime()) {
                this.task = Scheduler.runAtEntityTimer(plugin, player, this::tick,
                        this::stop, 1L, 1L);
            }
        }

        private synchronized void tick() {
            if (stopped || paused || track == null) {
                return;
            }
            if (!player.isOnline() || !isCurrent()) {
                stop();
                return;
            }

            double position = currentPositionMillis();
            int loopsThisTick = 0;
            while (true) {
                emitNotesThrough(position);
                if (position < track.durationMillis()) {
                    break;
                }
                if (!canLoop() || ++loopsThisTick > 128) {
                    stop();
                    break;
                }

                completedLoops++;
                position = track.loopStartMillis() + position - track.durationMillis();
                noteIndex = firstNoteAtOrAfter(track.loopStartMillis());
                basePositionMillis = position;
                anchorNanos = System.nanoTime();
            }
        }

        private void emitNotesThrough(double position) {
            while (noteIndex < track.notes().size()
                    && track.notes().get(noteIndex).timeMillis() <= position) {
                MusicNote note = track.notes().get(noteIndex++);
                try {
                    note.sound().play(player, note.volume() * volume, note.pitch(), note.pan());
                } catch (RuntimeException exception) {
                    plugin.getLogger().fine("Unable to emit a music note: " + exception.getMessage());
                }
            }
        }

        private boolean canLoop() {
            return track.loopEnabled()
                    && (track.maximumLoops() == 0 || completedLoops < track.maximumLoops());
        }

        private int firstNoteAtOrAfter(long timeMillis) {
            int low = 0;
            int high = track.notes().size();
            while (low < high) {
                int middle = low + (high - low) / 2;
                if (track.notes().get(middle).timeMillis() < timeMillis) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }

        private boolean isCurrent() {
            return !stopped && active.get(player.getUniqueId()) == this;
        }

        private double currentPositionMillis() {
            if (paused) {
                return basePositionMillis;
            }
            return basePositionMillis
                    + (System.nanoTime() - anchorNanos) / 1_000_000.0D * speed;
        }

        @Override
        public boolean isPlaying() {
            return !stopped && !paused;
        }

        @Override
        public boolean isPaused() {
            return !stopped && paused;
        }

        @Override
        public synchronized long getDurationMillis() {
            return track == null ? 0L : track.durationMillis();
        }

        @Override
        public double getSpeed() {
            return speed;
        }

        @Override
        public synchronized void setSpeed(double speed) {
            double validated = MusicOptions.validateSpeed(speed);
            if (track != null && !paused && !stopped) {
                basePositionMillis = currentPositionMillis();
                anchorNanos = System.nanoTime();
            }
            this.speed = validated;
        }

        @Override
        public synchronized void pause() {
            if (stopped || paused) {
                return;
            }
            if (track != null) {
                basePositionMillis = currentPositionMillis();
            }
            paused = true;
        }

        @Override
        public synchronized void resume() {
            if (stopped || !paused) {
                return;
            }
            anchorNanos = System.nanoTime();
            paused = false;
        }

        @Override
        public synchronized void stop() {
            if (stopped) {
                return;
            }
            stopped = true;
            if (task != null && !task.cancelled()) {
                task.cancel();
            }
            active.remove(player.getUniqueId(), this);
            stopSharedTaskIfIdle();
        }
    }

    private static final class CachedTrack {
        private final long modified;
        private final long size;
        private final MusicTrack track;

        private CachedTrack(long modified, long size, MusicTrack track) {
            this.modified = modified;
            this.size = size;
            this.track = track;
        }

        private boolean matches(long modified, long size) {
            return this.modified == modified && this.size == size;
        }
    }
}
