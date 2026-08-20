# Hytale

`net.blueva.foundation:BlueFoundation-Hytale`. Hytale is Hypixel Studios' own game, not Minecraft server software - this module is a from-scratch implementation against Hytale's own plugin API, not a port of Bukkit/Spigot code, and that API is still young: expect real gaps below, not stubs papering over them.

```java
BlueFoundation.Dependencies.loader(plugin);
BlueFoundation.Configs.yaml(plugin, "config.yml");

BlueFoundation.Messages.send(sender, "<green>Hello!");

BlueFoundation.Commands.register(plugin.getCommandRegistry(), "hello", context -> {
    context.sendMessage(BlueFoundation.Text.toMessage("hi"));
    return CompletableFuture.completedFuture(null);
});

BlueFoundation.Sounds.play(player, "SFX_UI_Click", SoundCategory.UI, 1.0f, 1.0f);
```

- **Dependencies, Configs** - same core as every platform, plus `PluginBase`-based convenience overloads. Hytale's own logger type isn't a `java.util.logging.Logger`, so downloads through these overloads aren't logged. `Configs` runs standalone, alongside Hytale's own Codec/`BuilderCodec` config system rather than integrating with it.
- **Reflection** - generic only.
- **Version** - `Version.pluginVersion(plugin)` returns the plugin's own manifest semver. There's no public "server version" accessor to compare against.
- **Text, Messages** - `Text.toMessage(String)` bridges BlueFoundation's MiniMessage engine to Hytale's `Message` builder. `Message` is a single mutable builder over a flat property bag, not a component tree, so only bold/italic/color survive the trip - the real server jar exposes no strikethrough/underline/title/action-bar API to target. `Messages.send` works with anything implementing `IMessageReceiver` (e.g. `CommandSender`).
- **Commands** - basic literal registration over `CommandRegistry`/`AbstractCommand`. For typed arguments/suggestions, extend `AbstractCommand` directly.
- **Scheduler** - wraps `HytaleServer.SCHEDULED_EXECUTOR`, since Hytale's own `TaskRegistry` only tracks a `Future` for cleanup on unload and has no `runTaskLater`-style scheduling API of its own.
- **Sounds** - plays any sound-event id registered by the loaded asset packs, through `SoundUtil`.
- **Music** - per-player MIDI melody playback (parsed via the JDK's own `javax.sound.midi`, not NBS - Hytale has no note-block instrument sounds to map NBS onto), triggering one sound event per note. BlueFoundation makes no assumption about which asset pack provides those events or how it names them: you supply a `MusicManager.SoundEventIdResolver` (instrument, octave, note-length) → event id when building one via `BlueFoundation.Music.create(plugin, resolver)`. `BlueFoundation.Music.createWithInstrumentSounds(plugin)` is a ready-made alternative that downloads and injects a free instrument sound pack from [HytaleSounds](https://github.com/BluevaDevelopment/HytaleSounds) (GPL-3.0) via `Assets` below - a restart is required after the first install.
- **Assets** - injects files directly into the calling plugin's own running jar, the one asset-registration mechanism proven reliable in production (BlueArcade's item sync uses the same approach). Changes only take effect after a restart, and the consuming plugin's `manifest.json` needs `"IncludesAssetPack": true` for Hytale to load what gets injected. Generic on purpose - not tied to sounds or any other specific asset kind; `Music.createWithInstrumentSounds` is one consumer of it.
- **Scoreboards** *(experimental)* - the real server API has no scoreboard/sidebar/boss-bar concept at all, so this builds a persistent HUD page with one label per line, on top of a third-party HTML/CSS-like UI library (`compileOnly`, not shaded). `Scoreboards` downloads it straight into the plugin's own classloader on first use, via `BlueFoundation.Dependencies`, then calls it normally.

Not included: `Materials`, `Items`, `Entities`, `NPCs`, `Particles`, `Attributes`, `GameRules`, `Inventories`, `Blocks`, `Worlds`, `Players`, `BossBars` - Hytale's ECS-based world/entity model has nothing equivalent to port from Bukkit onto (not yet mapped), and holograms are intentionally not attempted (there's no reliable reference implementation for them yet).

Some Hytale APIs this module relies on (`Entity#getUuid()`, `Player#getPlayerRef()`) are marked deprecated-for-removal in the current server build with no documented replacement yet - expected, given how early the platform still is.
