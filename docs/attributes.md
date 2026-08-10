# Attributes

`BlueFoundation.Attributes` reads and writes entity attributes across every version.
`org.bukkit.attribute.Attribute` only exists from 1.9, and its constants were renamed twice:
`GENERIC_MAX_HEALTH` through 1.21.2, `MAX_HEALTH` afterwards, when the type also stopped being an
enum and became a registry-backed `Keyed`. You pass names, not constants.

```java
LivingEntity entity = player;

Double base = BlueFoundation.Attributes.getBaseValue(entity, "MAX_HEALTH", "GENERIC_MAX_HEALTH");
BlueFoundation.Attributes.setBaseValue(entity, 40.0, "MAX_HEALTH", "GENERIC_MAX_HEALTH");

boolean supported = BlueFoundation.Attributes.isSupported("SCALE", "GENERIC_SCALE");
```

`getBaseValue` returns `null` when the entity or the attribute is unavailable, so the absence of an
attribute is distinguishable from a real value of zero.

Names are tried in order, so the modern spelling first and the legacy one as the fallback. Lookup
covers both eras in one step: the constants are public static fields whether `Attribute` is an enum
or an interface, and a registry lookup (`max_health` and `generic.max_health`) backs it up.

## Max health

Max health gets dedicated methods because 1.8 has no attribute API at all and uses the
`LivingEntity` health methods instead. These pick whichever exists:

```java
double max = BlueFoundation.Attributes.getMaxHealth(player);
BlueFoundation.Attributes.setMaxHealth(player, 20.0);
```

`getMaxHealth` returns `0` when the entity is `null` or the value cannot be read;
`setMaxHealth` returns whether it was applied.

## Resolving the constant

`match` returns the constant as an opaque `Object`, since the type cannot appear in a signature
compiled against 1.8:

```java
Object attribute = BlueFoundation.Attributes.match("MAX_HEALTH", "GENERIC_MAX_HEALTH");
```

On a server with no attribute API this returns `null`, which is how the max-health helpers know to
take the legacy path.
