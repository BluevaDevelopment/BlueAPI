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
