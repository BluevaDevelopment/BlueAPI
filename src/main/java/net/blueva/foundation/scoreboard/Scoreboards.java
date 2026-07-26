package net.blueva.foundation.scoreboard;

import org.bukkit.entity.Player;

/**
 * Public entry point for BlueFoundation scoreboards.
 */
public final class Scoreboards {

    private Scoreboards() {
    }

    /**
     * Creates a new sidebar scoreboard for the given player.
     *
     * @param player the scoreboard owner
     * @return the created scoreboard
     */
    public static BfScoreboard create(Player player) {
        return new BfScoreboard(player);
    }

    /**
     * Creates a scoreboard through the Adventure-independent string API.
     *
     * @param player the scoreboard owner
     * @return the created scoreboard handle
     */
    public static BfScoreboardHandle createHandle(Player player) {
        return new BfScoreboard(player);
    }
}
