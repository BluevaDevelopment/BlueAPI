package net.blueva.foundation.menus;

import net.blueva.foundation.config.ConfigSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Reads a menu out of a config section.
 *
 * <p>The schema, in full:</p>
 *
 * <pre>{@code
 * title: "<gradient:#EC4899:#F43F5E>Warps</gradient>"
 * mode: auto             # auto | chest | form
 * layout: paginated      # static | paginated
 * size: 54               # or: rows: 6
 * page-size: 28
 * dynamic-slots: [10,11,12,13,14,15,16]
 * content:               # form body; ignored by chest menus
 *   - "<gray>Pick somewhere to go"
 * empty: "<red>No warps yet"
 * form:
 *   style: simple        # simple | modal
 *   icon: "https://example.com/icon.png"
 * sounds:
 *   open: BLOCK_CHEST_OPEN
 *   click: UI_BUTTON_CLICK
 *
 * decoration:
 *   material: GRAY_STAINED_GLASS_PANE
 *   name: " "
 *   slots: [0,1,2,3,4,5,6,7,8]
 *
 * items:
 *   spawn:
 *     name: "<green>Spawn"
 *     lore: ["<gray>Go home"]
 *     material: "PLAYER_HEAD, SKULL_ITEM"    # first that exists on this server
 *     slot: 20
 *     image: "https://example.com/spawn.png" # Bedrock button icon
 *     visible-if: "%some_placeholder%"
 *     actions:
 *       - "command: spawn"
 *       - type: message
 *         value: "<green>Off you go"
 *         click: RIGHT
 *
 * template:              # the shape entries take in a paginated menu
 *   name: "<aqua>{name}"
 *   material: ENDER_PEARL
 *   actions: ["command: warp {name}"]
 *
 * navigation:
 *   previous: { material: ARROW, name: "<yellow>Back",  slot: 45, actions: ["page: previous"] }
 *   next:     { material: ARROW, name: "<yellow>Next",  slot: 53, actions: ["page: next"] }
 *   close:    { material: BARRIER, name: "<red>Close", slot: 49, actions: ["close"] }
 * }</pre>
 *
 * <p>Every key is optional. A section may also be wrapped in a top-level
 * {@code menu:} key, which is unwrapped here so a file can carry a menu
 * alongside a plugin's own settings.</p>
 */
public final class MenuLoader {

    private MenuLoader() {
    }

    /**
     * @param id      the menu's identifier; when {@code null} the section's own
     *                {@code id} key is used
     * @param section the config
     * @return the parsed menu
     */
    public static MenuDefinition load(String id, ConfigSection section) {
        if (section == null) {
            throw new IllegalArgumentException("section cannot be null");
        }
        ConfigSection root = section;
        ConfigSection wrapped = section.section("menu");
        if (wrapped != null && wrapped.exists()) {
            root = wrapped;
        }

        String menuId = id != null && !id.isEmpty() ? id : root.getString("id", "");
        MenuDefinition.Builder builder = MenuDefinition.builder(menuId)
                .title(root.getString("title", menuId))
                .mode(MenuRenderMode.parse(root.getString("mode")))
                .layout(MenuLayout.parse(root.getString("layout")))
                .content(root.getStringList("content"))
                .emptyMessage(root.contains("empty") ? root.getString("empty") : null)
                .pageSize(root.getInt("page-size", root.getInt("pageSize", 0)))
                .dynamicSlots(root.getIntList("dynamic-slots"));

        if (root.contains("rows")) {
            builder.rows(root.getInt("rows", 6));
        } else {
            builder.size(root.getInt("size", 54));
        }
        if (root.getIntList("dynamic-slots").isEmpty()) {
            builder.dynamicSlots(root.getIntList("dynamicSlots"));
        }

        ConfigSection form = root.section("form");
        if (form != null && form.exists()) {
            builder.formStyle(FormStyle.parse(form.getString("style")));
            builder.formIcon(form.getString("icon", null));
        }

        ConfigSection sounds = root.section("sounds");
        if (sounds != null && sounds.exists()) {
            builder.openSound(sounds.getString("open", null));
            builder.clickSound(sounds.getString("click", null));
        }

        ConfigSection decoration = root.section("decoration");
        if (decoration != null && decoration.exists()) {
            builder.decoration(button("decoration", decoration));
        }

        ConfigSection items = root.section("items");
        if (items != null && items.exists()) {
            for (String key : items.keys()) {
                ConfigSection item = items.section(key);
                if (item != null && item.exists()) {
                    builder.button(button(key, item));
                }
            }
        }

        ConfigSection template = root.section("template");
        if (template != null && template.exists()) {
            builder.template(button("template", template));
        }

        ConfigSection navigation = root.section("navigation");
        if (navigation != null && navigation.exists()) {
            builder.previousButton(navigationButton(navigation, "previous", "page: previous"));
            builder.nextButton(navigationButton(navigation, "next", "page: next"));
            builder.closeButton(navigationButton(navigation, "close", "close"));
        }

        for (String key : root.keys()) {
            if (!KNOWN.contains(key)) {
                builder.property(key, root.get(key));
            }
        }
        return builder.build();
    }

