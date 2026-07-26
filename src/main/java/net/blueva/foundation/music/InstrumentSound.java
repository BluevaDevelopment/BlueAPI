package net.blueva.foundation.music;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

final class InstrumentSound {

    private static final String[][] VANILLA_NAMES = {
            {"BLOCK_NOTE_BLOCK_HARP", "NOTE_PIANO"},
            {"BLOCK_NOTE_BLOCK_BASS", "NOTE_BASS_GUITAR"},
            {"BLOCK_NOTE_BLOCK_BASEDRUM", "NOTE_BASS_DRUM"},
            {"BLOCK_NOTE_BLOCK_SNARE", "NOTE_SNARE_DRUM"},
            {"BLOCK_NOTE_BLOCK_HAT", "NOTE_STICKS"},
            {"BLOCK_NOTE_BLOCK_GUITAR", "NOTE_BASS_GUITAR"},
            {"BLOCK_NOTE_BLOCK_FLUTE", "NOTE_PIANO"},
            {"BLOCK_NOTE_BLOCK_BELL", "NOTE_PLING"},
            {"BLOCK_NOTE_BLOCK_CHIME", "NOTE_PLING"},
            {"BLOCK_NOTE_BLOCK_XYLOPHONE", "NOTE_PLING"},
            {"BLOCK_NOTE_BLOCK_IRON_XYLOPHONE", "NOTE_PLING"},
            {"BLOCK_NOTE_BLOCK_COW_BELL", "NOTE_PLING"},
            {"BLOCK_NOTE_BLOCK_DIDGERIDOO", "NOTE_BASS_GUITAR"},
            {"BLOCK_NOTE_BLOCK_BIT", "NOTE_PLING"},
            {"BLOCK_NOTE_BLOCK_BANJO", "NOTE_PLING"},
            {"BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING"}
    };

    private final Sound bukkitSound;
    private final String customSound;

    private InstrumentSound(Sound bukkitSound, String customSound) {
        this.bukkitSound = bukkitSound;
        this.customSound = customSound;
    }

    static InstrumentSound vanilla(int instrument) {
        int index = instrument >= 0 && instrument < VANILLA_NAMES.length ? instrument : 0;
        for (String name : VANILLA_NAMES[index]) {
            try {
                return new InstrumentSound(Sound.valueOf(name), null);
            } catch (IllegalArgumentException ignored) {
                // Try the next name for the active server generation.
            }
        }
        return custom("minecraft:block.note_block.harp");
    }

    static InstrumentSound custom(String name) {
        if (name == null) {
            return vanilla(0);
        }
        String normalized = name.trim();
        if (normalized.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.isEmpty()) {
            return vanilla(0);
        }
        return new InstrumentSound(null, normalized);
    }

    void play(Player player, float volume, float pitch, float pan) {
        Location location = player.getEyeLocation();
        if (pan != 0.0F) {
            double yaw = Math.toRadians(location.getYaw());
            double distance = Math.max(-1.0D, Math.min(1.0D, pan)) * 1.5D;
            location = location.clone().add(Math.cos(yaw) * distance, 0.0D, Math.sin(yaw) * distance);
        }

        if (bukkitSound != null) {
            player.playSound(location, bukkitSound, volume, pitch);
        } else {
            player.playSound(location, customSound, volume, pitch);
        }
    }
}
