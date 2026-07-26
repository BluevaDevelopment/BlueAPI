package net.blueva.foundation.music;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NbsTrackLoader {

    private static final int MAX_NOTES = 2_000_000;

    MusicTrack load(File file) throws IOException {
        try (LittleEndianInput input = new LittleEndianInput(
                new BufferedInputStream(new FileInputStream(file)))) {
            int declaredLength = input.unsignedShort();
            int version = 0;
            int vanillaInstrumentCount = 10;
            if (declaredLength == 0) {
                version = input.unsignedByte();
                vanillaInstrumentCount = input.unsignedByte();
                if (version >= 3) {
                    declaredLength = input.unsignedShort();
                }
            }

            int layerCount = input.unsignedShort();
            skipHeader(input, version);
            int tempoHundredths = input.unsignedShort();
            if (tempoHundredths <= 0) {
                throw new MusicFormatException("NBS tempo must be greater than zero");
            }
            NbsLoop loop = readStatistics(input, version);

            List<RawNbsNote> rawNotes = readNotes(input, version);
            List<NbsLayer> layers = readLayers(input, version, layerCount);
            List<NbsInstrument> customInstruments = readCustomInstruments(input);
            boolean hasSoloLayer = hasSoloLayer(layers);

            double initialTempo = tempoHundredths / 100.0D;
            TempoTimeline timeline = TempoTimeline.create(
                    rawNotes, layers, hasSoloLayer, customInstruments,
                    vanillaInstrumentCount, initialTempo);
            List<MusicNote> notes = new ArrayList<>(rawNotes.size());
            int highestTick = Math.max(0, declaredLength);
            for (RawNbsNote raw : rawNotes) {
                highestTick = Math.max(highestTick, raw.tick);
                NbsLayer layer = raw.layer < layers.size() ? layers.get(raw.layer) : NbsLayer.DEFAULT;
                if (!layer.audible(hasSoloLayer)) {
                    continue;
                }
                InstrumentSound sound;
                int baseKey = 45;
                if (raw.instrument < vanillaInstrumentCount) {
                    sound = InstrumentSound.vanilla(raw.instrument);
                } else {
                    int customIndex = raw.instrument - vanillaInstrumentCount;
                    NbsInstrument custom = customIndex >= 0 && customIndex < customInstruments.size()
                            ? customInstruments.get(customIndex)
                            : null;
                    if (custom != null && custom.tempoChanger) {
                        continue;
                    }
                    sound = custom == null ? InstrumentSound.vanilla(0) : custom.sound;
                    if (custom != null) {
                        baseKey = custom.baseKey;
                    }
                }

                double semitones = raw.key + baseKey - 90.0D + raw.finePitch / 100.0D;
                float pitch = normalizePitch((float) Math.pow(2.0D, semitones / 12.0D));
                float volume = clamp(raw.velocity / 100.0F * layer.volume / 100.0F, 0.0F, 1.0F);
                float effectivePan = layer.pan == 100
                        ? raw.pan
                        : (raw.pan + layer.pan) / 2.0F;
                float pan = clamp((effectivePan - 100.0F) / 100.0F, -1.0F, 1.0F);
                notes.add(new MusicNote(
                        timeline.timeAtTick(raw.tick), sound, pitch, volume, pan));
            }
            Collections.sort(notes);
            return new MusicTrack(notes, timeline.timeAtTick(highestTick + 1L),
                    loop.enabled, timeline.timeAtTick(loop.startTick), loop.maximumLoops);
        } catch (MusicFormatException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new MusicFormatException("Unable to read NBS file " + file.getName(), exception);
        }
    }

    private void skipHeader(LittleEndianInput input, int version) throws IOException {
        input.string(); // title
        input.string(); // author
        input.string(); // original author
        input.string(); // description
    }

    private NbsLoop readStatistics(LittleEndianInput input, int version) throws IOException {
        input.unsignedByte(); // auto-save
        input.unsignedByte(); // auto-save duration
        input.unsignedByte(); // time signature
        input.integer(); // minutes spent
        input.integer(); // left clicks
        input.integer(); // right clicks
        input.integer(); // blocks added
        input.integer(); // blocks removed
        input.string(); // imported file
        if (version >= 4) {
            boolean enabled = input.unsignedByte() != 0;
            int maximumLoops = input.unsignedByte();
            int startTick = input.unsignedShort();
            return new NbsLoop(enabled, maximumLoops, startTick);
        }
        return NbsLoop.DISABLED;
    }

    private List<RawNbsNote> readNotes(LittleEndianInput input, int version) throws IOException {
        List<RawNbsNote> notes = new ArrayList<>();
        int tick = -1;
        while (true) {
            int tickJump = input.unsignedShort();
            if (tickJump == 0) {
                break;
            }
            tick += tickJump;
            int layer = -1;
            while (true) {
                int layerJump = input.unsignedShort();
                if (layerJump == 0) {
                    break;
                }
                layer += layerJump;
                int instrument = input.unsignedByte();
                int key = Math.min(87, input.unsignedByte());
                int velocity = version >= 4 ? input.unsignedByte() : 100;
                int pan = version >= 4 ? input.unsignedByte() : 100;
                int finePitch = version >= 4 ? input.signedShort() : 0;
                notes.add(new RawNbsNote(tick, layer, instrument, key, velocity, pan, finePitch));
                if (notes.size() > MAX_NOTES) {
                    throw new MusicFormatException("NBS file contains too many notes");
                }
            }
        }
        return notes;
    }

    private List<NbsLayer> readLayers(LittleEndianInput input, int version, int layerCount)
            throws IOException {
        List<NbsLayer> layers = new ArrayList<>(layerCount);
        for (int index = 0; index < layerCount; index++) {
            input.string();
            int lock = 0;
            if (version >= 4) {
                lock = input.unsignedByte();
            }
            int volume = input.unsignedByte();
            int pan = version >= 2 ? input.unsignedByte() : 100;
            layers.add(new NbsLayer(volume, pan, lock));
        }
        return layers;
    }

    private boolean hasSoloLayer(List<NbsLayer> layers) {
        for (NbsLayer layer : layers) {
            if (layer.lock == 2) {
                return true;
            }
        }
        return false;
    }

    private List<NbsInstrument> readCustomInstruments(LittleEndianInput input) throws IOException {
        int count = input.unsignedByte();
        List<NbsInstrument> instruments = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String name = input.string();
            String sound = input.string();
            int baseKey = input.unsignedByte();
            input.unsignedByte();
            instruments.add(new NbsInstrument(
                    InstrumentSound.custom(sound), baseKey,
                    "Tempo Changer".equalsIgnoreCase(name.trim())));
        }
        return instruments;
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

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class RawNbsNote {
        private final int tick;
        private final int layer;
        private final int instrument;
        private final int key;
        private final int velocity;
        private final int pan;
        private final int finePitch;

        private RawNbsNote(int tick, int layer, int instrument, int key,
                           int velocity, int pan, int finePitch) {
            this.tick = tick;
            this.layer = layer;
            this.instrument = instrument;
            this.key = key;
            this.velocity = velocity;
            this.pan = pan;
            this.finePitch = finePitch;
        }
    }

    private static final class NbsLayer {
        private static final NbsLayer DEFAULT = new NbsLayer(100, 100, 0);
        private final int volume;
        private final int pan;
        private final int lock;

        private NbsLayer(int volume, int pan, int lock) {
            this.volume = volume;
            this.pan = pan;
            this.lock = lock;
        }

        private boolean audible(boolean hasSoloLayer) {
            return lock != 1 && (!hasSoloLayer || lock == 2);
        }
    }

    private static final class NbsInstrument {
        private final InstrumentSound sound;
        private final int baseKey;
        private final boolean tempoChanger;

        private NbsInstrument(InstrumentSound sound, int baseKey, boolean tempoChanger) {
            this.sound = sound;
            this.baseKey = baseKey;
            this.tempoChanger = tempoChanger;
        }
    }

    private static final class NbsLoop {
        private static final NbsLoop DISABLED = new NbsLoop(false, 0, 0);

        private final boolean enabled;
        private final int maximumLoops;
        private final int startTick;

        private NbsLoop(boolean enabled, int maximumLoops, int startTick) {
            this.enabled = enabled;
            this.maximumLoops = maximumLoops;
            this.startTick = startTick;
        }
    }

    private static final class TempoTimeline {
        private static final double MINIMUM_TEMPO = 0.25D;

        private final List<TempoChange> changes;
        private final double initialTempo;

        private TempoTimeline(List<TempoChange> changes, double initialTempo) {
            this.changes = changes;
            this.initialTempo = initialTempo;
        }

        static TempoTimeline create(List<RawNbsNote> notes,
                                    List<NbsLayer> layers,
                                    boolean hasSoloLayer,
                                    List<NbsInstrument> customInstruments,
                                    int vanillaInstrumentCount,
                                    double initialTempo) {
            List<TempoChange> changes = new ArrayList<>();
            for (RawNbsNote note : notes) {
                NbsLayer layer = note.layer < layers.size()
                        ? layers.get(note.layer)
                        : NbsLayer.DEFAULT;
                if (!layer.audible(hasSoloLayer)) {
                    continue;
                }
                int customIndex = note.instrument - vanillaInstrumentCount;
                if (customIndex < 0 || customIndex >= customInstruments.size()
                        || !customInstruments.get(customIndex).tempoChanger) {
                    continue;
                }
                double tempo = Math.max(MINIMUM_TEMPO,
                        Math.floor(Math.abs(note.finePitch)) / 15.0D);
                if (!changes.isEmpty() && changes.get(changes.size() - 1).tick == note.tick) {
                    changes.set(changes.size() - 1, new TempoChange(note.tick, tempo));
                } else {
                    changes.add(new TempoChange(note.tick, tempo));
                }
            }
            return new TempoTimeline(changes, Math.max(MINIMUM_TEMPO, initialTempo));
        }

        long timeAtTick(long targetTick) {
            double elapsedMillis = 0.0D;
            long segmentStartTick = 0L;
            double tempo = initialTempo;
            for (TempoChange change : changes) {
                if (change.tick >= targetTick) {
                    break;
                }
                elapsedMillis += (change.tick - segmentStartTick) * 1_000.0D / tempo;
                segmentStartTick = change.tick;
                tempo = change.tempo;
            }
            elapsedMillis += (targetTick - segmentStartTick) * 1_000.0D / tempo;
            return Math.round(elapsedMillis);
        }
    }

    private static final class TempoChange {
        private final int tick;
        private final double tempo;

        private TempoChange(int tick, double tempo) {
            this.tick = tick;
            this.tempo = tempo;
        }
    }
}
