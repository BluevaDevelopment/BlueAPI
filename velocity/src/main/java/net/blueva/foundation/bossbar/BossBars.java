package net.blueva.foundation.bossbar;

import net.blueva.foundation.text.AdventureText;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;

/**
 * Boss bars backed by native Adventure {@link BossBar}. Unlike Bukkit's
 * {@code BossBars}, this needs none of the pre-1.9 "wither trick" reflection -
 * every Velocity player is an {@link Audience} that natively supports boss bars.
 */
public class BossBars {

    protected BossBars() {
    }

    public static BossBar create(String title, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        return BossBar.bossBar(AdventureText.component(title), clamp(progress), color, overlay);
    }

    public static BossBar create(String title, float progress) {
        return create(title, progress, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
    }

    public static void show(Audience audience, BossBar bossBar) {
        if (audience != null && bossBar != null) {
            audience.showBossBar(bossBar);
        }
    }

    public static void hide(Audience audience, BossBar bossBar) {
        if (audience != null && bossBar != null) {
            audience.hideBossBar(bossBar);
        }
    }

    public static void title(BossBar bossBar, String title) {
        if (bossBar != null) {
            bossBar.name(AdventureText.component(title));
        }
    }

    public static void progress(BossBar bossBar, float progress) {
        if (bossBar != null) {
            bossBar.progress(clamp(progress));
        }
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
