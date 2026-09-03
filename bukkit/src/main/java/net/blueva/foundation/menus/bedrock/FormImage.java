package net.blueva.foundation.menus.bedrock;

/**
 * The icon shown next to a Bedrock button.
 *
 * <p>Either a URL the client downloads, or a path into the client's own
 * resources ({@code textures/items/diamond_sword} and friends).</p>
 */
public final class FormImage {

    /** Where the client should look for the image. */
    public enum Type {
        /** An absolute {@code http(s)} URL. */
        URL("url"),
        /** A path inside the client's resource packs. */
        PATH("path");

        private final String jsonName;

        Type(String jsonName) {
            this.jsonName = jsonName;
        }

        /**
         * @return the value of the image's {@code "type"} JSON property
         */
        public String jsonName() {
            return jsonName;
        }
    }

    private final Type type;
    private final String data;

    private FormImage(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    /**
     * @param url an absolute image URL
     * @return an image the client downloads, or {@code null} if the URL is blank
     */
    public static FormImage url(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        return new FormImage(Type.URL, url.trim());
    }

    /**
     * @param path a path into the client's resources
     * @return an image the client resolves locally, or {@code null} if the path is blank
     */
    public static FormImage path(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return new FormImage(Type.PATH, path.trim());
    }

    /**
     * Guess the kind from the value itself, so config files can carry a single
     * {@code image:} key without also asking the author for its type.
     *
     * @param value a URL or a resource path
     * @return the image, or {@code null} if the value is blank
     */
    public static FormImage of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return url(trimmed);
        }
        return path(trimmed);
    }

    /**
     * @return whether the client downloads this image or reads it locally
     */
    public Type type() {
        return type;
    }

    /**
     * @return the URL or resource path
     */
    public String data() {
        return data;
    }

    /**
     * @return this image as a JSON object
     */
    public String toJson() {
        return "{\"type\":" + Json.quote(type.jsonName()) + ",\"data\":" + Json.quote(data) + "}";
    }
}
