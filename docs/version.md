# Version utilities

**Available on:** Bukkit ✅ full · Velocity ⚠️ proxy/protocol version · BungeeCord ⚠️ proxy/protocol version · Hytale ⚠️ plugin manifest semver only

`BlueFoundation.Version` exposes Minecraft/Bukkit version helpers.

```java
BlueFoundation.Version.MinecraftVersion version = BlueFoundation.Version.current();

if (BlueFoundation.Version.isAtLeast(1, 20)) {
    // modern server logic
}

String bukkitVersion = BlueFoundation.Version.bukkitVersion();
String craftBukkitRevision = BlueFoundation.Version.craftBukkitRevision();
```

The parser supports classic versions like `1.8.8`, modern versions like `1.21.11`, and future-style versions like `26.1`.
