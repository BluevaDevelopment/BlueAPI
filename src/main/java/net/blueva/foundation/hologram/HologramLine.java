package net.blueva.foundation.hologram;

/** One physical fake {@code TextDisplay} entity - a single line of a {@link Hologram}. */
final class HologramLine {

    final Object entityHandle;
    final int entityId;
    final String text;

    HologramLine(Object entityHandle, int entityId, String text) {
        this.entityHandle = entityHandle;
        this.entityId = entityId;
        this.text = text;
    }
}
