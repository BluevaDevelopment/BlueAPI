package net.blueva.foundation;

/**
 * Build-time constants filtered from the Gradle project version.
 *
 * <p>Baking the version into a compile-time constant keeps {@link BlueFoundation#version()}
 * accurate when BlueFoundation is shaded into a consumer jar, where the manifest and the
 * Gradle module metadata belong to that jar (or are stripped from it) instead.</p>
 */
final class BuildInfo {

    static final String VERSION = "${version}";

    private BuildInfo() {
    }
}
