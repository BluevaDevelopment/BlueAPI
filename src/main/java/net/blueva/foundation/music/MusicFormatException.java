package net.blueva.foundation.music;

import java.io.IOException;

/**
 * Indicates that a music file is unsupported or malformed.
 */
public final class MusicFormatException extends IOException {

    public MusicFormatException(String message) {
        super(message);
    }

    public MusicFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
