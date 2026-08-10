# Boss bars

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
// Wrong for a bar you refresh every tick - each call adds another bar.
BlueFoundation.Messages.bossBar(player, title, colour, style, progress);

// Right - one bar, updated in place.
BfBossBar bar = BlueFoundation.BossBars.create(title, "YELLOW", "SEGMENTED_6", 1.0);
```

## Servers without boss bars

On 1.8 there is no boss bar API. `create` still returns a usable handle, every method is a no-op,
and `isSupported()` tells you which situation you are in if you want to fall back to a title or a
chat message:

```java
if (!BlueFoundation.BossBars.isSupported()) {
    BlueFoundation.Messages.title(player, title, "", 10, 40, 10);
}
```
