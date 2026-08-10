# Particles

`BlueFoundation.Particles` spawns particles by name on any server version. `org.bukkit.Particle`
does not exist before 1.9, so nothing here takes or returns that type: you pass candidate names and
get a `boolean` telling you whether anything was actually spawned.

```java
BlueFoundation.Particles.spawn(location, 8, 0.4, 0.4, 0.4, 0.05, "ENCHANTED_HIT", "SPELL_WITCH");

boolean supported = BlueFoundation.Particles.isSupported("DUST", "REDSTONE");
```

Names are tried in order, so put the modern one first and let the legacy name cover older servers.
A particle that exists on no supported version simply returns `false` instead of throwing, which
makes it safe to spawn decorative effects without a version check:

```java
// END_ROD is 1.9+; on 1.8 this is a no-op.
BlueFoundation.Particles.spawn(point, 1, 0.0, 0.0, 0.0, 0.0, "END_ROD");
```

## Coloured dust

Coloured dust changed mechanism twice, so it gets a dedicated method rather than leaking the
difference to callers:

- 1.13+ passes a `Particle.DustOptions` data object.
- 1.9–1.12 use the classic `REDSTONE` hack, where the offsets carry the RGB channels and the count
  must be zero.
- 1.8 falls back to `Effect.COLOURED_DUST` through the same offset convention.

```java
BlueFoundation.Particles.spawnColored(location, 255, 85, 85, 1.2F, 4);
```

`size` and `count` are only honoured from 1.13 onwards; the older mechanisms have no equivalent.
Note that `REDSTONE` was renamed to `DUST` in 1.20.5, which this already handles internally.

## Per-viewer particles

`Player#spawnParticle` arrived in 1.9. On 1.8 there is no per-player particle in the Bukkit API at
all, so this returns `false` rather than falling back to a world-wide spawn every nearby player
would see:

```java
BlueFoundation.Particles.spawnFor(player, player.getLocation(), 1, 0.0, 0.0, 0.0, 0.0,
        "MOB_APPEARANCE", "ELDER_GUARDIAN");
```

## Resolving the constant

When you need the raw constant (to hand it to an API this class does not wrap), `match` returns it
as an opaque `Object`, since the type cannot appear in a signature here:

```java
Object particle = BlueFoundation.Particles.match("DUST", "REDSTONE");
```
