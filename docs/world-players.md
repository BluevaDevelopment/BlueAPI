# Worlds, blocks, players and inventories

**Available on:** Bukkit ✅ full · Velocity ⚠️ `Players` only (ping/UUID/name) · BungeeCord ⚠️ `Players` only (ping/UUID/name) · Hytale ❌ not yet mapped

Small multi-version helpers for API that moved, was renamed, or simply did not exist yet. Each one
picks the right call for the running server so callers stay free of version branches.

## Worlds

`World#getMinHeight` is 1.17+. Older worlds always start at y=0, which is what this returns there:

```java
int minY = BlueFoundation.Worlds.getMinHeight(world);
```

## Blocks

`Block#isPassable` is 1.13+:

```java
boolean passable = BlueFoundation.Blocks.isPassable(block);
```

`serialize` turns a block's state into a string. On 1.13+ that is `BlockData#getAsString()`, the
canonical form the matching restore path expects; older servers have no BlockData at all, so it
falls back to the namespaced material name, which is the most a legacy snapshot can carry:

```java
String state = BlueFoundation.Blocks.serialize(block); // "minecraft:oak_stairs[facing=north,...]"
```

Keep in mind the restore side is still 1.13-only: `Bukkit.createBlockData` has no legacy
equivalent, so a feature built on block snapshots needs its own version gate.

## Players

`Player#getPing` is 1.16+, `hidePlayer`/`showPlayer` changed to a `Plugin`-taking overload in 1.12,
and `getTargetBlockExact` is 1.13+:

```java
int ping = BlueFoundation.Players.getPing(player);

BlueFoundation.Players.hidePlayer(viewer, plugin, target);
BlueFoundation.Players.showPlayer(viewer, plugin, target);

Block looking = BlueFoundation.Players.targetBlock(player, 5);

BlueFoundation.Players.swingMainHand(player);
BlueFoundation.Players.swingOffHand(player);
```

`targetBlock` uses `getTargetBlockExact` on 1.13+ so modern behaviour is unchanged, and falls back
to `getTargetBlock(Set, int)` elsewhere, treating an air result as "nothing hit" to match the
modern contract. It returns `null` when there is no block within range.

Note these cover *players* only. Hiding an arbitrary entity from one viewer needs
`Player#hideEntity`, which is 1.18+ and has no legacy equivalent in the Bukkit API.

## Inventories

`InventoryView` was a class up to 1.20.6 and became an interface in 1.21. Bytecode compiled against
one side throws `IncompatibleClassChangeError` on the other, so the call has to be reflective — it
is not enough to just compile against the newest API:

```java
Inventory top = BlueFoundation.Inventories.getOpenTopInventory(player);
```

Returns `null` when the player has no view open or it cannot be read.
