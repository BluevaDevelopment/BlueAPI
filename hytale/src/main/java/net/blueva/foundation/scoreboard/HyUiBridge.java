package net.blueva.foundation.scoreboard;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.LabelBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.List;
import java.util.Optional;

/**
 * Direct, typed usage of the third-party UI library {@link Scoreboards}
 * builds on. Deliberately kept out of {@code Scoreboards} itself: that class
 * must always be loadable even when this library isn't on the classpath yet,
 * and {@code Scoreboards} only ever calls into this one after making sure it
 * is (see {@code Scoreboards#ensureLibraryLoaded}) - by then the library is
 * on the classloader and every reference below resolves normally.
 */
final class HyUiBridge {

    private HyUiBridge() {
    }

    static Scoreboards.ScoreboardHandle show(PlayerRef playerRef, String title, List<String> lines) {
        HyUIHud hud = HudBuilder.hudForPlayer(playerRef)
                .fromHtml(buildHtml(title, lines))
                .show();
        return new Scoreboards.ScoreboardHandle(hud);
    }

    static void update(Scoreboards.ScoreboardHandle handle, String title, List<String> lines) {
        HyUIHud hud = (HyUIHud) handle.hud;

        Optional<LabelBuilder> titleLabel = hud.getById("bf-scoreboard-title", LabelBuilder.class);
        titleLabel.ifPresent(label -> label.withText(title));

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Optional<LabelBuilder> lineLabel = hud.getById("bf-scoreboard-line-" + i, LabelBuilder.class);
            lineLabel.ifPresent(label -> label.withText(line));
        }
    }

    static void hide(Scoreboards.ScoreboardHandle handle) {
        ((HyUIHud) handle.hud).remove();
    }

    private static String buildHtml(String title, List<String> lines) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='anchor-top: 10; anchor-right: 10; layout-mode: top;'>");
        html.append("<p id='bf-scoreboard-title'>").append(escape(title)).append("</p>");
        for (int i = 0; i < lines.size(); i++) {
            html.append("<p id='bf-scoreboard-line-").append(i).append("'>")
                    .append(escape(lines.get(i)))
                    .append("</p>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
