package net.blueva.foundation.players;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

/**
 * Partial player helpers - a proxy has no world/inventory/block state, so this
 * only covers what actually exists at the connection level: ping and lookups.
 * See {@link net.blueva.foundation.version.Version} for protocol version.
 */
public class Players {

    protected Players() {
    }

    public static int ping(ProxiedPlayer player) {
        return player.getPing();
    }

    public static ProxiedPlayer byName(ProxyServer proxyServer, String name) {
        return proxyServer.getPlayer(name);
    }

    public static ProxiedPlayer byUuid(ProxyServer proxyServer, UUID uuid) {
        return proxyServer.getPlayer(uuid);
    }
}
