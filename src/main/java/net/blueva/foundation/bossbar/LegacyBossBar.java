package net.blueva.foundation.bossbar;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.scheduler.Scheduler;
import net.blueva.foundation.text.TextAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss bar for servers older than 1.9, where Bukkit has no boss bar API.
 *
 * <p>Before the API existed, the only way to show one was to spawn an invisible wither far in
 * front of the player and let the client draw its health bar: the wither's custom name becomes the
 * title and its health becomes the progress. The entity is sent as packets to that one player, so
 * it never really exists on the server and nobody else sees it.</p>
 *
 * <p>Everything is resolved reflectively because this library compiles against the 1.8.8 Bukkit
 * API and must not name NMS or CraftBukkit types. When any part of that fails the whole thing
 * reports unsupported and the caller falls back to no-op behaviour rather than breaking.</p>
 */
final class LegacyBossBar {

    /**
     * How far in front of the viewer the wither sits. Far enough that the model is never visible,
     * close enough that the client still tracks the entity and keeps drawing its bar.
     */
    private static final double DISTANCE = 90.0D;

    /** Wither invulnerability ticks, which suppresses the spawn animation. */
    private static final int INVULNERABLE_TICKS = 900;

    private static final Object LOCK = new Object();
    private static volatile boolean resolved;
    private static volatile boolean available;

    private static Method craftWorldGetHandle;
    private static Constructor<?> witherConstructor;
    private static Method setLocation;
    private static Method setInvisible;
    private static Method setCustomName;
    private static Method setCustomNameVisible;
    private static Method setHealth;
    private static Method getMaxHealth;
    private static Method getEntityId;
    private static Method setInvulnerableTicks;
    private static Constructor<?> spawnPacket;
    private static Constructor<?> destroyPacket;
    private static Constructor<?> teleportPacket;

    private final Plugin plugin;
    private final Map<UUID, Object> withers = new ConcurrentHashMap<UUID, Object>();
    private final Map<UUID, Player> viewers = new ConcurrentHashMap<UUID, Player>();

    private volatile String title;
    private volatile double progress;
    private Scheduler.Task task;

    LegacyBossBar(Plugin plugin, String title, double progress) {
        this.plugin = plugin;
        this.title = TextAdapter.legacySection(title);
        this.progress = clamp(progress);
    }

    static boolean isAvailable() {
        if (!resolved) {
            resolve();
        }
        return available;
    }

    void setTitle(String value) {
        this.title = TextAdapter.legacySection(value);
        respawnAll();
    }

    void setProgress(double value) {
        this.progress = clamp(value);
        respawnAll();
    }

    void addPlayer(Player player) {
        if (player == null || !isAvailable()) {
            return;
        }
        viewers.put(player.getUniqueId(), player);
        spawnFor(player);
        startTracking();
    }

    void removePlayer(Player player) {
        if (player == null) {
            return;
        }
        viewers.remove(player.getUniqueId());
        destroyFor(player);
        if (viewers.isEmpty()) {
            stopTracking();
        }
    }

    void removeAll() {
        for (Player viewer : viewers.values()) {
            destroyFor(viewer);
        }
        viewers.clear();
        stopTracking();
    }

