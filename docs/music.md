# Music

BlueFoundation includes per-player NBS and MIDI playback without requiring an
additional server plugin.

```java
MusicManager music = BlueFoundation.Music.create(plugin);

MusicPlayback playback = music.play(
        player,
        new File(plugin.getDataFolder(), "music/victory.nbs"),
        MusicOptions.defaults().speed(1.25).volume(0.8F)
);
```

Supported file extensions:

- `.nbs`
- `.mid`
- `.midi`

NBS playback honors the song tempo, mid-song tempo changers, finite or
continuous loop metadata, layer state, volume, stereo panning, fine pitch, and
custom-instrument base pitch. MIDI tempo changes are also preserved.

Playback speed can be changed while a track is playing:

```java
playback.setSpeed(1.5);
playback.pause();
playback.resume();
playback.stop();
```

The manager parses files asynchronously, caches unchanged tracks, and uses
player-owned scheduling on Folia. High-tempo tracks keep their intended duration
by playing every note that became due during the current server tick.

Only one track is active per player. Starting another track replaces the
previous one. Close the manager when the owning plugin is disabled:

```java
music.close();
```
