package net.blueva.foundation.menus.bedrock;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * Tells Bedrock players apart from Java ones without compiling against
 * Floodgate.
 *
 * <p>Four checks, tried in order of how much they actually know:</p>
 *
 * <ol>
 *   <li>Floodgate's API, if it is reachable. It has the answer outright,
 *       including for linked accounts.</li>
 *   <li>Geyser's API, if Geyser runs in this JVM. Same certainty, and it
 *       covers the Geyser-without-Floodgate setup.</li>
 *   <li>The UUID's high bits. Floodgate mints Bedrock UUIDs as
 *       {@code new UUID(0, xuid)}, so a zero high half is a Bedrock player -
 *       this is exactly the test Floodgate's own {@code isFloodgateId} makes.
 *       It cannot see a Bedrock player who logged in through a linked Java
 *       account, because that player genuinely has a Java UUID.</li>
 *   <li>The username prefix, when one has been configured. Floodgate prepends
 *       a character (a dot by default) to Bedrock usernames; it is off unless
 *       set, because a server that did not configure a prefix would otherwise
 *       misread any Java player whose name happens to start with it.</li>
 * </ol>
 */
public class Bedrock {

    private static final FloodgateFormTransport FLOODGATE = new FloodgateFormTransport();
    private static final GeyserFormTransport GEYSER = new GeyserFormTransport();

    private static volatile String usernamePrefix;

    protected Bedrock() {
    }

    /**
     * @param player the player to test, may be {@code null}
     * @return whether they are playing from Bedrock Edition
     */
    public static boolean isBedrockPlayer(Player player) {
        if (player == null) {
            return false;
        }
        if (isBedrockId(player.getUniqueId())) {
            return true;
        }
        String prefix = usernamePrefix;
        return prefix != null && player.getName() != null
                && player.getName().toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    /**
     * The same question when all that is on hand is a UUID, such as for an
     * offline player.
     *
     * @param uuid the id to test, may be {@code null}
     * @return whether it belongs to a Bedrock player
     */
    public static boolean isBedrockId(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        if (FLOODGATE.isFloodgatePlayer(uuid)) {
            return true;
        }
        if (GEYSER.isBedrockPlayer(uuid)) {
            return true;
        }
        return uuid.getMostSignificantBits() == 0L;
    }

    /**
     * Teach the UUID-only fallback about this server's Floodgate username
     * prefix, so linked-account Bedrock players are still recognised when
     * neither API is reachable.
     *
     * @param prefix the prefix from Floodgate's config, or {@code null} to
     *               stop checking names
     */
    public static void usernamePrefix(String prefix) {
        usernamePrefix = prefix == null || prefix.isEmpty() ? null : prefix.toLowerCase(Locale.ROOT);
    }

    /**
     * @return the prefix currently being checked, or {@code null}
     */
    public static String usernamePrefix() {
        return usernamePrefix;
    }

    /**
     * @return whether Floodgate's API is on the classpath and callable
     */
    public static boolean hasFloodgateApi() {
        return net.blueva.foundation.reflection.Reflection
                .classExists("org.geysermc.floodgate.api.FloodgateApi") && CumulusBridge.available();
    }

    /**
     * @return whether Geyser is running in this JVM and its API is callable
     */
    public static boolean hasGeyserApi() {
        return net.blueva.foundation.reflection.Reflection
                .classExists("org.geysermc.geyser.api.GeyserApi") && CumulusBridge.available();
    }

    static FloodgateFormTransport floodgate() {
        return FLOODGATE;
    }

    static GeyserFormTransport geyser() {
        return GEYSER;
    }
}
