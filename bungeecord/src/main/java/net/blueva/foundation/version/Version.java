package net.blueva.foundation.version;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Version helpers for the proxy itself and for connected players.
 *
 * <p>Like Velocity, BungeeCord has no single "server version" - only the
 * proxy's own version string and each player's negotiated protocol number.</p>
 */
public class Version {

    protected Version() {
    }

    public static String proxy(ProxyServer proxyServer) {
        return proxyServer.getVersion();
    }

    public static int protocolNumber(ProxiedPlayer player) {
        return player.getPendingConnection().getVersion();
    }
}