    private static final List<String> KNOWN = Arrays.asList(
            "id", "title", "mode", "layout", "content", "empty", "size", "rows",
            "page-size", "pageSize", "dynamic-slots", "dynamicSlots",
            "form", "sounds", "decoration", "items", "template", "navigation");

    /**
     * Navigation buttons get their action for free, because a next button that
     * does not go to the next page is a config mistake nobody meant to make.
     * An explicit {@code actions} list still wins.
     */
    private static MenuButton navigationButton(ConfigSection navigation, String key, String defaultAction) {
        ConfigSection section = navigation.section(key);
        if (section == null || !section.exists()) {
            return null;
        }
        MenuButton button = button(key, section);
        if (!button.actions().isEmpty()) {
            return button;
        }
        return button.toBuilder().action(defaultAction).build();
    }

    /**
     * Parse a single button.
     *
     * @param id      the button's key within its menu
     * @param section the button's section
     * @return the button
     */
    public static MenuButton button(String id, ConfigSection section) {
        MenuButton.Builder builder = MenuButton.builder(id)
                .name(section.getString("name", ""))
                .lore(section.getStringList("lore"))
                .amount(section.getInt("amount", 1))
                .skull(section.getString("skull", null))
                .image(section.getString("image", null))
                .glow(section.getBoolean("glow", false))
                .hideFlags(section.getBoolean("hide-flags", section.getBoolean("hideFlags", false)))
                .enchantments(section.getStringList("enchantments"))
                .visibleIf(section.contains("visible-if")
                        ? section.getString("visible-if")
                        : section.getString("visible_if", null))
                .material(materials(section))
                .actions(actions(section));

        List<Integer> slots = section.getIntList("slots");
        if (!slots.isEmpty()) {
            builder.slots(slots);
        } else if (section.contains("slot")) {
            builder.slot(section.getInt("slot", -1));
        }
        return builder.build();
    }

    /**
     * A material may be a single name, a comma-separated list or a YAML list.
     * All three mean the same thing: try each in turn and take the first that
     * exists on this server, which is how one config survives 1.8 through
     * modern without a version branch.
     */
    private static List<String> materials(ConfigSection section) {
        List<String> names = new ArrayList<String>();
        List<String> listed = section.getStringList("material");
        if (!listed.isEmpty()) {
            for (String entry : listed) {
                split(entry, names);
            }
            return names;
        }
        split(section.getString("material", null), names);
        return names;
    }

    private static void split(String value, List<String> into) {
        if (value == null) {
            return;
        }
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                into.add(trimmed);
            }
        }
    }

    /**
     * Actions accept both the short string form and the mapping form, in the
     * same list, because a config that has grown for a while will have both.
     */
    private static Collection<MenuAction> actions(ConfigSection section) {
        List<MenuAction> actions = new ArrayList<MenuAction>();
        List<Object> raw = section.getList("actions");
        if (raw == null) {
            return actions;
        }
        for (Object entry : raw) {
            if (entry == null) {
                continue;
            }
            if (entry instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) entry;
                MenuActionType type = MenuActionType.parse(string(map.get("type")));
                actions.add(new MenuAction(type, string(map.get("value")), string(map.get("click"))));
            } else {
                actions.add(MenuAction.parse(entry.toString()));
            }
        }
        return actions;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }
}
