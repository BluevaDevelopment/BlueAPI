package net.blueva.foundation.music;

/**
 * Controls one player's current music track.
 */
public interface MusicPlayback {

    boolean isPlaying();

    boolean isPaused();

    long getDurationMillis();

    double getSpeed();

    void setSpeed(double speed);

    void pause();

    void resume();

    void stop();
}
