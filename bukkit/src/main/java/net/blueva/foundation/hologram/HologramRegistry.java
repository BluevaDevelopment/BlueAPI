package net.blueva.foundation.hologram;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Internal registry of active holograms. */
final class HologramRegistry {

    private static final Set<HologramImpl> ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private HologramRegistry() {
    }

    static void register(HologramImpl hologram) {
        ACTIVE.add(hologram);
    }

    static void unregister(HologramImpl hologram) {
        ACTIVE.remove(hologram);
    }

    static Collection<HologramImpl> all() {
        return Collections.unmodifiableCollection(ACTIVE);
    }

    static void clear() {
        ACTIVE.clear();
    }
}
