package net.blueva.foundation.sounds;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

/**
 * Plays sound events registered in Hytale's own asset map, through the
 * server's {@link SoundUtil} pipeline. No cue/cooldown/registry layer is
 * included here - that belongs to whatever game logic calls this, not to a
 * foundation library.
 *
 * <p>{@code eventId} is a sound event id as defined by the loaded asset packs
 * (e.g. base-game sound events, or a custom pack's own); see
 * {@link net.blueva.foundation.music.MusicManager} for note-by-note MIDI
 * playback built on top of the same asset-map lookup.</p>
 */
@SuppressWarnings("removal") // Player#getPlayerRef() is marked for removal in this server build
// with no documented replacement yet - Hytale's plugin API is still young (see README).
public class Sounds {

    protected Sounds() {
    }

    /** Resolves a sound event id to its asset-map index, or a negative number if unknown. */
    public static int resolveIndex(String eventId) {
        try {
            return SoundEvent.getAssetMap().getIndex(eventId);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** Plays a sound event to one player only, at that player's current position. */
    public static void play(Player player, String eventId, SoundCategory category, float volume, float pitch) {
        int index = resolveIndex(eventId);
        if (index < 0) {
            return;
        }
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        world.execute(() -> {
            EntityStore entityStore = world.getEntityStore();
            PlayerRef playerRef = player.getPlayerRef();
            Ref<EntityStore> ref = playerRef == null ? null : playerRef.getReference();
            Vector3d position = position(entityStore, ref);
            if (ref != null) {
                SoundUtil.playSoundEvent3dToPlayer(ref, index, category, position.x, position.y, position.z, volume, pitch, entityStore.getStore());
            } else {
                SoundUtil.playSoundEvent3d(index, category, position.x, position.y, position.z, volume, pitch, entityStore.getStore());
            }
        });
    }

    /** Plays a sound event at a fixed world position, audible to every nearby player. */
    public static void playAt(World world, double x, double y, double z, String eventId, SoundCategory category, float volume, float pitch) {
        int index = resolveIndex(eventId);
        if (index < 0) {
            return;
        }
        world.execute(() -> SoundUtil.playSoundEvent3d(index, category, x, y, z, volume, pitch, world.getEntityStore().getStore()));
    }

    private static Vector3d position(EntityStore entityStore, Ref<EntityStore> ref) {
        if (ref == null) {
            return new Vector3d(0.0, 0.0, 0.0);
        }
        TransformComponent transform = entityStore.getStore().getComponent(ref, TransformComponent.getComponentType());
        return transform != null ? transform.getPosition() : new Vector3d(0.0, 0.0, 0.0);
    }
}
