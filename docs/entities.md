# Entities

`BlueFoundation.Entities` resolves renamed `EntityType` constants safely by trying multiple names, so code compiled against an old API keeps working on servers where the constant was renamed (and vice versa). Aliases are resolved bidirectionally: passing only the modern name still works on legacy servers, and only the legacy name works on modern ones.

```java
// EntityType.PRIMED_TNT was renamed to EntityType.TNT in 1.20.5
TNTPrimed tnt = (TNTPrimed) BlueFoundation.Entities.spawn(location, "TNT", "PRIMED_TNT");

EntityType type = BlueFoundation.Entities.require("ITEM", "DROPPED_ITEM");
boolean supported = BlueFoundation.Entities.isSupported("BLOCK_DISPLAY");
```

Known renames are covered out of the box (TNT, item drops, minecart variants, leash knots, firework rockets, piglins, mooshrooms, and more), so a single name is usually enough:

```java
EntityType tntType = BlueFoundation.Entities.require("TNT"); // resolves PRIMED_TNT on legacy servers
```

## Entity flags

Several entity flags arrived after 1.8 — `setSilent` and `setCollidable` in 1.9, `setAI` in 1.10,
`setAware` in 1.15 — so calling them directly breaks on older servers. These resolve the setter at
runtime and quietly do nothing where it is missing:

```java
BlueFoundation.Entities.setInvulnerable(entity, true);
BlueFoundation.Entities.setSilent(entity, true);
BlueFoundation.Entities.setAI(entity, false);
BlueFoundation.Entities.setCollidable(entity, false);
BlueFoundation.Entities.setPersistent(entity, false);
BlueFoundation.Entities.setGlowing(entity, true);
```

Each returns whether the flag was applied. For anything not covered by a named method, `setFlag`
and `getFlag` take the accessor name directly:

```java
BlueFoundation.Entities.setFlag(guardian, "setLaser", true);
boolean gliding = BlueFoundation.Entities.getFlag(player, "isGliding", false);
```

`getFlag` takes the value to return when the getter does not exist, so a flag that predates the
running version degrades to a sensible default instead of throwing.

## Passengers

`addPassenger` is 1.11+; before that a vehicle could only carry one passenger, through
`setPassenger`. This uses whichever exists:

```java
BlueFoundation.Entities.addPassenger(horse, player);
```

Note there is no matching `removePassenger` helper: `Entity#eject()` has existed since 1.8 and
drops every passenger, so use it directly when you want to clear them all.

## Per-viewer entity glow

Create one glow manager for your plugin, reuse it for every update, and close it
when the plugin disables:

```java
GlowManager glows = BlueFoundation.Entities.createGlowManager(plugin);

glows.setGlowing(target, viewer, ChatColor.RED);
glows.unsetGlowing(target, viewer);

glows.close();
```

The effect and its color are client-side for the selected viewer. Other players
do not see it, and normal entity metadata changes do not remove it.
