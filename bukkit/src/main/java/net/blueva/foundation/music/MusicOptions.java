package net.blueva.foundation.music;

/**
 * Immutable options used when starting music playback.
 */
public final class MusicOptions {

    private static final double MIN_SPEED = 0.05D;
    private static final double MAX_SPEED = 20.0D;
    private static final MusicOptions DEFAULTS = new MusicOptions(1.0D, 1.0F);

    private final double speed;
    private final float volume;

    private MusicOptions(double speed, float volume) {
        this.speed = speed;
        this.volume = volume;
    }

    public static MusicOptions defaults() {
        return DEFAULTS;
    }

    public MusicOptions speed(double speed) {
        return new MusicOptions(validateSpeed(speed), volume);
    }

    public MusicOptions volume(float volume) {
        return new MusicOptions(speed, clamp(volume, 0.0F, 1.0F));
    }

    public double getSpeed() {
        return speed;
    }

    public float getVolume() {
        return volume;
    }

    static double validateSpeed(double speed) {
        if (Double.isNaN(speed) || Double.isInfinite(speed)) {
            throw new IllegalArgumentException("Music speed must be a finite number");
        }
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }

    private static float clamp(float value, float minimum, float maximum) {
        if (Float.isNaN(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
