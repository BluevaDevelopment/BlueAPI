package net.blueva.foundation.entities;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BlueFoundation's packet-backed glow manager.
 */
final class EntityGlowManager implements GlowManager, Listener {

    private final Plugin plugin;
    private final GlowPacketSupport packets;
    private final Map<UUID, ViewerState> viewers = new ConcurrentHashMap<UUID, ViewerState>();
    private final Map<ChatColor, GlowPacketSupport.TeamPackets> teams =
            new EnumMap<ChatColor, GlowPacketSupport.TeamPackets>(ChatColor.class);
    private final String handlerPrefix;
    private final String teamPrefix;
    private final AtomicBoolean packetErrorLogged = new AtomicBoolean();
    private volatile boolean closed;

    EntityGlowManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        try {
            this.packets = new GlowPacketSupport();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize per-viewer entity glow", exception);
        }

        String uid = Integer.toHexString(System.identityHashCode(this));
        this.handlerPrefix = "bluefoundation_glow_" + uid;
        this.teamPrefix = "bfg" + trim(uid, 8);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean setGlowing(Entity entity, Player viewer) {
        return setGlowing(entity, viewer, null);
    }

    @Override
    public boolean setGlowing(Entity entity, Player viewer, ChatColor color) {
        requireOpen();
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(viewer, "viewer");
        if (color != null && !color.isColor()) {
            throw new IllegalArgumentException("The glow ChatColor must be a color");
        }
        if (!viewer.isOnline()) {
            return false;
        }

        ViewerState state = state(viewer);
        synchronized (state) {
            try {
                GlowPacketSupport.TrackedGlow glow = state.glows.get(entity.getUniqueId());
                if (glow == null) {
                    glow = packets.track(entity, color);
                    state.glows.put(entity.getUniqueId(), glow);
                    packets.sendGlow(viewer, glow, true);
                    updateColor(state, glow, null, color);
                    return true;
                }

                ChatColor previous = glow.color;
                if (glow.enabled && Objects.equals(previous, color)) {
                    return true;
                }
                glow.color = color;
                packets.sendGlow(viewer, glow, true);
                updateColor(state, glow, previous, color);
                return true;
            } catch (ReflectiveOperationException exception) {
                logPacketError(exception);
                return false;
            }
        }
    }

    @Override
    public boolean unsetGlowing(Entity entity, Player viewer) {
        requireOpen();
        if (entity == null || viewer == null) {
            return false;
        }

        ViewerState state = viewers.get(viewer.getUniqueId());
        if (state == null) {
            return false;
        }
        synchronized (state) {
            GlowPacketSupport.TrackedGlow glow = state.glows.remove(entity.getUniqueId());
            if (glow == null) {
                return false;
            }
            try {
                packets.sendGlow(viewer, glow, false);
                if (glow.color != null) {
                    GlowPacketSupport.TeamPackets team = team(glow.color);
                    packets.send(viewer, team.remove(glow.teamEntry));
                }
                return true;
            } catch (ReflectiveOperationException exception) {
                logPacketError(exception);
                return false;
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        HandlerList.unregisterAll(this);

        for (ViewerState state : new ArrayList<ViewerState>(viewers.values())) {
            Player viewer = state.viewer;
            if (viewer.isOnline()) {
                synchronized (state) {
                    for (GlowPacketSupport.TrackedGlow glow : state.glows.values()) {
                        try {
                            packets.sendGlow(viewer, glow, false);
                        } catch (ReflectiveOperationException exception) {
                            logPacketError(exception);
                        }
                    }
                    for (ChatColor color : state.sentColors) {
                        try {
                            packets.send(viewer, team(color).removal());
                        } catch (ReflectiveOperationException exception) {
                            logPacketError(exception);
                        }
                    }
                }
            }
            removeHandler(state);
        }
        viewers.clear();
        teams.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ViewerState state = viewers.remove(event.getPlayer().getUniqueId());
        if (state != null) {
            removeHandler(state);
        }
    }

    private ViewerState state(Player viewer) {
        ViewerState existing = viewers.get(viewer.getUniqueId());
        if (existing != null) {
            return existing;
        }

        ViewerState created = new ViewerState(viewer);
        ViewerState previous = viewers.putIfAbsent(viewer.getUniqueId(), created);
        if (previous != null) {
            return previous;
        }

        try {
            installHandler(created);
            return created;
        } catch (ReflectiveOperationException exception) {
            viewers.remove(viewer.getUniqueId(), created);
            throw new IllegalStateException("Unable to install the glow packet listener", exception);
        }
    }

    private void installHandler(final ViewerState state) throws ReflectiveOperationException {
        final Channel channel = packets.channel(state.viewer);
        state.channel = channel;
        state.handler = new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                if (packets.isMetadata(message) && !packets.isOwned(message)) {
                    GlowPacketSupport.TrackedGlow glow =
                            state.byEntityId(packets.metadataEntityId(message));
                    if (glow != null) {
                        Object replacement = packets.rewriteMetadata(message, glow);
                        if (replacement != null) {
                            super.write(context, replacement, promise);
                            return;
                        }
                    }
                } else {
                    Iterable<?> bundled = packets.bundledPackets(message);
                    if (bundled != null) {
                        handleBundle(context, state, bundled);
                    }
                }
                super.write(context, message, promise);
            }
        };

        runOnEventLoop(channel, new Runnable() {
            @Override
            public void run() {
                if (channel.pipeline().get(handlerPrefix) != null) {
                    return;
                }
                if (channel.pipeline().get("packet_handler") != null) {
                    channel.pipeline().addBefore("packet_handler", handlerPrefix, state.handler);
                } else {
                    channel.pipeline().addLast(handlerPrefix, state.handler);
                }
            }
        });
    }

