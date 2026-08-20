package net.blueva.foundation.music;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Parses a standard MIDI file into a {@link Melody}, using the JDK's own
 * {@code javax.sound.midi} rather than a hand-rolled byte parser. Notes are
 * normalized after parsing: shifted so the first note starts at time 0, and
 * velocity is scaled relative to the song's average.
 */
public final class MidiParser {

    private MidiParser() {
    }

    public static Melody parse(String name, InputStream inputStream) throws IOException {
        javax.sound.midi.Sequence sequence;
        try {
            sequence = MidiSystem.getSequence(inputStream);
        } catch (InvalidMidiDataException e) {
            throw new IOException("Invalid MIDI data: " + e.getMessage(), e);
        }

        // Collect tempo-change events shared across all tracks.
        List<MidiEvent> sharedEvents = new LinkedList<>();
        for (Track track : sequence.getTracks()) {
            for (MidiEvent event : listEvents(track)) {
                if (event.getMessage() instanceof MetaMessage && ((MetaMessage) event.getMessage()).getType() == 0x51) {
                    sharedEvents.add(event);
                }
            }
        }

        List<Melody.Track> melodyTracks = new ArrayList<>();
        int trackNr = 1;

        for (Track track : sequence.getTracks()) {
            List<MidiEvent> events = listEvents(track);
            events.addAll(0, sharedEvents);
            events.sort((a, b) -> Long.compare(a.getTick(), b.getTick()));

            double bpm = 120.0;
            long lastTick = 0;
            double timeMs = 0.0;
            String trackName = "Track " + trackNr;
            List<Melody.Note> notes = new LinkedList<>();
            Map<Integer, Melody.Note.Builder> currentNotes = new HashMap<>();

            for (MidiEvent event : events) {
                long tick = event.getTick();
                double deltaMs = (tick - lastTick) * 60000.0 / (sequence.getResolution() * bpm);
                timeMs += deltaMs;
                lastTick = tick;
                int ms = (int) timeMs;

                javax.sound.midi.MidiMessage message = event.getMessage();

                if (message instanceof MetaMessage) {
                    MetaMessage meta = (MetaMessage) message;
                    byte[] data = meta.getData();
                    int type = meta.getType();
                    if (type == 0x03) {
                        String newName = new String(data).trim();
                        if (!newName.isEmpty()) {
                            trackName = newName;
                        }
                    } else if (type == 0x51 && data.length >= 3) {
                        int microsecondsPerBeat = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                        bpm = Math.round(60000000.0 / microsecondsPerBeat);
                    }
                }

                if (message instanceof ShortMessage) {
                    ShortMessage shortMessage = (ShortMessage) message;
                    int command = shortMessage.getCommand();
                    // NOTE_ON with velocity 0 is an alias for NOTE_OFF.
                    if (command == ShortMessage.NOTE_ON && shortMessage.getData2() == 0) {
                        command = ShortMessage.NOTE_OFF;
                    }
                    if (command == ShortMessage.NOTE_ON) {
                        currentNotes.put(shortMessage.getData1(), new Melody.Note.Builder(shortMessage.getData1(), shortMessage.getData2(), ms));
                    } else if (command == ShortMessage.NOTE_OFF) {
                        Melody.Note.Builder builder = currentNotes.remove(shortMessage.getData1());
                        if (builder != null) {
                            builder.lengthMs(ms - builder.timeMs());
                            notes.add(builder.build());
                        }
                    }
                }
            }

            if (!notes.isEmpty()) {
                notes.sort((a, b) -> Integer.compare(a.timeMs(), b.timeMs()));
                melodyTracks.add(new Melody.Track(trackName, notes));
                trackNr++;
            }
        }

        // Normalize: shift all notes so the first note starts at time 0, and
        // scale velocity relative to the song's average.
        if (!melodyTracks.isEmpty()) {
            int offset = Integer.MAX_VALUE;
            for (Melody.Track t : melodyTracks) {
                if (!t.notes().isEmpty()) {
                    offset = Math.min(offset, t.notes().get(0).timeMs());
                }
            }
            long totalVelocity = 0;
            int totalNotes = 0;
            for (Melody.Track t : melodyTracks) {
                for (Melody.Note n : t.notes()) {
                    totalVelocity += n.velocity();
                    totalNotes++;
                }
            }
            float avgVelocity = totalNotes > 0 ? (float) totalVelocity / totalNotes : 64f;

            List<Melody.Track> normalized = new ArrayList<>();
            for (Melody.Track t : melodyTracks) {
                List<Melody.Note> newNotes = new ArrayList<>();
                for (Melody.Note n : t.notes()) {
                    int scaledVelocity = Math.round(n.velocity() / avgVelocity * 64);
                    newNotes.add(new Melody.Note(n.midiNote(), scaledVelocity, n.timeMs() - offset, n.lengthMs()));
                }
                normalized.add(new Melody.Track(t.name(), newNotes));
            }
            melodyTracks = normalized;
        }

        return new Melody(name, melodyTracks);
    }

    private static List<MidiEvent> listEvents(Track track) {
        List<MidiEvent> events = new ArrayList<>(track.size());
        for (int i = 0; i < track.size(); i++) {
            events.add(track.get(i));
        }
        return events;
    }
}
