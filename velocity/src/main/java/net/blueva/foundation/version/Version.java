package net.blueva.foundation.version;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;

/**
 * Version helpers for the proxy itself and for connected players.
 *
 * <p>Velocity has no "Minecraft server version" the way Bukkit does - there is
 * no single version running, only the proxy's own version and each player's
 * negotiated protocol version. Both are explicit here rather than read off a
 * global singleton, since {@link ProxyServer} is always dependency-injected.</p>
 */
public class Version {

    protected Version() {
    }

    public static ProxyVersion proxy(ProxyServer proxyServer) {
        return proxyServer.getVersion();
    }

    public static ProtocolVersion protocol(Player player) {
        return player.getProtocolVersion();
    }

    public static int protocolNumber(Player player) {
        return player.getProtocolVersion().getProtocol();
    }
}
