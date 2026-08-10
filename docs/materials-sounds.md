# Materials and sounds

`BlueFoundation.Materials` and `BlueFoundation.Sounds` resolve renamed constants safely by trying multiple names. Sounds also accept modern namespaced keys on servers that expose the Bukkit sound registry.

```java
Material oakSign = BlueFoundation.Materials.require("OAK_SIGN", "SIGN");
Sound levelUp = BlueFoundation.Sounds.require("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
Sound pling = BlueFoundation.Sounds.require("minecraft:block.note_block.pling", "NOTE_PLING");

BlueFoundation.Sounds.play(player, 1.0F, 1.0F, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
```

## Material checks

`Material#isAir()` and `Material#isItem()` are both 1.13+, so calling them directly breaks on older
servers:

```java
if (BlueFoundation.Materials.isAir(block.getType())) {
    return;
}

if (BlueFoundation.Materials.isItem(material)) {
    player.getInventory().addItem(new ItemStack(material));
}
```

`isAir` matches on the name, so it covers `AIR` everywhere plus `CAVE_AIR` and `VOID_AIR` where
they exist, and treats `null` as air. `isItem` answers `true` on pre-flattening servers, where every
material had an item form.
