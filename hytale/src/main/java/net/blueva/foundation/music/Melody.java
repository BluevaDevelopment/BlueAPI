package net.blueva.foundation.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory representation of a parsed MIDI song.
 */
public final class Melody {

    private final String name;
    private final List<Track> tracks;
    private final int durationMs;

    public Melody(String name, List<Track> tracks) {
        this.name = name;
        this.tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
        this.durationMs = computeDuration(this.tracks);
    }

    public String name() {
        return name;
    }

    public List<Track> tracks() {
        return tracks;
    }

    /** Total playback length in milliseconds. */
    public int durationMs() {
        return durationMs;
    }

    private static int computeDuration(List<Track> tracks) {
        int max = 0;
        for (Track track : tracks) {
            for (Note note : track.notes()) {
                int end = note.timeMs() + note.lengthMs();
                if (end > max) {
                    max = end;
                }
            }
        }
        return max;
    }

    public static final class Track {
        private final String name;
        private final List<Note> notes;

        public Track(String name, List<Note> notes) {
            this.name = name;
            this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        }

        public String name() {
            return name;
        }

        public List<Note> notes() {
            return notes;
        }
    }

    public static final class Note {
        private final int midiNote;
        private final int velocity;
        private final int timeMs;
        private final int lengthMs;

        public Note(int midiNote, int velocity, int timeMs, int lengthMs) {
            this.midiNote = midiNote;
            this.velocity = velocity;
            this.timeMs = timeMs;
            this.lengthMs = lengthMs;
        }

        public int midiNote() {
            return midiNote;
        }

        public int velocity() {
            return velocity;
        }

        public int timeMs() {
            return timeMs;
        }

        public int lengthMs() {
            return lengthMs;
        }

        public static final class Builder {
            private final int midiNote;
            private final int velocity;
            private final int timeMs;
            private int lengthMs;

            public Builder(int midiNote, int velocity, int timeMs) {
                this.midiNote = midiNote;
                this.velocity = velocity;
                this.timeMs = timeMs;
            }

            public void lengthMs(int lengthMs) {
                this.lengthMs = lengthMs;
            }

            public int timeMs() {
                return timeMs;
            }

            public Note build() {
                return new Note(midiNote, velocity, timeMs, lengthMs);
            }
        }
    }
}
