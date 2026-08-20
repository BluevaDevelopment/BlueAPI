# Text and messages

**Available on:** Bukkit ✅ · Velocity ✅ native Adventure · BungeeCord ✅ via BaseComponent · Hytale ✅ via Message - see the [feature matrix](../README.md#feature-matrix) for what differs per platform

`BlueFoundation.Text` parses MiniMessage - legacy `&`/`§` codes are accepted too, resolving to the same styling as the equivalent tag would (`&c` and `<red>` produce an identical result). There is only this one call: whether the actual rendering ends up using Adventure natively (Paper, Velocity) or BlueFoundation's own engine (Spigot, BungeeCord, Hytale) is resolved internally per server, not something callers choose between.

```java
BfComponent title = BlueFoundation.Text.parse("<gold>Victory!</gold>");
String plain = BlueFoundation.Text.plain("<gold>Victory!</gold>");
String inventoryTitle = BlueFoundation.Text.legacySection("<gold>Victory!</gold>");
```

`BlueFoundation.Messages` sends text to players, command senders, action bars, titles and boss bars, using the same MiniMessage/legacy input as `Text`. On Paper it delivers through a native Adventure `Audience`; everywhere else it falls back to legacy Bukkit APIs - both paths are internal to `Messages`, never a choice the caller makes.

```java
BlueFoundation.Messages.send(player, "<green>Hello!");
BlueFoundation.Messages.actionBar(player, "<yellow>Action bar text");
BlueFoundation.Messages.title(player, "<aqua>Title", "<gray>Subtitle", 10, 70, 20);
```
