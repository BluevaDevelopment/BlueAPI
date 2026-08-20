package net.blueva.foundation.music;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class LittleEndianInput implements Closeable {

    private static final int MAX_STRING_BYTES = 1_048_576;
    private final DataInputStream input;

    LittleEndianInput(InputStream input) {
        this.input = new DataInputStream(input);
    }

    int unsignedByte() throws IOException {
        return input.readUnsignedByte();
    }

    int unsignedShort() throws IOException {
        int low = input.readUnsignedByte();
        int high = input.readUnsignedByte();
        return low | high << 8;
    }

    short signedShort() throws IOException {
        return (short) unsignedShort();
    }

    int integer() throws IOException {
        int first = input.readUnsignedByte();
        int second = input.readUnsignedByte();
        int third = input.readUnsignedByte();
        int fourth = input.readUnsignedByte();
        return first | second << 8 | third << 16 | fourth << 24;
    }

    String string() throws IOException {
        int length = integer();
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new MusicFormatException("Invalid NBS string length: " + length);
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
