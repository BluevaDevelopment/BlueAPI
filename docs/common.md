# Common module

`BlueFoundation-Common` is the platform-agnostic core every other module depends on. You don't usually declare it directly - Gradle/Maven pull it in transitively when you depend on `BlueFoundation`, `BlueFoundation-Velocity`, `BlueFoundation-BungeeCord` or `BlueFoundation-Hytale`.

It holds the pieces that have nothing to do with any specific server/proxy API:

- **Dependencies core** - the Maven artifact downloader and classloader injector behind `BlueFoundation.Dependencies` on every platform. See [Runtime dependencies](dependencies.md).
- **Config engine** - the YAML/TOML/JSON parsing, diffing and update-policy logic behind `BlueFoundation.Configs`. See [Configs](configs.md).
- **Text engine** - BlueFoundation's own component tree (`BfComponent`/`BfStyle`/`BfColor`), a hand-rolled MiniMessage parser, and legacy/plain serializers. This is what `BlueFoundation.Text` parses through on every platform, and what each platform's `Messages`/`Items`/rendering code converts into that platform's own text type (Adventure `Component`, BungeeCord `BaseComponent[]`, Hytale `Message`). Legacy `&`/`§` input and MiniMessage tags both resolve to the same styling here, so there's no separate "legacy mode" to think about.
- **Adventure bridge** - a small `AdventureText` class that parses MiniMessage/legacy input straight into a real Adventure `Component`, for platforms/code paths where Adventure is guaranteed (Velocity) or available (Paper). Not part of the public `BlueFoundation.*` facade on any platform - each platform's `Messages`/`Items` decide internally when to reach for it, so callers only ever have one method to call regardless of what's actually rendering the text underneath.

Generic reflection helpers (`classExists`/`findClass`/`method`/`field`) are duplicated per platform module rather than shared, since each platform's `Reflection.craftBukkitClass`-style extras (where they exist) are platform-specific anyway.
