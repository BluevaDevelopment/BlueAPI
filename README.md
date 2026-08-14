<p align="center">
  <img src="docs/assets/bluefoundation-logo.png" alt="BlueFoundation" width="760">
</p>

<p align="center">
  <strong>A lightweight API foundation for Minecraft plugins.</strong>
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-26.26-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-8+-ED8B00?logo=openjdk&logoColor=white">
  <img alt="Build" src="https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white">
</p>

BlueFoundation focuses on keeping reusable plugin infrastructure small, explicit, and easy to access from a single namespace:

```java
import net.blueva.foundation.BlueFoundation;
```

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.blueva.net/releases")
}

dependencies {
    compileOnly("net.blueva.foundation:BlueFoundation:26.26")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>blueva</id>
        <url>https://repo.blueva.net/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.blueva.foundation</groupId>
        <artifactId>BlueFoundation</artifactId>
        <version>26.26</version>
    </dependency>
</dependencies>
```

repo.blueva.net is a public Maven repository, no authentication needed to depend on it.

## API structure

`BlueFoundation` is a small facade. Implementations live in dedicated packages, while public aliases keep usage centralized under `BlueFoundation.*`. Event wrappers are also split into dedicated interfaces/adapters internally.

```java
BlueFoundation.Dependencies
BlueFoundation.Version
BlueFoundation.Reflection
BlueFoundation.Materials
BlueFoundation.Items
BlueFoundation.AdventureItems
BlueFoundation.Sounds
BlueFoundation.Music
BlueFoundation.Entities
BlueFoundation.Scheduler
BlueFoundation.Commands
BlueFoundation.Messages
BlueFoundation.Text
BlueFoundation.AdventureText
BlueFoundation.Events
BlueFoundation.Configs
BlueFoundation.NPCs
BlueFoundation.Players
BlueFoundation.Worlds
BlueFoundation.Blocks
BlueFoundation.Particles
BlueFoundation.Attributes
BlueFoundation.GameRules
BlueFoundation.BossBars
BlueFoundation.Inventories
BlueFoundation.Scoreboards
```

## Documentation

- [Runtime dependencies](docs/dependencies.md): `BlueFoundation.Dependencies`
- [Version utilities](docs/version.md): `BlueFoundation.Version`
- [Reflection helpers](docs/reflection.md): `BlueFoundation.Reflection`
- [Materials and sounds](docs/materials-sounds.md): `BlueFoundation.Materials`, `BlueFoundation.Sounds`
- [Music](docs/music.md): `BlueFoundation.Music`
- [Entities](docs/entities.md): `BlueFoundation.Entities`
- [Items](docs/items.md): `BlueFoundation.Items`
- [Text and messages](docs/text-messages.md): `BlueFoundation.Text`, `BlueFoundation.Messages`
- [Scheduler](docs/scheduler.md): `BlueFoundation.Scheduler`
- [Commands](docs/commands.md): `BlueFoundation.Commands`
- [Configs](docs/configs.md): `BlueFoundation.Configs`
- [NPCs](docs/npcs.md): `BlueFoundation.NPCs`
- [Scoreboard](docs/scoreboards.md): `BlueFoundation.Scoreboards`
- [Multi-version events](docs/events.md): `BlueFoundation.Events`
- [Particles](docs/particles.md): `BlueFoundation.Particles`
- [Attributes](docs/attributes.md): `BlueFoundation.Attributes`
- [Game rules](docs/gamerules.md): `BlueFoundation.GameRules`
- [Boss bars](docs/bossbars.md): `BlueFoundation.BossBars`
- [Worlds, blocks, players and inventories](docs/world-players.md): `BlueFoundation.Worlds`, `BlueFoundation.Blocks`, `BlueFoundation.Players`, `BlueFoundation.Inventories`

## Goal

BlueFoundation aims to be a common foundation for Blueva plugins: small, clear, and ready to grow with multiversion tools and reusable systems for Bukkit/Spigot/Paper development.
