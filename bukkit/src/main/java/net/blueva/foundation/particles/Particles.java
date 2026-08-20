package net.blueva.foundation.particles;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Multi-version particle helpers.
 *
 * <p>Particles changed shape three times: 1.8 has no {@code Particle} type at all and goes through
 * {@code World.Spigot#playEffect}, 1.9 introduced {@code World#spawnParticle}, and 1.13 moved
 * coloured dust from an offset hack to a {@code Particle.DustOptions} data object (renaming
 * {@code REDSTONE} to {@code DUST} in 1.20.5). Callers pass particle names instead of constants and
 * get a no-op {@code false} rather than an exception when a particle does not exist on the running
 * server.</p>
 */
public class Particles {

    private static volatile Class<?> particleClass;
    private static volatile Method spawnParticle;      // World#spawnParticle(Particle, Location, int, dx, dy, dz, extra) (1.9+)
    private static volatile Method spawnParticleData;  // ... plus a data argument (1.13+)
    private static volatile Constructor<?> dustOptions; // Particle.DustOptions(Color, float) (1.13+)
    private static volatile Method playerSpawnParticle; // Player#spawnParticle(Particle, Location, int, dx, dy, dz, extra) (1.9+)
    private static volatile Method spigotPlayEffect;   // World.Spigot#playEffect(...) (1.8)
    private static volatile Method worldSpigot;        // World#spigot() (1.8)
    private static volatile boolean resolved;

    protected Particles() {
    }

    /**
     * Returns whether the running server knows any of the given particle names.
     *
     * @param names candidate names, tried in order, e.g. {@code "DUST", "REDSTONE"}
     * @return {@code true} when one of them resolves
     */
    public static boolean isSupported(String... names) {
        return match(names) != null;
    }

    /**
     * Resolves a particle constant by name, returned as an opaque {@code Object} because the type
     * does not exist on the 1.8 API this library compiles against.
     *
     * @param names candidate names, tried in order
     * @return the particle constant, or {@code null} when none resolve
     */
    public static Object match(String... names) {
        if (names == null) {
            return null;
        }
        if (!resolved) {
            resolve();
        }
        if (particleClass == null) {
            return null;
        }
        for (String name : names) {
            if (isBlank(name)) {
                continue;
            }
            try {
                Object particle = particleClass.getField(normalize(name)).get(null);
                if (particle != null) {
                    return particle;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Spawns a particle on any server version.
     *
     * @param location where to spawn (may be {@code null})
     * @param count    how many particles
     * @param offsetX  x spread
     * @param offsetY  y spread
     * @param offsetZ  z spread
     * @param extra    particle speed / extra data
     * @param names    candidate particle names, tried in order
     * @return {@code true} when the particle was spawned
     */
    public static boolean spawn(Location location, int count,
                                double offsetX, double offsetY, double offsetZ,
                                double extra, String... names) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!resolved) {
            resolve();
        }

        Object particle = match(names);
        if (particle != null && spawnParticle != null) {
            try {
                spawnParticle.invoke(location.getWorld(), particle, location, count,
                        offsetX, offsetY, offsetZ, extra);
                return true;
            } catch (Throwable ignored) {
                // fall through to the legacy path
            }
        }
        return spawnLegacy(location, count, offsetX, offsetY, offsetZ, extra, names);
    }

    /**
     * Spawns a particle visible only to one player, on any server version.
     *
     * <p>{@code Player#spawnParticle} arrived with 1.9. On 1.8 there is no per-player particle in
     * the Bukkit API, so this reports {@code false} rather than falling back to a world-wide
     * spawn, which every nearby player would see.</p>
     *
     * @param player   the player to show it to (may be {@code null})
     * @param location where to spawn (may be {@code null})
     * @param count    how many particles
     * @param offsetX  x spread
     * @param offsetY  y spread
     * @param offsetZ  z spread
     * @param extra    particle speed / extra data
     * @param names    candidate particle names, tried in order
     * @return {@code true} when the particle was shown
     */
    public static boolean spawnFor(Player player, Location location, int count,
                                   double offsetX, double offsetY, double offsetZ,
                                   double extra, String... names) {
        if (player == null || location == null) {
            return false;
        }
        if (!resolved) {
            resolve();
        }
        Object particle = match(names);
        if (particle == null || playerSpawnParticle == null) {
            return false;
        }
        try {
            playerSpawnParticle.invoke(player, particle, location, count,
                    offsetX, offsetY, offsetZ, extra);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Spawns coloured dust on any server version, hiding the three different colouring mechanisms.
     *
     * <p>1.13+ passes a {@code DustOptions}; 1.9-1.12 uses the classic {@code REDSTONE} hack where
     * the offsets carry the RGB channels and the count must be zero; 1.8 falls back to
     * {@code Effect.COLOURED_DUST} through the same offset convention.</p>
     *
     * @param location where to spawn (may be {@code null})
     * @param red      red channel, 0-255
     * @param green    green channel, 0-255
     * @param blue     blue channel, 0-255
     * @param size     dust size, only honoured on 1.13+
     * @param count    how many particles, only honoured on 1.13+
     * @return {@code true} when the dust was spawned
     */
    public static boolean spawnColored(Location location, int red, int green, int blue,
                                       float size, int count) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!resolved) {
            resolve();
        }

        int r = clampColor(red);
        int g = clampColor(green);
        int b = clampColor(blue);

        Object particle = match("DUST", "REDSTONE");

        // 1.13+: the colour travels in a DustOptions data object.
        if (particle != null && dustOptions != null && spawnParticleData != null) {
            try {
                Object color = org.bukkit.Color.fromRGB(r, g, b);
                Object options = dustOptions.newInstance(color, size <= 0.0F ? 1.0F : size);
                spawnParticleData.invoke(location.getWorld(), particle, location,
                        Math.max(count, 1), 0.0D, 0.0D, 0.0D, 0.0D, options);
                return true;
            } catch (Throwable ignored) {
            }
        }

        // 1.9-1.12: count must be 0 and the offsets carry the colour, with a minimum of 1/255 on
        // red because an exact 0 there is interpreted as full red by the client.
        if (particle != null && spawnParticle != null) {
            try {
                spawnParticle.invoke(location.getWorld(), particle, location, 0,
                        r == 0 ? 0.001D : r / 255.0D, g / 255.0D, b / 255.0D, 1.0D);
                return true;
            } catch (Throwable ignored) {
            }
        }

        // 1.8: same offset convention, through the Spigot effect API.
        return spawnLegacyEffect(location, Effect.COLOURED_DUST, 0,
                (float) (r == 0 ? 0.001D : r / 255.0D), g / 255.0F, b / 255.0F, 1.0F, 1);
    }

    /**
     * 1.8 path: map the particle name onto the {@code Effect} enum and go through
     * {@code World.Spigot#playEffect}, which is the only way to pass offsets on that version.
     */
    private static boolean spawnLegacy(Location location, int count,
                                       double offsetX, double offsetY, double offsetZ,
                                       double extra, String... names) {
        if (names == null) {
            return false;
        }
        for (String name : names) {
            if (isBlank(name)) {
                continue;
            }
            Effect effect;
            try {
                effect = Effect.valueOf(normalize(name));
            } catch (Throwable ignored) {
                continue;
            }
            if (spawnLegacyEffect(location, effect, 0,
                    (float) offsetX, (float) offsetY, (float) offsetZ, (float) extra, count)) {
                return true;
            }
        }
        return false;
    }

    private static boolean spawnLegacyEffect(Location location, Effect effect, int data,
                                             float offsetX, float offsetY, float offsetZ,
                                             float speed, int count) {
        if (location == null || effect == null || worldSpigot == null || spigotPlayEffect == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        try {
            Object spigot = worldSpigot.invoke(world);
            spigotPlayEffect.invoke(spigot, location, effect, 0, data,
                    offsetX, offsetY, offsetZ, speed, Math.max(count, 1), 64);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void resolve() {
        synchronized (Particles.class) {
            if (resolved) {
                return;
            }
            try {
                particleClass = Class.forName("org.bukkit.Particle");
                spawnParticle = World.class.getMethod("spawnParticle", particleClass, Location.class,
                        int.class, double.class, double.class, double.class, double.class);
            } catch (Throwable ignored) {
                // 1.8, the legacy effect path below is the only one
            }
            if (particleClass != null) {
                try {
                    spawnParticleData = World.class.getMethod("spawnParticle", particleClass, Location.class,
                            int.class, double.class, double.class, double.class, double.class, Object.class);
                } catch (Throwable ignored) {
                }
                try {
                    playerSpawnParticle = Player.class.getMethod("spawnParticle", particleClass, Location.class,
                            int.class, double.class, double.class, double.class, double.class);
                } catch (Throwable ignored) {
                }
                try {
                    Class<?> dustClass = Class.forName("org.bukkit.Particle$DustOptions");
                    dustOptions = dustClass.getConstructor(org.bukkit.Color.class, float.class);
                } catch (Throwable ignored) {
                }
            }
            try {
                worldSpigot = World.class.getMethod("spigot");
                Class<?> spigotClass = Class.forName("org.bukkit.World$Spigot");
                spigotPlayEffect = spigotClass.getMethod("playEffect", Location.class, Effect.class,
                        int.class, int.class, float.class, float.class, float.class, float.class,
                        int.class, int.class);
            } catch (Throwable ignored) {
                // modern server, World.Spigot#playEffect no longer exists
            }
            resolved = true;
        }
    }

    private static String normalize(String name) {
        String normalized = name.trim();
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return normalized.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_').replace('.', '_');
    }

    private static int clampColor(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 255);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
