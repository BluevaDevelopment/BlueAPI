package net.blueva.foundation.menus.bedrock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The smallest JSON writer and reader that the Bedrock form protocol needs.
 *
 * <p>BlueFoundation ships its own rather than reaching for Gson because the
 * only Gson guaranteed to be on a 1.8.8 server is relocated
 * ({@code org.bukkit.craftbukkit.libs.com.google.gson}), and a form layer that
 * works on some versions and silently does not on others is worse than a
 * hundred lines of parser. The form protocol is flat: strings, numbers,
 * booleans, arrays and one level of object.</p>
 */
public final class Json {

    private Json() {
    }

    /**
     * Escape and quote a string for embedding in JSON.
     *
     * @param value the raw text, may be {@code null}
     * @return a quoted JSON string, {@code "null"} when the value is null
     */
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    // Control characters must be escaped; § and friends are fine raw as UTF-8.
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
        return out.toString();
    }

    /**
     * Render a float without a trailing {@code .0} when it is a whole number,
     * which is what the Bedrock client expects for slider bounds.
     *
     * @param value the number
     * @return its JSON representation
     */
    public static String number(float value) {
        if (value == Math.rint(value) && !Float.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    /**
     * Parse a JSON document into plain Java values: {@link Map}, {@link List},
     * {@link String}, {@link Double}, {@link Boolean} or {@code null}.
     *
     * @param json the document
     * @return the parsed value
     * @throws IllegalArgumentException if the document is malformed
     */
    public static Object parse(String json) {
        if (json == null) {
            return null;
        }
        Reader reader = new Reader(json);
        reader.skipWhitespace();
        Object value = reader.readValue();
        reader.skipWhitespace();
        if (!reader.done()) {
            throw new IllegalArgumentException("Trailing data at index " + reader.index);
        }
        return value;
    }

    private static final class Reader {
        private final String source;
        private int index;

        Reader(String source) {
            this.source = source;
        }

        boolean done() {
            return index >= source.length();
        }

        void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        Object readValue() {
            if (done()) {
                throw new IllegalArgumentException("Unexpected end of input");
            }
            char c = source.charAt(index);
            switch (c) {
                case '{':
                    return readObject();
                case '[':
                    return readArray();
                case '"':
                    return readString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    return readNumber();
            }
        }

        private void expect(String literal) {
            if (!source.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected " + literal + " at index " + index);
            }
            index += literal.length();
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            index++; // {
            skipWhitespace();
            if (!done() && source.charAt(index) == '}') {
                index++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                if (done() || source.charAt(index) != ':') {
                    throw new IllegalArgumentException("Expected ':' at index " + index);
                }
                index++;
                skipWhitespace();
                map.put(key, readValue());
                skipWhitespace();
                if (done()) {
                    throw new IllegalArgumentException("Unterminated object");
                }
                char c = source.charAt(index++);
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at index " + (index - 1));
                }
            }
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<Object>();
            index++; // [
            skipWhitespace();
            if (!done() && source.charAt(index) == ']') {
                index++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(readValue());
                skipWhitespace();
                if (done()) {
                    throw new IllegalArgumentException("Unterminated array");
                }
                char c = source.charAt(index++);
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or ']' at index " + (index - 1));
                }
            }
        }

        private String readString() {
            if (done() || source.charAt(index) != '"') {
                throw new IllegalArgumentException("Expected string at index " + index);
            }
            index++;
            StringBuilder out = new StringBuilder();
            while (true) {
                if (done()) {
                    throw new IllegalArgumentException("Unterminated string");
                }
                char c = source.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (done()) {
                    throw new IllegalArgumentException("Unterminated escape");
                }
                char escape = source.charAt(index++);
                switch (escape) {
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case '/':
                        out.append('/');
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'u':
                        if (index + 4 > source.length()) {
                            throw new IllegalArgumentException("Truncated unicode escape");
                        }
                        out.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                        index += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown escape \\" + escape);
                }
            }
        }

        private Double readNumber() {
            int start = index;
            while (index < source.length() && "+-.eE0123456789".indexOf(source.charAt(index)) >= 0) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("Expected value at index " + index);
            }
            try {
                return Double.valueOf(source.substring(start, index));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Malformed number at index " + start, e);
            }
        }
    }
}
