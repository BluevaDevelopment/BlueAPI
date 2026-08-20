# Boss bars

**Available on:** Bukkit ✅ legacy fallback included · Velocity ✅ native Adventure · BungeeCord ❌ no such API · Hytale ❌ no such API

`BlueFoundation.BossBars` creates boss bars you can keep, update and remove. The Bukkit boss bar
API (`org.bukkit.boss.BossBar`) arrived in 1.9, so nothing here names that type: `create` hands back
a `BfBossBar` that owns the real bar when the server has one and silently no-ops when it does not.

```java
BfBossBar bar = BlueFoundation.BossBars.create("<gold>Setting up arena", "YELLOW", "SEGMENTED_6", 1.0);
bar.addPlayer(player);

bar.setTitle("<green>Arena ready");
bar.setProgress(0.5);
bar.setColor("GREEN");

bar.removePlayer(player);   // or bar.removeAll() to drop every viewer
```

Titles accept MiniMessage or legacy text. Colours and styles are passed as `BarColor` /
`BarStyle` constant names; unknown names are ignored rather than throwing.

## Keep the handle

This is the difference from `BlueFoundation.Messages.bossBar`, which is fire-and-forget: it builds
a throwaway bar per call, so using it for something you update repeatedly stacks bars on the
player's screen. Use `Messages.bossBar` for one-off notices, and `BossBars.create` whenever the bar
has a lifetime:

```java
// Wrong for a bar you refresh every tick. Each call adds another bar.
BlueFoundation.Messages.bossBar(player, title, colour, style, progress);

// Right. One bar, updated in place.
BfBossBar bar = BlueFoundation.BossBars.create(title, "YELLOW", "SEGMENTED_6", 1.0);
```

## Servers without a boss bar API

1.8 has no boss bar API, but it can still show one. That is what plugins did before 1.9. Pass a
plugin and the bar falls back to the wither trick: an invisible wither is sent to the viewer as
packets 90 blocks in front of them, its custom name is the title, and its health is the progress.
The entity never exists server-side and nobody else sees it.

```java
BfBossBar bar = BlueFoundation.BossBars.create(plugin, "<gold>Starting", "YELLOW", "SEGMENTED_6", 1.0);
bar.addPlayer(player);
```

The plugin is required because the wither has to be pulled along as the viewer moves, or the client
stops tracking it and the bar vanishes. That runs as a repeating task, cancelled automatically once
the last viewer is removed. The plugin-less `create` cannot do this and stays a no-op on 1.8.

Two things behave differently on that path:

- `setColor` and `setStyle` do nothing. The wither bar has a fixed appearance.
- `isSupported()` on `BossBars` reports whether the *native* API exists, so it is `false` on 1.8
  even when the fallback is working. Ask the bar itself instead:

```java
if (!bar.isSupported()) {
    BlueFoundation.Messages.title(player, title, "", 10, 40, 10);
}
```
