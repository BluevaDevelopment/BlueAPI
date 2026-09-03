package net.blueva.foundation.menus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A menu described once, for both editions.
 *
 * <p>Everything here is layout and content. Nothing in a definition knows
 * which player is looking at it or which page they are on - that lives in
 * {@link MenuContext} - so a definition is built once at load time and shared
 * by every player who opens it.</p>
 */
public final class MenuDefinition {

    private final String id;
    private final String title;
    private final List<String> content;
    private final MenuRenderMode mode;
    private final MenuLayout layout;
    private final FormStyle formStyle;
    private final String formIcon;
    private final int size;
    private final int pageSize;
    private final List<Integer> dynamicSlots;
    private final String emptyMessage;
    private final MenuButton decoration;
    private final Map<String, MenuButton> buttons;
    private final MenuButton template;
    private final MenuButton previousButton;
    private final MenuButton nextButton;
    private final MenuButton closeButton;
    private final String openSound;
    private final String clickSound;
    private final Map<String, Object> properties;

    private MenuDefinition(Builder builder) {
        this.id = builder.id;
        this.title = builder.title == null ? builder.id : builder.title;
        this.content = Collections.unmodifiableList(new ArrayList<String>(builder.content));
        this.mode = builder.mode;
        this.layout = builder.layout;
        this.formStyle = builder.formStyle;
        this.formIcon = builder.formIcon;
        this.size = normaliseSize(builder.size);
        this.pageSize = builder.pageSize > 0 ? builder.pageSize : 0;
        this.dynamicSlots = Collections.unmodifiableList(new ArrayList<Integer>(builder.dynamicSlots));
        this.emptyMessage = builder.emptyMessage;
        this.decoration = builder.decoration;
        this.buttons = Collections.unmodifiableMap(new LinkedHashMap<String, MenuButton>(builder.buttons));
        this.template = builder.template;
        this.previousButton = builder.previousButton;
        this.nextButton = builder.nextButton;
        this.closeButton = builder.closeButton;
        this.openSound = builder.openSound;
        this.clickSound = builder.clickSound;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.properties));
    }

    /**
     * @param id the menu's identifier, used by {@code open_menu} actions
     * @return a fresh builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Round a requested inventory size to something Bukkit will accept: a
     * multiple of nine between one and six rows.
     */
    private static int normaliseSize(int requested) {
        if (requested <= 0) {
            return 54;
        }
        int rows = (requested + 8) / 9;
        return Math.max(1, Math.min(6, rows)) * 9;
    }

    /**
     * @return the menu's identifier
     */
    public String id() {
        return id;
    }

    /**
     * @return the inventory or form title, in MiniMessage
     */
    public String title() {
        return title;
    }

    /**
     * @return the form's body lines; ignored by chest menus
     */
    public List<String> content() {
        return content;
    }

    /**
     * @return which surface this menu is drawn on
     */
    public MenuRenderMode mode() {
        return mode;
    }

    /**
     * @return whether this menu paginates
     */
    public MenuLayout layout() {
        return layout;
    }

    /**
     * @return which Bedrock form this becomes
     */
    public FormStyle formStyle() {
        return formStyle;
    }

    /**
     * @return the form's title-bar icon, or {@code null}
     */
    public String formIcon() {
        return formIcon;
    }

    /**
     * @return the inventory size in slots
     */
    public int size() {
        return size;
    }

    /**
     * How many dynamic entries fit on one page.
     *
     * <p>When not set explicitly, a chest menu uses however many dynamic slots
     * it has and a form uses ten, which is about what fits on a phone screen
     * without scrolling past the navigation.</p>
     *
     * @param forForm whether the caller is rendering a form
     * @return the page size, always at least one
     */
    public int pageSize(boolean forForm) {
        if (pageSize > 0) {
            return pageSize;
        }
        if (forForm) {
            return 10;
        }
        return dynamicSlots.isEmpty() ? Math.max(1, size - 9) : dynamicSlots.size();
    }

    /**
     * @return the slots dynamic entries are placed in; empty means every slot
     *         not taken by a declared button
     */
    public List<Integer> dynamicSlots() {
        return dynamicSlots;
    }

    /**
     * @return what to show when a paginated menu has no entries, or {@code null}
     */
    public String emptyMessage() {
        return emptyMessage;
    }

    /**
     * @return the filler item, or {@code null}
     */
    public MenuButton decoration() {
        return decoration;
    }

    /**
     * @return the declared buttons, in declaration order
     */
    public Map<String, MenuButton> buttons() {
        return buttons;
    }

    /**
     * The shape dynamic entries take when the caller supplies data rather than
     * ready-made buttons.
     *
     * @return the template button, or {@code null}
     */
    public MenuButton template() {
        return template;
    }

    /**
     * @return the previous-page button, or {@code null}
     */
    public MenuButton previousButton() {
        return previousButton;
    }

    /**
     * @return the next-page button, or {@code null}
     */
    public MenuButton nextButton() {
        return nextButton;
    }

    /**
     * @return the close button, or {@code null}
     */
    public MenuButton closeButton() {
        return closeButton;
    }

    /**
     * @return the sound played on open, or {@code null}
     */
    public String openSound() {
        return openSound;
    }

    /**
     * @return the sound played on click, or {@code null}
     */
    public String clickSound() {
        return clickSound;
    }

    /**
     * Anything the config carried that BlueFoundation does not itself
     * understand, so a plugin can keep its own keys in the same file.
     *
     * @return the extra properties
     */
    public Map<String, Object> properties() {
        return properties;
    }

    /**
     * @param key a property name
     * @return the value, or {@code null}
     */
    public Object property(String key) {
        return properties.get(key);
    }

    /**
     * @return a builder pre-filled with this menu's state
     */
    public Builder toBuilder() {
        Builder builder = new Builder(id);
        builder.title = title;
        builder.content.addAll(content);
        builder.mode = mode;
        builder.layout = layout;
        builder.formStyle = formStyle;
        builder.formIcon = formIcon;
        builder.size = size;
        builder.pageSize = pageSize;
        builder.dynamicSlots.addAll(dynamicSlots);
        builder.emptyMessage = emptyMessage;
        builder.decoration = decoration;
        builder.buttons.putAll(buttons);
        builder.template = template;
        builder.previousButton = previousButton;
        builder.nextButton = nextButton;
        builder.closeButton = closeButton;
        builder.openSound = openSound;
        builder.clickSound = clickSound;
        builder.properties.putAll(properties);
        return builder;
    }

    /** Builds a {@link MenuDefinition}. */
    public static final class Builder {
        private final String id;
        private String title;
        private final List<String> content = new ArrayList<String>();
        private MenuRenderMode mode = MenuRenderMode.AUTO;
        private MenuLayout layout = MenuLayout.STATIC;
        private FormStyle formStyle = FormStyle.SIMPLE;
        private String formIcon;
        private int size = 54;
        private int pageSize;
        private final List<Integer> dynamicSlots = new ArrayList<Integer>();
        private String emptyMessage;
        private MenuButton decoration;
        private final Map<String, MenuButton> buttons = new LinkedHashMap<String, MenuButton>();
        private MenuButton template;
        private MenuButton previousButton;
        private MenuButton nextButton;
        private MenuButton closeButton;
        private String openSound;
        private String clickSound;
        private final Map<String, Object> properties = new LinkedHashMap<String, Object>();

        Builder(String id) {
            this.id = id == null ? "" : id;
        }

        /**
         * @param title the inventory or form title, in MiniMessage
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * @param lines the form's body lines
         * @return this builder
         */
        public Builder content(Collection<String> lines) {
            content.clear();
            if (lines != null) {
                content.addAll(lines);
            }
            return this;
        }

        /**
         * @param mode which surface to draw on
         * @return this builder
         */
        public Builder mode(MenuRenderMode mode) {
            this.mode = mode == null ? MenuRenderMode.AUTO : mode;
            return this;
        }

        /**
         * @param layout whether the menu paginates
         * @return this builder
         */
        public Builder layout(MenuLayout layout) {
            this.layout = layout == null ? MenuLayout.STATIC : layout;
            return this;
        }

        /**
         * @param formStyle which Bedrock form this becomes
         * @return this builder
         */
        public Builder formStyle(FormStyle formStyle) {
            this.formStyle = formStyle == null ? FormStyle.SIMPLE : formStyle;
            return this;
        }

        /**
         * @param formIcon the form's title-bar icon
         * @return this builder
         */
        public Builder formIcon(String formIcon) {
            this.formIcon = formIcon;
            return this;
        }

        /**
         * @param size the inventory size in slots, rounded to a whole row
         * @return this builder
         */
        public Builder size(int size) {
            this.size = size;
            return this;
        }

        /**
         * @param rows the inventory height in rows
         * @return this builder
         */
        public Builder rows(int rows) {
            this.size = rows * 9;
            return this;
        }

        /**
         * @param pageSize how many dynamic entries fit on a page
         * @return this builder
         */
        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * @param slots the slots dynamic entries go into
         * @return this builder
         */
        public Builder dynamicSlots(Collection<Integer> slots) {
            dynamicSlots.clear();
            if (slots != null) {
                for (Integer slot : slots) {
                    if (slot != null) {
                        dynamicSlots.add(slot);
                    }
                }
            }
            return this;
        }

        /**
         * @param emptyMessage what to show when there are no entries
         * @return this builder
         */
        public Builder emptyMessage(String emptyMessage) {
            this.emptyMessage = emptyMessage;
            return this;
        }

        /**
         * @param decoration the filler item
         * @return this builder
         */
        public Builder decoration(MenuButton decoration) {
            this.decoration = decoration;
            return this;
        }

        /**
         * @param button a button to add, keyed by its own id
         * @return this builder
         */
        public Builder button(MenuButton button) {
            if (button != null) {
                buttons.put(button.id(), button);
            }
            return this;
        }

        /**
         * @param buttons buttons to add
         * @return this builder
         */
        public Builder buttons(Collection<MenuButton> buttons) {
            if (buttons != null) {
                for (MenuButton button : buttons) {
                    button(button);
                }
            }
            return this;
        }

        /**
         * @param template the shape dynamic entries take
         * @return this builder
         */
        public Builder template(MenuButton template) {
            this.template = template;
            return this;
        }

        /**
         * @param previousButton the previous-page button
         * @return this builder
         */
        public Builder previousButton(MenuButton previousButton) {
            this.previousButton = previousButton;
            return this;
        }

        /**
         * @param nextButton the next-page button
         * @return this builder
         */
        public Builder nextButton(MenuButton nextButton) {
            this.nextButton = nextButton;
            return this;
        }

        /**
         * @param closeButton the close button
         * @return this builder
         */
        public Builder closeButton(MenuButton closeButton) {
            this.closeButton = closeButton;
            return this;
        }

        /**
         * @param openSound the sound played on open
         * @return this builder
         */
        public Builder openSound(String openSound) {
            this.openSound = openSound;
            return this;
        }

        /**
         * @param clickSound the sound played on click
         * @return this builder
         */
        public Builder clickSound(String clickSound) {
            this.clickSound = clickSound;
            return this;
        }

        /**
         * @param key   a property name
         * @param value the value
         * @return this builder
         */
        public Builder property(String key, Object value) {
            if (key != null) {
                properties.put(key, value);
            }
            return this;
        }

        /**
         * @return the finished definition
         */
        public MenuDefinition build() {
            return new MenuDefinition(this);
        }
    }
}
