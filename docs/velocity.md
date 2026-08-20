# Velocity

`net.blueva.foundation:BlueFoundation-Velocity`. Velocity is Adventure-native end to end - `ProxyServer`, `Player` and `CommandSource` all implement Adventure's `Audience` - so this module leans directly on `net.kyori.adventure` instead of the legacy-reflection tricks the Bukkit module needs.

```java
BlueFoundation.Dependencies.loader(dataDirectory, getClass().getClassLoader(), logger);
BlueFoundation.Configs.load(dataDirectory, getClass().getClassLoader(), "config.yml", ConfigFormat.YAML);

BlueFoundation.Messages.send(player, "<green>Hello!");
BlueFoundation.Messages.broadcast(proxyServer, "<yellow>Server restarting soon");

BossBar bar = BlueFoundation.BossBars.create("<red>Boss", 1.0f);
BlueFoundation.BossBars.show(player, bar);

BlueFoundation.Commands.register(proxyServer, this, "hello", (source, args) -> source.sendMessage(Component.text("hi")));

BlueFoundation.Scheduler.runLater(proxyServer, this, () -> {}, 5, TimeUnit.SECONDS);
```

- **Dependencies, Reflection (generic only), Configs** - identical to the [common module](common.md)'s core; Velocity injects `Path`/`Logger` directly, so there's no `JavaPlugin`-style bundling to bridge like on Bukkit.
- **Version** - `Version.proxy(proxyServer)` returns Velocity's own `ProxyVersion`; `Version.protocol(player)`/`protocolNumber(player)` return the player's negotiated protocol version. There is no single "server version" concept on a proxy.
- **Text, Messages** - `Text.parse` uses BlueFoundation's own MiniMessage engine (same as every platform); `Messages` renders through native Adventure, since Velocity always has it. One call either way - see [Text and messages](text-messages.md) for the shared behavior.
- **BossBars** - a thin wrapper over Adventure's own `BossBar`, genuinely simpler than Bukkit's pre-1.9 fallback since Velocity has no legacy client versions to work around.
- **Commands** - basic literal registration over `CommandManager`/`SimpleCommand`. For Brigadier argument parsing/suggestions, register a `BrigadierCommand` directly instead.
- **Scheduler** - a thin wrapper over `ProxyServer#getScheduler()`.
- **Players** - partial: ping, protocol version, username/UUID lookup. No world/inventory state exists on a proxy.

Not included: `Materials`, `Items`, `Sounds`, `Music`, `Entities`, `NPCs`, `Particles`, `Attributes`, `GameRules`, `Inventories`, `Hologram`, `Blocks`, `Worlds`, `Scoreboards` - a proxy has no game/world state for any of those to act on.
