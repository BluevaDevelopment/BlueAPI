package net.blueva.foundation.menus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Marks an inventory as a BlueFoundation menu and carries what the click
 * listener needs to route a click.
 *
 * <p>Identifying menus by their holder rather than by their title is what lets
 * two plugins run menus side by side without either one cancelling the
 * other's clicks, and it survives a title with placeholders in it.</p>
 */
public final class MenuHolder implements InventoryHolder {

    private final Plugin plugin;
    private final MenuContext context;
    private final Map<Integer, MenuButton> slots = new HashMap<Integer, MenuButton>();
    private Inventory inventory;

    MenuHolder(Plugin plugin, MenuContext context) {
        this.plugin = plugin;
        this.context = context;
    }

    /**
     * @return the plugin that opened this menu
     */
    public Plugin plugin() {
        return plugin;
    }

    /**
     * @return the view this inventory is drawing
     */
    public MenuContext context() {
        return context;
    }

    /**
     * @param slot an inventory slot
     * @return the button drawn there, or {@code null}
     */
    public MenuButton buttonAt(int slot) {
        return slots.get(Integer.valueOf(slot));
    }

    void bind(int slot, MenuButton button) {
        slots.put(Integer.valueOf(slot), button);
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