    /**
     * The wither stays where it was spawned, so it has to be pulled along as the viewer moves or
     * the client stops tracking it and the bar disappears. Teleporting avoids the flicker a
     * destroy/respawn pair would cause at this interval.
     */
    private void startTracking() {
        if (task != null || plugin == null) {
            return;
        }
        task = Scheduler.syncTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player viewer : viewers.values()) {
                    if (viewer == null || !viewer.isOnline()) {
                        continue;
                    }
                    Object wither = withers.get(viewer.getUniqueId());
                    if (wither == null) {
                        continue;
                    }
                    if (!teleportFor(viewer, wither)) {
                        spawnFor(viewer);
                    }
                }
            }
        }, 20L, 20L);
    }

    private void stopTracking() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void respawnAll() {
        if (!isAvailable()) {
            return;
        }
        for (Player viewer : viewers.values()) {
            spawnFor(viewer);
        }
    }

    private void spawnFor(Player player) {
        if (player == null || !player.isOnline() || !isAvailable()) {
            return;
        }
        destroyFor(player);
        try {
            Object worldHandle = craftWorldGetHandle.invoke(player.getWorld());
            Object wither = witherConstructor.newInstance(worldHandle);

            Location location = anchor(player);
            setLocation.invoke(wither, location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch());
            setInvisible.invoke(wither, Boolean.TRUE);
            if (setInvulnerableTicks != null) {
                setInvulnerableTicks.invoke(wither, INVULNERABLE_TICKS);
            }
            setCustomName.invoke(wither, title == null ? "" : title);
            setCustomNameVisible.invoke(wither, Boolean.TRUE);

            float max = ((Number) getMaxHealth.invoke(wither)).floatValue();
            float health = (float) Math.max(1.0E-4D, max * progress);
            setHealth.invoke(wither, health);

            withers.put(player.getUniqueId(), wither);
            Reflection.sendPacket(player, spawnPacket.newInstance(wither));
        } catch (Throwable ignored) {
            withers.remove(player.getUniqueId());
        }
    }

    private boolean teleportFor(Player player, Object wither) {
        if (teleportPacket == null) {
            return false;
        }
        try {
            Location location = anchor(player);
            setLocation.invoke(wither, location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch());
            Reflection.sendPacket(player, teleportPacket.newInstance(wither));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void destroyFor(Player player) {
        Object wither = withers.remove(player.getUniqueId());
        if (wither == null || !player.isOnline()) {
            return;
        }
        try {
            int id = ((Number) getEntityId.invoke(wither)).intValue();
            Reflection.sendPacket(player, destroyPacket.newInstance(new int[]{id}));
        } catch (Throwable ignored) {
        }
    }

    private Location anchor(Player player) {
        Vector direction = player.getLocation().getDirection();
        return player.getLocation().add(direction.multiply(DISTANCE));
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static void resolve() {
        synchronized (LOCK) {
            if (resolved) {
                return;
            }
            resolved = true;
            try {
                Class<?> craftWorld = Reflection.craftBukkitClass("CraftWorld");
                Class<?> nmsWorld = Reflection.nmsClass("World");
                Class<?> witherClass = Reflection.nmsClass("EntityWither");
                Class<?> entityClass = Reflection.nmsClass("Entity");
                Class<?> livingClass = Reflection.nmsClass("EntityLiving");
                if (craftWorld == null || nmsWorld == null || witherClass == null
                        || entityClass == null || livingClass == null) {
                    return;
                }

                craftWorldGetHandle = craftWorld.getMethod("getHandle");
                witherConstructor = witherClass.getConstructor(nmsWorld);
                setLocation = entityClass.getMethod("setLocation",
                        double.class, double.class, double.class, float.class, float.class);
                setInvisible = entityClass.getMethod("setInvisible", boolean.class);
                setCustomName = entityClass.getMethod("setCustomName", String.class);
                setCustomNameVisible = entityClass.getMethod("setCustomNameVisible", boolean.class);
                setHealth = livingClass.getMethod("setHealth", float.class);
                getMaxHealth = livingClass.getMethod("getMaxHealth");
                getEntityId = entityClass.getMethod("getId");

                Class<?> spawnClass = Reflection.nmsClass("PacketPlayOutSpawnEntityLiving");
                Class<?> destroyClass = Reflection.nmsClass("PacketPlayOutEntityDestroy");
                if (spawnClass == null || destroyClass == null) {
                    return;
                }
                spawnPacket = spawnClass.getConstructor(livingClass);
                destroyPacket = destroyClass.getConstructor(int[].class);

                // Optional: without it the bar still works, it just gets respawned instead of moved.
                Class<?> teleportClass = Reflection.nmsClass("PacketPlayOutEntityTeleport");
                if (teleportClass != null) {
                    try {
                        teleportPacket = teleportClass.getConstructor(entityClass);
                    } catch (Throwable ignored) {
                        teleportPacket = null;
                    }
                }

                // Obfuscated setter for the wither's invulnerability timer; absent on some forks.
                try {
                    setInvulnerableTicks = witherClass.getMethod("r", int.class);
                } catch (Throwable ignored) {
                    setInvulnerableTicks = null;
                }

                available = true;
            } catch (Throwable ignored) {
                available = false;
            }
        }
    }
}
