package net.blueva.foundation.music;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class MidiTrackLoader {

    private static final int DEFAULT_TEMPO_MICROSECONDS = 500_000;
    private static final int PERCUSSION_CHANNEL = 9;

    MusicTrack load(File file) throws IOException {
        final Sequence sequence;
        try {
            sequence = MidiSystem.getSequence(file);
        } catch (InvalidMidiDataException exception) {
            throw new MusicFormatException("Invalid MIDI file " + file.getName(), exception);
        }

        List<MidiEvent> events = mergeEvents(sequence);
        TempoTimeline timeline = TempoTimeline.from(sequence, events);
        int[] programs = new int[16];
        List<MusicNote> notes = new ArrayList<>();
        long lastEventMillis = 0L;

        for (MidiEvent event : events) {
            long timeMillis = timeline.toMillis(event.getTick());
            lastEventMillis = Math.max(lastEventMillis, timeMillis);
            MidiMessage message = event.getMessage();
            if (!(message instanceof ShortMessage)) {
                continue;
            }

            ShortMessage shortMessage = (ShortMessage) message;
            int channel = shortMessage.getChannel();
            if (shortMessage.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                programs[channel] = shortMessage.getData1();
                continue;
            }
            if (shortMessage.getCommand() != ShortMessage.NOTE_ON || shortMessage.getData2() <= 0) {
                continue;
            }

            int note = shortMessage.getData1();
            int velocity = shortMessage.getData2();
            int instrument = channel == PERCUSSION_CHANNEL
                    ? percussionInstrument(note)
                    : programInstrument(programs[channel]);
            float pitch = normalizePitch((float) Math.pow(2.0D, (note - 66.0D) / 12.0D));
            notes.add(new MusicNote(timeMillis, InstrumentSound.vanilla(instrument), pitch,
                    velocity / 127.0F, 0.0F));
        }

        Collections.sort(notes);
        long sequenceDuration = Math.max(lastEventMillis, sequence.getMicrosecondLength() / 1_000L);
        return new MusicTrack(notes, sequenceDuration);
    }

    private List<MidiEvent> mergeEvents(Sequence sequence) {
        List<MidiEvent> events = new ArrayList<>();
        for (Track track : sequence.getTracks()) {
            for (int index = 0; index < track.size(); index++) {
                events.add(track.get(index));
            }
        }
        events.sort(Comparator.comparingLong(MidiEvent::getTick));
        return events;
    }

    private int programInstrument(int program) {
        int family = Math.max(0, Math.min(127, program)) / 8;
        switch (family) {
            case 0:
            case 1:
                return 0; // Piano and chromatic percussion
            case 2:
                return 7; // Organ
            case 3:
                return 5; // Guitar
            case 4:
                return 1; // Bass
            case 5:
                return 14; // Strings
            case 6:
            case 7:
                return 6; // Ensemble and brass
            case 8:
                return 0; // Reed
            case 9:
                return 6; // Pipe
            case 10:
                return 9; // Synth lead
            case 11:
                return 8; // Synth pad
            case 12:
                return 13; // Synth effects
            case 13:
                return 10; // Ethnic
            case 14:
                return 4; // Percussive
            default:
                return 15; // Sound effects
        }
    }

    private int percussionInstrument(int midiNote) {
        if (midiNote == 35 || midiNote == 36) {
            return 2;
        }
        if (midiNote == 37 || midiNote == 38 || midiNote == 39 || midiNote == 40) {
            return 3;
        }
        if (midiNote >= 42 && midiNote <= 46) {
            return 4;
        }
        if (midiNote >= 49 && midiNote <= 57) {
            return 15;
        }
        return 4;
    }

    private static float normalizePitch(float pitch) {
        while (pitch < 0.5F) {
            pitch *= 2.0F;
        }
        while (pitch > 2.0F) {
            pitch /= 2.0F;
        }
        return pitch;
    }

    private static final class TempoTimeline {
        private final float divisionType;
        private final int resolution;
        private final long[] ticks;
        private final long[] elapsedMicros;
        private final int[] tempos;

        private TempoTimeline(float divisionType, int resolution, long[] ticks,
                              long[] elapsedMicros, int[] tempos) {
            this.divisionType = divisionType;
            this.resolution = resolution;
            this.ticks = ticks;
            this.elapsedMicros = elapsedMicros;
            this.tempos = tempos;
        }

        static TempoTimeline from(Sequence sequence, List<MidiEvent> events) {
            if (sequence.getDivisionType() != Sequence.PPQ) {
                return new TempoTimeline(sequence.getDivisionType(), sequence.getResolution(),
                        new long[0], new long[0], new int[0]);
            }

            List<TempoChange> changes = new ArrayList<>();
            changes.add(new TempoChange(0L, DEFAULT_TEMPO_MICROSECONDS));
            for (MidiEvent event : events) {
                MidiMessage message = event.getMessage();
                if (!(message instanceof MetaMessage)) {
                    continue;
                }
                MetaMessage meta = (MetaMessage) message;
                if (meta.getType() != 0x51 || meta.getData().length != 3) {
                    continue;
                }
                byte[] data = meta.getData();
                int tempo = (data[0] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | data[2] & 0xFF;
                if (tempo > 0) {
                    changes.add(new TempoChange(event.getTick(), tempo));
                }
            }
            changes.sort(Comparator.comparingLong(change -> change.tick));

            List<TempoChange> compact = new ArrayList<>();
            for (TempoChange change : changes) {
                if (!compact.isEmpty() && compact.get(compact.size() - 1).tick == change.tick) {
                    compact.set(compact.size() - 1, change);
                } else {
                    compact.add(change);
                }
            }

            long[] ticks = new long[compact.size()];
            long[] elapsed = new long[compact.size()];
            int[] tempos = new int[compact.size()];
            long elapsedMicros = 0L;
            for (int index = 0; index < compact.size(); index++) {
                TempoChange change = compact.get(index);
                if (index > 0) {
                    long delta = change.tick - ticks[index - 1];
                    elapsedMicros += delta * tempos[index - 1] / sequence.getResolution();
                }
                ticks[index] = change.tick;
                elapsed[index] = elapsedMicros;
                tempos[index] = change.tempo;
            }
            return new TempoTimeline(sequence.getDivisionType(), sequence.getResolution(),
                    ticks, elapsed, tempos);
        }

        long toMillis(long tick) {
            if (divisionType != Sequence.PPQ) {
                double framesPerSecond = Math.abs(divisionType);
                return Math.round(tick * 1_000.0D / (framesPerSecond * resolution));
            }
            int index = Arrays.binarySearch(ticks, tick);
            if (index < 0) {
                index = -index - 2;
            }
            index = Math.max(0, index);
            long micros = elapsedMicros[index]
                    + (tick - ticks[index]) * tempos[index] / resolution;
            return micros / 1_000L;
        }
    }

    private static final class TempoChange {
        private final long tick;
        private final int tempo;

        private TempoChange(long tick, int tempo) {
            this.tick = tick;
            this.tempo = tempo;
        }
    }
}
