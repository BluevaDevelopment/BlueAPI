# Items

**Available on:** Bukkit ✅ only - no ItemStack concept on a proxy or (yet) mapped on Hytale

`BlueFoundation.Items` creates and edits Bukkit `ItemStack` instances. Display names and lore are MiniMessage-first, then serialized to legacy strings only because older Bukkit item metadata APIs require strings.

```java
ItemStack item = BlueFoundation.Items.builder("OAK_SIGN", "SIGN")
        .amount(1)
        .name("<gold>Game Selector</gold>")
        .lore("<gray>Right click to open")
        .glow()
        .hideAllFlags()
        .build();
```

Useful helpers:

```java
BlueFoundation.Items.name(item, "<green>Ready");
BlueFoundation.Items.lore(item, "<gray>Line one", "<yellow>Line two");
ItemStack copy = BlueFoundation.Items.builder(existingItem).name("<green>Copy").build();
BlueFoundation.Items.enchant(item, "sharpness", 1);
BlueFoundation.Items.unbreakable(item, true);
BlueFoundation.Items.customModelData(item, 1001);
BlueFoundation.Items.pdcString(item, "myplugin", "item_id", "selector");
String itemId = BlueFoundation.Items.pdcString(item, "myplugin", "item_id");
BlueFoundation.Items.skullTexture(item, "http://textures.minecraft.net/texture/...");
BlueFoundation.Items.skullValue(item, "{player}", player);
BlueFoundation.Items.editMeta(item, meta -> {
    // Add plugin-specific metadata without leaving the BlueFoundation item flow.
});
```

## Hands

The off hand arrived in 1.9, and with it `getItemInMainHand`/`getItemInOffHand`; 1.8 only has
`getItemInHand`. These pick whichever exists, for a `PlayerInventory` or any entity's
`EntityEquipment`:

```java
ItemStack held = BlueFoundation.Items.mainHandItem(player.getInventory());
BlueFoundation.Items.mainHandItem(player.getInventory(), item);

ItemStack offHand = BlueFoundation.Items.offHandItem(player.getInventory());
BlueFoundation.Items.offHandItem(armorStand.getEquipment(), banner);
```

`offHandItem` returns `null` on 1.8, where there is no off hand, and the setter is a no-op there.

## Persistent data

`PersistentDataContainer` is 1.14+ and `NamespacedKey` 1.12+, so the `pdc*` helpers take a plain
namespace and key instead. On older servers they fall back to a custom item tag, which means a
tagged item keeps resolving across the whole range:

```java
BlueFoundation.Items.pdcString(item, "myplugin", "item_id", "selector");
String itemId = BlueFoundation.Items.pdcString(item, "myplugin", "item_id");

BlueFoundation.Items.pdcInt(item, "myplugin", "uses", 3);
boolean tagged = BlueFoundation.Items.pdcHas(item, "myplugin", "item_id", "STRING");
BlueFoundation.Items.pdcRemove(item, "myplugin", "item_id");

String dump = BlueFoundation.Items.pdcDebug(item, "myplugin", "item_id", "STRING");
```

The namespace/key pair produces the same key a `NamespacedKey(plugin, key)` would, so switching
existing call sites over does not orphan items tagged by earlier builds. There is no helper to
enumerate a container's keys — that needs the 1.14+ API — so check the specific keys you write.

If your plugin already has Adventure components after placeholder processing,
use the component overloads to avoid serializing and parsing text twice:

```java
BlueFoundation.Items.name(item, titleComponent);
BlueFoundation.Items.loreComponents(item, loreComponents);
BlueFoundation.Items.loreSplit(item, "<gray>Line one\n<yellow>Line two");
```
