package net.blueva.foundation.materials;

import org.bukkit.Material;

import java.util.Arrays;

/** Multi-version material helpers. */
public class Materials {

    protected Materials() {
    }

    public static Material match(String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            Material material = matchOne(name);
            if (material != null) {
                return material;
            }
        }
        return null;
    }

    public static Material require(String... names) {
        Material material = match(names);
        if (material == null) {
            throw new IllegalArgumentException("Unsupported material names: " + Arrays.toString(names));
        }
        return material;
    }

    public static Material orDefault(Material fallback, String... names) {
        Material material = match(names);
        return material == null ? fallback : material;
    }

    /**
     * Whether a material is one of the air variants, on any server version.
     *
     * <p>{@code Material#isAir()} only exists from 1.13. Matching on the name instead covers
     * {@code AIR} on every version plus {@code CAVE_AIR} and {@code VOID_AIR} where they exist.</p>
     *
     * @param material the material (may be {@code null}, which counts as air)
     * @return {@code true} when the material is air
     */
    public static boolean isAir(Material material) {
        if (material == null) {
            return true;
        }
        String name = material.name();
        return "AIR".equals(name) || "CAVE_AIR".equals(name) || "VOID_AIR".equals(name);
    }

    /**
     * Whether a material can exist as an item, on any server version.
     *
     * <p>{@code Material#isItem()} is 1.13+. Before the flattening every material had an item
     * form, so a missing method means {@code true}.</p>
     *
     * @param material the material (may be {@code null})
     * @return {@code true} when the material has an item form
     */
    public static boolean isItem(Material material) {
        if (material == null) {
            return false;
        }
        try {
            Object value = Material.class.getMethod("isItem").invoke(material);
            return !(value instanceof Boolean) || (Boolean) value;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean isSupported(String... names) {
        return match(names) != null;
    }

    private static Material matchOne(String name) {
        if (isBlank(name)) {
            return null;
        }
        String normalized = name.trim().toUpperCase().replace(' ', '_').replace('-', '_');

        try {
            Material material = Material.matchMaterial(normalized);
            if (material != null) {
                return material;
            }
        } catch (Throwable ignored) {
        }

        try {
            return Material.valueOf(normalized);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
