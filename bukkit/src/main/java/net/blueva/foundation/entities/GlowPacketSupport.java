package net.blueva.foundation.entities;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.version.Version;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Reflection-backed packet implementation for per-viewer entity glow. */
final class GlowPacketSupport {

    private static final byte GLOWING_FLAG = 1 << 6;

    private final boolean modernValues;

    private final Class<?> metadataPacketClass;
    private final Class<?> bundlePacketClass;
    private final Field metadataEntityId;
    private final Field metadataItems;
    private final Method bundlePackets;

    private final Field sharedFlags;
    private final Method getEntityData;
    private final Method watcherGet;
    private final Constructor<?> oldDataItemConstructor;
    private final Method dataValueCreate;
    private final Method oldItemAccessor;
    private final Method itemId;
    private final Method itemValue;
    private final Constructor<?> metadataConstructor;

    private final Field playerConnection;
    private final Field networkConnection;
    private final Field channel;
    private final Method sendPacket;

    private final Constructor<?> teamPacketConstructor;
    private final Constructor<?> teamParametersConstructor;
    private final Constructor<?> scoreboardConstructor;
    private final Constructor<?> playerTeamConstructor;
    private final Method setTeamColor;
    private final Method resolveColor;
    private final boolean optionalTeamColor;

    private final Set<Object> ownedPackets =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));

    GlowPacketSupport() throws ReflectiveOperationException {
        modernValues = Version.isAtLeast(1, 19, 3);

        Class<?> entityClass = GlowReflectionResolver.firstClass(
                "net.minecraft.world.entity.Entity");
        Class<?> watcherClass = GlowReflectionResolver.firstClass(
                "net.minecraft.network.syncher.SynchedEntityData",
                "net.minecraft.network.syncher.DataWatcher");
        Class<?> accessorClass = GlowReflectionResolver.firstClass(
                "net.minecraft.network.syncher.EntityDataAccessor",
                "net.minecraft.network.syncher.DataWatcherObject");
        metadataPacketClass = GlowReflectionResolver.firstClass(
                "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket",
                "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata");

        sharedFlags = GlowReflectionResolver.sharedFlagsField(entityClass, accessorClass);
        getEntityData = GlowReflectionResolver.noArgReturning(entityClass, watcherClass);
        watcherGet = GlowReflectionResolver.oneArgMethod(watcherClass, accessorClass, Object.class);

        if (modernValues) {
            Class<?> valueClass = GlowReflectionResolver.innerClassWithFactory(
                    watcherClass, accessorClass, Object.class);
            dataValueCreate = GlowReflectionResolver.dataValueFactory(valueClass, accessorClass);
            itemId = GlowReflectionResolver.noArgReturning(valueClass, int.class);
            itemValue = GlowReflectionResolver.noArgReturning(valueClass, Object.class);
            oldDataItemConstructor = null;
            oldItemAccessor = null;
            metadataConstructor = GlowReflectionResolver.constructor(metadataPacketClass, int.class, List.class);
        } else {
            Class<?> itemClass = GlowReflectionResolver.innerClassWithConstructor(
                    watcherClass, accessorClass, Object.class);
            oldDataItemConstructor = GlowReflectionResolver.constructor(itemClass, accessorClass, Object.class);
            oldItemAccessor = GlowReflectionResolver.noArgReturning(itemClass, accessorClass);
            itemValue = GlowReflectionResolver.noArgReturning(itemClass, Object.class);
            dataValueCreate = null;
            itemId = null;
            metadataConstructor = GlowReflectionResolver.constructor(
                    metadataPacketClass, int.class, watcherClass, boolean.class);
        }

        metadataEntityId = GlowReflectionResolver.intField(metadataPacketClass);
        metadataItems = GlowReflectionResolver.fieldByType(metadataPacketClass, List.class, false);

        Class<?> listenerClass;
        if (Version.isAtLeast(1, 20, 2)) {
            listenerClass = GlowReflectionResolver.firstClass(
                    "net.minecraft.server.network.ServerCommonPacketListenerImpl");
        } else {
            listenerClass = GlowReflectionResolver.firstClass(
                    "net.minecraft.server.network.ServerGamePacketListenerImpl",
                    "net.minecraft.server.network.PlayerConnection");
        }
        Class<?> serverPlayerClass = GlowReflectionResolver.firstClass(
                "net.minecraft.server.level.ServerPlayer",
                "net.minecraft.server.level.EntityPlayer");
        Class<?> connectionClass = GlowReflectionResolver.firstClass(
                "net.minecraft.network.Connection",
                "net.minecraft.network.NetworkManager");
        Class<?> packetClass = GlowReflectionResolver.firstClass(
                "net.minecraft.network.protocol.Packet");
        playerConnection = GlowReflectionResolver.fieldByType(serverPlayerClass, listenerClass, false);
        networkConnection = GlowReflectionResolver.fieldByType(listenerClass, connectionClass, false);
        channel = GlowReflectionResolver.fieldByType(connectionClass, Channel.class, false);
        sendPacket = GlowReflectionResolver.packetSender(listenerClass, packetClass);

        if (Version.isAtLeast(1, 19, 4)) {
            bundlePacketClass = GlowReflectionResolver.firstClass(
                    "net.minecraft.network.protocol.BundlePacket");
            bundlePackets = GlowReflectionResolver.noArgAssignableReturn(bundlePacketClass, Iterable.class);
        } else {
            bundlePacketClass = null;
            bundlePackets = null;
        }

        Class<?> scoreboardClass = GlowReflectionResolver.firstClass(
                "net.minecraft.world.scores.Scoreboard");
        Class<?> playerTeamClass = GlowReflectionResolver.firstClass(
                "net.minecraft.world.scores.PlayerTeam",
                "net.minecraft.world.scores.ScoreboardTeam");
        Class<?> teamPacketClass = GlowReflectionResolver.firstClass(
                "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket",
                "net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam");
        Class<?> parametersClass = GlowReflectionResolver.innerClassWithConstructor(
                teamPacketClass, playerTeamClass);

        scoreboardConstructor = GlowReflectionResolver.constructor(scoreboardClass);
        playerTeamConstructor = GlowReflectionResolver.constructor(
                playerTeamClass, scoreboardClass, String.class);

        optionalTeamColor = Version.isAtLeast(26, 2);
        if (optionalTeamColor) {
            Class<?> colorClass = GlowReflectionResolver.firstClass(
                    "net.minecraft.world.scores.TeamColor");
            setTeamColor = GlowReflectionResolver.oneArgMethod(playerTeamClass, Optional.class, void.class);
            resolveColor = GlowReflectionResolver.staticFactory(
                    colorClass, colorClass, String.class);
        } else {
            Class<?> colorClass = GlowReflectionResolver.firstClass(
                    "net.minecraft.ChatFormatting",
                    "net.minecraft.EnumChatFormat");
            setTeamColor = GlowReflectionResolver.oneArgMethod(playerTeamClass, colorClass, void.class);
            resolveColor = GlowReflectionResolver.staticFactory(
                    colorClass, colorClass, char.class);
        }

        teamPacketConstructor = GlowReflectionResolver.constructor(teamPacketClass,
                String.class, int.class, Optional.class, Collection.class);
        teamParametersConstructor = GlowReflectionResolver.constructor(parametersClass, playerTeamClass);
    }

    TrackedGlow track(Entity entity, ChatColor color) throws ReflectiveOperationException {
        Object handle = requireHandle(entity);
        Object watcher = getEntityData.invoke(handle);
        byte flags = ((Byte) watcherGet.invoke(watcher, sharedFlags.get(null))).byteValue();
        return new TrackedGlow(entity, entity instanceof Player ? entity.getName() : entity.getUniqueId().toString(),
                color, flags);
    }

    boolean sendGlow(Player viewer, TrackedGlow glow, boolean enabled) throws ReflectiveOperationException {
        glow.enabled = enabled;
        if (!viewer.getWorld().equals(glow.entity.getWorld())) {
            return false;
        }
        Object packet = metadataPacket(glow, applyGlow(glow.otherFlags, enabled));
        ownedPackets.add(packet);
        send(viewer, packet);
        return true;
    }

    Object rewriteMetadata(Object packet, TrackedGlow glow) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        List<Object> original = (List<Object>) metadataItems.get(packet);
        if (original == null) {
            return null;
        }

        List<Object> items = original;
        boolean foundFlags = false;
        boolean changed = false;
        for (int i = 0; i < original.size(); i++) {
            Object item = original.get(i);
            if (!isFlagsItem(item)) {
                continue;
            }
            foundFlags = true;
            byte flags = ((Byte) itemValue.invoke(item)).byteValue();
            glow.otherFlags = flags;
            byte updated = applyGlow(flags, glow.enabled);
            if (updated != flags) {
                items = new ArrayList<Object>(original);
                items.set(i, newFlagsItem(updated));
                changed = true;
            }
            break;
        }

        if (!foundFlags) {
            byte updated = applyGlow(glow.otherFlags, glow.enabled);
            if (updated != 0) {
                items = new ArrayList<Object>(original);
                items.add(newFlagsItem(updated));
                changed = true;
            }
        }
        if (!changed) {
            return null;
        }

        int entityId = metadataEntityId.getInt(packet);
        return metadataPacket(entityId, glow, items);
    }

    boolean isOwned(Object packet) {
        return ownedPackets.remove(packet);
    }

    void synchronizeFlags(Object packet, TrackedGlow glow) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) metadataItems.get(packet);
        if (items == null) {
            return;
        }
        for (Object item : items) {
            if (isFlagsItem(item)) {
                glow.otherFlags = ((Byte) itemValue.invoke(item)).byteValue();
                return;
            }
        }
    }

    boolean isMetadata(Object packet) {
        return packet != null && metadataPacketClass.isInstance(packet);
    }

    int metadataEntityId(Object packet) throws IllegalAccessException {
        return metadataEntityId.getInt(packet);
    }

    Iterable<?> bundledPackets(Object packet) throws ReflectiveOperationException {
        if (bundlePacketClass == null || !bundlePacketClass.isInstance(packet)) {
            return null;
        }
        return (Iterable<?>) bundlePackets.invoke(packet);
    }

    Channel channel(Player player) throws ReflectiveOperationException {
        Object handle = requireHandle(player);
        Object listener = playerConnection.get(handle);
        Object connection = networkConnection.get(listener);
        return (Channel) channel.get(connection);
    }

    void send(Player player, Object packet) throws ReflectiveOperationException {
        Object listener = playerConnection.get(requireHandle(player));
        sendPacket.invoke(listener, packet);
    }

    TeamPackets createTeam(String id, ChatColor color) throws ReflectiveOperationException {
        Object scoreboard = scoreboardConstructor.newInstance();
        Object team = playerTeamConstructor.newInstance(scoreboard, id);
        Object resolvedColor = optionalTeamColor
                ? resolveColor.invoke(null, color.name().toLowerCase(Locale.ROOT))
                : resolveColor.invoke(null, Character.valueOf(color.getChar()));
        setTeamColor.invoke(team, optionalTeamColor ? Optional.of(resolvedColor) : resolvedColor);
        Object parameters = teamParametersConstructor.newInstance(team);
        Object creation = teamPacketConstructor.newInstance(
                id, Integer.valueOf(0), Optional.of(parameters), Collections.emptyList());
        Object removal = teamPacketConstructor.newInstance(
                id, Integer.valueOf(1), Optional.empty(), Collections.emptyList());
        return new TeamPackets(id, creation, removal);
    }

    private Object metadataPacket(TrackedGlow glow, byte flags) throws ReflectiveOperationException {
        return metadataPacket(glow.entity.getEntityId(), glow,
                Collections.singletonList(newFlagsItem(flags)));
    }

    private Object metadataPacket(int entityId, TrackedGlow glow, List<Object> items)
            throws ReflectiveOperationException {
        if (modernValues) {
            return metadataConstructor.newInstance(Integer.valueOf(entityId), items);
        }
        Object watcher = getEntityData.invoke(requireHandle(glow.entity));
        Object packet = metadataConstructor.newInstance(Integer.valueOf(entityId), watcher, Boolean.FALSE);
        metadataItems.set(packet, items);
        return packet;
    }

    private Object newFlagsItem(byte flags) throws ReflectiveOperationException {
        Object accessor = sharedFlags.get(null);
        if (modernValues) {
            return dataValueCreate.invoke(null, accessor, Byte.valueOf(flags));
        }
        return oldDataItemConstructor.newInstance(accessor, Byte.valueOf(flags));
    }

    private boolean isFlagsItem(Object item) throws ReflectiveOperationException {
        if (modernValues) {
            return ((Integer) itemId.invoke(item)).intValue() == 0;
        }
        return sharedFlags.get(null).equals(oldItemAccessor.invoke(item));
    }

    private static byte applyGlow(byte flags, boolean enabled) {
        return enabled ? (byte) (flags | GLOWING_FLAG) : (byte) (flags & ~GLOWING_FLAG);
    }

    private static Object requireHandle(Entity entity) {
        Object handle = Reflection.getHandle(entity);
        if (handle == null) {
            throw new IllegalStateException("Unable to access the entity handle");
        }
        return handle;
    }

    static final class TrackedGlow {
        final Entity entity;
        final String teamEntry;
        volatile ChatColor color;
        volatile byte otherFlags;
        volatile boolean enabled = true;

        private TrackedGlow(Entity entity, String teamEntry, ChatColor color, byte otherFlags) {
            this.entity = entity;
            this.teamEntry = teamEntry;
            this.color = color;
            this.otherFlags = otherFlags;
        }
    }

    final class TeamPackets {
        private final String id;
        private final Object creation;
        private final Object removal;
        private final Map<String, Object> addPackets = new java.util.concurrent.ConcurrentHashMap<String, Object>();
        private final Map<String, Object> removePackets = new java.util.concurrent.ConcurrentHashMap<String, Object>();

        private TeamPackets(String id, Object creation, Object removal) {
            this.id = id;
            this.creation = creation;
            this.removal = removal;
        }

        Object creation() {
            return creation;
        }

        Object removal() {
            return removal;
        }

        Object add(String entry) throws ReflectiveOperationException {
            Object packet = addPackets.get(entry);
            if (packet == null) {
                packet = teamPacketConstructor.newInstance(
                        id, Integer.valueOf(3), Optional.empty(), Collections.singletonList(entry));
                Object previous = addPackets.putIfAbsent(entry, packet);
                if (previous != null) {
                    packet = previous;
                }
            }
            return packet;
        }

        Object remove(String entry) throws ReflectiveOperationException {
            Object packet = removePackets.get(entry);
            if (packet == null) {
                packet = teamPacketConstructor.newInstance(
                        id, Integer.valueOf(4), Optional.empty(), Collections.singletonList(entry));
                Object previous = removePackets.putIfAbsent(entry, packet);
                if (previous != null) {
                    packet = previous;
                }
            }
            return packet;
        }
    }
}
