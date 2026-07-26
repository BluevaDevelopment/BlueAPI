package net.blueva.foundation.scoreboard;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Adventure-independent contract for a BlueFoundation sidebar scoreboard.
 */
public interface BfScoreboardHandle {

    String getTitle();

    void updateTitle(String title);

    List<String> getLines();

    String getLine(int line);

    void updateLine(int line, String text);

    void updateLine(int line, String text, String scoreText);

    void removeLine(int line);

    void updateLines(String... lines);

    void updateLines(Collection<String> lines);

    void updateLines(Collection<String> lines, Collection<String> scores);

    Player getPlayer();

    boolean isDeleted();

    int size();

    void delete();
}
