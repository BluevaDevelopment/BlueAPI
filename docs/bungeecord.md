# BungeeCord

`net.blueva.foundation:BlueFoundation-BungeeCord`. BungeeCord has no Adventure of its own, so text goes through its own `net.md_5.bungee.api.chat` component API (`BaseComponent`/`TextComponent`) - BlueFoundation bridges to it from the same MiniMessage engine every platform uses.

```java
BlueFoundation.Dependencies.loader(this).load(BlueFoundation.Dependencies.mavenCentral("com.zaxxer", "HikariCP", "4.0.3"));
BlueFoundation.Configs.yaml(this, "config.yml");

BlueFoundation.Messages.send(sender, "<green>Hello!");
BlueFoundation.Messages.broadcast(getProxy(), "<yellow>Server restarting soon");
BlueFoundation.Messages.actionBar(player, "<yellow>Action bar text");

BlueFoundation.Commands.register(this, "hello", (sender, args) -> sender.sendMessage(TextComponent.fromLegacyText("hi")));

BlueFoundation.Scheduler.runLater(this, () -> {}, 5, TimeUnit.SECONDS);
```

- **Dependencies, Configs** - same core as every platform, plus `Plugin`-based convenience overloads (`loader(Plugin)`, `Configs.yaml(Plugin, ...)`), since BungeeCord's own `Plugin` bundles a data folder, classloader and `java.util.logging.Logger` the same way Bukkit's `JavaPlugin` does.
- **Reflection** - generic only (`classExists`/`findClass`/`method`/`field`).
- **Version** - `Version.proxy(proxyServer)` returns the proxy's version string; `Version.protocolNumber(player)` reads `player.getPendingConnection().getVersion()`. No single "server version" concept on a proxy.
- **Text, Messages** - `Text.toBungee(String)` renders MiniMessage/legacy input as a `BaseComponent[]`. The whole `net.md_5.bungee.api.chat` package is marked `@Deprecated` by BungeeCord itself with no first-party replacement shipped, so that deprecation is suppressed here rather than "fixed" - a server running `net.kyori:adventure-platform-bungeecord` can bridge `CommandSender`/`ProxiedPlayer` to a real Adventure `Audience` and skip this module's `Text`/`Messages` entirely.
- **Commands** - basic registration over `PluginManager#registerCommand`.
- **Scheduler** - a thin wrapper over `Plugin#getProxy()`'s `TaskScheduler`.
- **Players** - partial: ping, name/UUID lookup. No world/inventory state exists on a proxy.

Not included: `BossBars` (no such API on BungeeCord), `Materials`, `Items`, `Sounds`, `Music`, `Entities`, `NPCs`, `Particles`, `Attributes`, `GameRules`, `Inventories`, `Hologram`, `Blocks`, `Worlds`, `Scoreboards`.
