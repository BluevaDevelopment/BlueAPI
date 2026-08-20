package net.blueva.foundation.players;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Optional;
import java.util.UUID;

/**
 * Partial player helpers - a proxy has no world/inventory/block state, so this
 * only covers what actually exists at the connection level: ping, identity
 * and lookups. See {@link net.blueva.foundation.version.Version} for protocol
 * version.
 */
public class Players {

    protected Players() {
    }

    public static long ping(Player player) {
        return player.getPing();
    }

    public static Optional<Player> byName(ProxyServer proxyServer, String name) {
        return proxyServer.getPlayer(name);
    }

    public static Optional<Player> byUuid(ProxyServer proxyServer, UUID uuid) {
        return proxyServer.getPlayer(uuid);
    }
}
