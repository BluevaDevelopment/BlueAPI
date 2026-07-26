package net.blueva.foundation.music;

import java.util.Collections;
import java.util.List;

final class MusicTrack {

    private final List<MusicNote> notes;
    private final long durationMillis;
    private final boolean loopEnabled;
    private final long loopStartMillis;
    private final int maximumLoops;

    MusicTrack(List<MusicNote> notes, long durationMillis) {
        this(notes, durationMillis, false, 0L, 0);
    }

    MusicTrack(List<MusicNote> notes, long durationMillis,
               boolean loopEnabled, long loopStartMillis, int maximumLoops) {
        this.notes = Collections.unmodifiableList(notes);
        this.durationMillis = Math.max(0L, durationMillis);
        this.loopEnabled = loopEnabled && loopStartMillis < this.durationMillis;
        this.loopStartMillis = Math.max(0L, Math.min(loopStartMillis, this.durationMillis));
        this.maximumLoops = Math.max(0, maximumLoops);
    }

    List<MusicNote> notes() {
        return notes;
    }

    long durationMillis() {
        return durationMillis;
    }

    boolean loopEnabled() {
        return loopEnabled;
    }

    long loopStartMillis() {
        return loopStartMillis;
    }

    int maximumLoops() {
        return maximumLoops;
    }
}

final class MusicNote implements Comparable<MusicNote> {

    private final long timeMillis;
    private final InstrumentSound sound;
    private final float pitch;
    private final float volume;
    private final float pan;

    MusicNote(long timeMillis, InstrumentSound sound, float pitch, float volume, float pan) {
        this.timeMillis = Math.max(0L, timeMillis);
        this.sound = sound;
        this.pitch = pitch;
        this.volume = volume;
        this.pan = pan;
    }

    long timeMillis() {
        return timeMillis;
    }

    InstrumentSound sound() {
        return sound;
    }

    float pitch() {
        return pitch;
    }

    float volume() {
        return volume;
    }

    float pan() {
        return pan;
    }

    @Override
    public int compareTo(MusicNote other) {
        return Long.compare(timeMillis, other.timeMillis);
    }
}