    private void handleBundle(ChannelHandlerContext context, final ViewerState state, Iterable<?> bundled)
            throws ReflectiveOperationException {
        final List<GlowPacketSupport.TrackedGlow> refresh = new ArrayList<GlowPacketSupport.TrackedGlow>();
        for (Object packet : bundled) {
            if (!packets.isMetadata(packet)) {
                continue;
            }
            GlowPacketSupport.TrackedGlow glow = state.byEntityId(packets.metadataEntityId(packet));
            if (glow != null) {
                packets.synchronizeFlags(packet, glow);
                refresh.add(glow);
            }
        }
        if (refresh.isEmpty()) {
            return;
        }
        context.executor().schedule(new Runnable() {
            @Override
            public void run() {
                if (!state.viewer.isOnline()) {
                    return;
                }
                for (GlowPacketSupport.TrackedGlow glow : refresh) {
                    try {
                        packets.sendGlow(state.viewer, glow, glow.enabled);
                    } catch (ReflectiveOperationException exception) {
                        logPacketError(exception);
                    }
                }
            }
        }, 50L, TimeUnit.MILLISECONDS);
    }

    private void removeHandler(final ViewerState state) {
        final Channel channel = state.channel;
        if (channel == null || state.handler == null) {
            return;
        }
        runOnEventLoop(channel, new Runnable() {
            @Override
            public void run() {
                if (channel.pipeline().context(state.handler) != null) {
                    channel.pipeline().remove(state.handler);
                }
            }
        });
    }

    private void updateColor(ViewerState state, GlowPacketSupport.TrackedGlow glow,
                             ChatColor previous, ChatColor current)
            throws ReflectiveOperationException {
        if (previous != null && previous != current) {
            packets.send(state.viewer, team(previous).remove(glow.teamEntry));
        }
        if (current == null) {
            return;
        }

        GlowPacketSupport.TeamPackets team = team(current);
        if (state.sentColors.add(current)) {
            packets.send(state.viewer, team.creation());
        }
        packets.send(state.viewer, team.add(glow.teamEntry));
    }

    private GlowPacketSupport.TeamPackets team(ChatColor color) throws ReflectiveOperationException {
        synchronized (teams) {
            GlowPacketSupport.TeamPackets team = teams.get(color);
            if (team == null) {
                team = packets.createTeam(trim(teamPrefix + color.getChar(), 16), color);
                teams.put(color, team);
            }
            return team;
        }
    }

    private static void runOnEventLoop(Channel channel, Runnable action) {
        if (channel.eventLoop().inEventLoop()) {
            action.run();
        } else {
            channel.eventLoop().submit(action).syncUninterruptibly();
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("The glow manager is closed");
        }
    }

    private void logPacketError(Throwable throwable) {
        if (packetErrorLogged.compareAndSet(false, true)) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Unable to update a per-viewer entity glow", throwable);
        }
    }

    private static String trim(String value, int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }

    private static final class ViewerState {
        private final Player viewer;
        private final Map<UUID, GlowPacketSupport.TrackedGlow> glows =
                new ConcurrentHashMap<UUID, GlowPacketSupport.TrackedGlow>();
        private final EnumSet<ChatColor> sentColors = EnumSet.noneOf(ChatColor.class);
        private volatile Channel channel;
        private volatile ChannelDuplexHandler handler;

        private ViewerState(Player viewer) {
            this.viewer = viewer;
        }

        private GlowPacketSupport.TrackedGlow byEntityId(int entityId) {
            for (GlowPacketSupport.TrackedGlow glow : glows.values()) {
                if (glow.entity.getWorld().equals(viewer.getWorld())
                        && glow.entity.getEntityId() == entityId) {
                    return glow;
                }
            }
            return null;
        }
    }
}
