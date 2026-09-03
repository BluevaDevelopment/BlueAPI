package net.blueva.foundation.menus.dialogs;

import net.blueva.foundation.messages.Messages;
import net.blueva.foundation.reflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Draws dialogs through Paper's API,
 * {@code io.papermc.paper.registry.data.dialog}.
 *
 * <p>Paper does not ship Spigot's {@code net.md-5:bungeecord-dialog}, so on a
 * Paper server this is the only backend that works, and on a Spigot one it is
 * absent entirely.</p>
 *
 * <p>The awkward part is {@code Dialog.create}, which takes a consumer of a
 * registry builder factory rather than a plain constructor. Because that
 * consumer is a {@link Consumer}, it can be a lambda whose parameter is
 * {@link Object} and whose body reflects - no proxy needed.</p>
 */
final class PaperDialogBackend implements DialogBackend {

    private static final String DATA = "io.papermc.paper.registry.data.dialog.";

    private volatile boolean resolved;
    private volatile boolean usable;

    private Method showDialog;
    private Method closeDialog;
    private Method dialogCreate;
    private Method factoryEmpty;
    private Method builderBase;
    private Method builderType;

    private Method legacySerializer;
    private Method deserialize;
    private Method keyOf;

    private Method dialogBaseCreate;
    private Method plainMessage;
    private Method actionButtonCreate;
    private Method customClick;
    private Method staticAction;
    private Method notice;
    private Method confirmation;
    private Method multiAction;

    private Method inputText;
    private Method inputBool;
    private Method inputNumberRange;
    private Method inputSingleOption;
    private Method optionEntryCreate;
    private Method multilineCreate;

    private Method clickRunCommand;
    private Method clickSuggestCommand;
    private Method clickOpenUrl;
    private Method clickCopyToClipboard;

    @Override
    public String name() {
        return "paper";
    }

    @Override
    public boolean available() {
        return resolve();
    }

    @Override
    public boolean show(Plugin plugin, Player player, Dialog dialog) {
        if (!resolve()) {
            return false;
        }
        listen(plugin);
        try {
            Object built = build(plugin, dialog);
            if (built == null) {
                return false;
            }
            showDialog.invoke(player, built);
            return true;
        } catch (Throwable e) {
            plugin.getLogger().warning("[BlueFoundation] Could not show dialog through the Paper API: " + e);
            return false;
        }
    }

    @Override
    public boolean clear(Player player) {
        if (!resolve() || closeDialog == null) {
            return false;
        }
        try {
            closeDialog.invoke(player);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private Object build(Plugin plugin, Dialog dialog) throws Exception {
        List<Object> bodies = new ArrayList<Object>();
        for (String line : dialog.body()) {
            bodies.add(plainMessage.invoke(null, component(line), Integer.valueOf(200)));
        }
        List<Object> inputs = new ArrayList<Object>();
        for (DialogInput input : dialog.inputs()) {
            Object built = input(input);
            if (built != null) {
                inputs.add(built);
            }
        }

        final Object base = dialogBaseCreate.invoke(null,
                component(dialog.title()),
                dialog.externalTitle() == null ? null : component(dialog.externalTitle()),
                Boolean.valueOf(dialog.canCloseWithEscape()),
                Boolean.valueOf(dialog.pause()),
                afterAction(dialog.afterAction()),
                bodies,
                inputs);
        final Object type = type(plugin, dialog);
        if (base == null || type == null) {
            return null;
        }

        // Dialog.create hands the lambda a factory; empty() opens a builder,
        // and the builder wants the base and the type.
        Consumer<Object> factoryConsumer = new Consumer<Object>() {
            @Override
            public void accept(Object factory) {
                try {
                    Object builder = factoryEmpty.invoke(factory);
                    builderBase.invoke(builder, base);
                    builderType.invoke(builder, type);
                } catch (Throwable e) {
                    throw new IllegalStateException("Could not populate the Paper dialog builder", e);
                }
            }
        };
        return dialogCreate.invoke(null, factoryConsumer);
    }

    private Object type(Plugin plugin, Dialog dialog) throws Exception {
        List<DialogButton> buttons = dialog.buttons();
        switch (dialog.type()) {
            case NOTICE:
                return notice.invoke(null, buttons.isEmpty() ? null : button(plugin, dialog, buttons.get(0)));
            case CONFIRMATION:
                if (buttons.size() < 2) {
                    throw new IllegalStateException("A confirmation dialog needs two buttons");
                }
                return confirmation.invoke(null,
                        button(plugin, dialog, buttons.get(0)),
                        button(plugin, dialog, buttons.get(1)));
            case MULTI_ACTION:
            default:
                List<Object> actions = new ArrayList<Object>();
                for (DialogButton button : buttons) {
                    actions.add(button(plugin, dialog, button));
                }
                return multiAction.invoke(null, actions,
                        dialog.exitButton() == null ? null : button(plugin, dialog, dialog.exitButton()),
                        Integer.valueOf(dialog.columns()));
        }
    }

    private Object button(Plugin plugin, Dialog dialog, DialogButton button) throws Exception {
        return actionButtonCreate.invoke(null,
                component(button.label()),
                button.tooltip() == null ? null : component(button.tooltip()),
                Integer.valueOf(button.width()),
                action(plugin, dialog, button.action()));
    }

    private Object action(Plugin plugin, Dialog dialog, DialogAction action) throws Exception {
        switch (action.kind()) {
            case RUN_COMMAND:
                return staticAction.invoke(null, clickRunCommand.invoke(null, withSlash(action.value())));
            case SUGGEST_COMMAND:
                return staticAction.invoke(null, clickSuggestCommand.invoke(null, withSlash(action.value())));
            case OPEN_URL:
                return staticAction.invoke(null, clickOpenUrl.invoke(null, action.value()));
            case COPY_TO_CLIPBOARD:
                return staticAction.invoke(null, clickCopyToClipboard.invoke(null, action.value()));
            case CALLBACK:
                String id = DialogCallbacks.register(plugin, dialog, action);
                int colon = id.indexOf(':');
                Object key = keyOf.invoke(null, id.substring(0, colon), id.substring(colon + 1));
                return customClick.invoke(null, key, null);
            case NONE:
            default:
                return null;
        }
    }

    private Object input(DialogInput input) throws Exception {
        switch (input.kind()) {
            case TEXT:
                Object multiline = null;
                if ((input.maxLines() != null || input.height() != null) && multilineCreate != null) {
                    multiline = multilineCreate.invoke(null, input.maxLines(), input.height());
                }
                return inputText.invoke(null, input.key(), Integer.valueOf(input.width()),
                        component(input.label()), Boolean.valueOf(!input.label().isEmpty()),
                        input.initialText(), Integer.valueOf(input.maxLength()), multiline);
            case BOOLEAN:
                return inputBool.invoke(null, input.key(), component(input.label()),
                        Boolean.valueOf(input.initialBoolean()), input.onTrue(), input.onFalse());
            case NUMBER_RANGE:
                return inputNumberRange.invoke(null, input.key(), Integer.valueOf(input.width()),
                        component(input.label()), input.labelFormat(),
                        Float.valueOf(input.start()), Float.valueOf(input.end()),
                        input.initialNumber(), input.step());
            case SINGLE_OPTION:
            default:
                List<Object> options = new ArrayList<Object>();
                for (DialogInput.Option option : input.options()) {
                    options.add(optionEntryCreate.invoke(null, option.id(),
                            component(option.label()), Boolean.valueOf(option.initial())));
                }
                return inputSingleOption.invoke(null, input.key(), Integer.valueOf(input.width()),
                        options, component(input.label()), Boolean.valueOf(!input.label().isEmpty()));
        }
    }

    private Object afterAction(DialogAfterAction afterAction) {
        Class<?> type = Reflection.findClass(DATA + "DialogBase$DialogAfterAction");
        if (type == null) {
            return null;
        }
        Object[] constants = type.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant instanceof Enum && afterAction.name().equals(((Enum<?>) constant).name())) {
                return constant;
            }
        }
        return null;
    }

    /**
     * Adventure is on every Paper server, so MiniMessage is rendered to legacy
     * codes by BlueFoundation's own parser and handed to Adventure's legacy
     * deserializer - one conversion, and no dependency on which Adventure
     * version Paper happens to bundle.
     */
    private Object component(String miniMessage) throws Exception {
        String legacy = Messages.legacySection(miniMessage == null ? "" : miniMessage);
        Object serializer = legacySerializer.invoke(null);
        return deserialize.invoke(serializer, legacy);
    }

    private void listen(Plugin plugin) {
        DialogCallbacks.listen(plugin, "io.papermc.paper.event.player.PlayerCustomClickEvent",
                new EventExecutor() {
                    @Override
                    public void execute(org.bukkit.event.Listener listener, Event event) {
                        handle(event);
                    }
                });
    }

    private void handle(Event event) {
        try {
            Object identifier = event.getClass().getMethod("getIdentifier").invoke(event);
            if (identifier == null) {
                return;
            }
            Player player = playerOf(event);
            if (player == null) {
                return;
            }
            final Object view = event.getClass().getMethod("getDialogResponseView").invoke(event);
            DialogCallbacks.fire(identifier.toString(), player, new DialogCallbacks.ResponseSource() {
                @Override
                public String read(DialogInput input) {
                    return readValue(view, input);
                }
            });
        } catch (Throwable e) {
            // A Paper build whose event moved is not worth crashing a click over.
        }
    }

    /**
     * Paper's response view has no "give me everything" call, only typed
     * getters keyed by name - which is fine, because the dialog we built is
     * the one telling us which names to ask for.
     */
    private String readValue(Object view, DialogInput input) {
        if (view == null) {
            return null;
        }
        try {
            switch (input.kind()) {
                case BOOLEAN:
                    Object bool = view.getClass().getMethod("getBoolean", String.class)
                            .invoke(view, input.key());
                    return bool == null ? null : bool.toString();
                case NUMBER_RANGE:
                    Object number = view.getClass().getMethod("getFloat", String.class)
                            .invoke(view, input.key());
                    return number == null ? null : number.toString();
                case TEXT:
                case SINGLE_OPTION:
                default:
                    Object text = view.getClass().getMethod("getText", String.class)
                            .invoke(view, input.key());
                    return text == null ? null : text.toString();
            }
        } catch (Throwable e) {
            return null;
        }
    }

    private static Player playerOf(Event event) {
        try {
            Object direct = event.getClass().getMethod("getPlayer").invoke(event);
            if (direct instanceof Player) {
                return (Player) direct;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object connection = event.getClass().getMethod("getCommonConnection").invoke(event);
            if (connection == null) {
                return null;
            }
            Object player = connection.getClass().getMethod("getPlayer").invoke(connection);
            return player instanceof Player ? (Player) player : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private static String withSlash(String command) {
        String trimmed = command == null ? "" : command.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private boolean resolve() {
        if (resolved) {
            return usable;
        }
        synchronized (this) {
            if (resolved) {
                return usable;
            }
            resolved = true;
            usable = resolve0();
            return usable;
        }
    }

    private boolean resolve0() {
        Class<?> dialogClass = Reflection.findClass("io.papermc.paper.dialog.Dialog");
        Class<?> component = Reflection.findClass("net.kyori.adventure.text.Component");
        Class<?> keyClass = Reflection.findClass("net.kyori.adventure.key.Key");
        Class<?> clickEvent = Reflection.findClass("net.kyori.adventure.text.event.ClickEvent");
        if (dialogClass == null || component == null || keyClass == null || clickEvent == null) {
            return false;
        }

        showDialog = Reflection.method(Player.class, "showDialog", dialogClass);
        if (showDialog == null) {
            return false;
        }
        closeDialog = Reflection.method(Player.class, "closeDialog");

        dialogCreate = Reflection.method(dialogClass, "create", Consumer.class);
        Class<?> factory = Reflection.findClass("io.papermc.paper.registry.RegistryBuilderFactory");
        Class<?> entryBuilder = Reflection.findClass(DATA + "DialogRegistryEntry$Builder");
        Class<?> base = Reflection.findClass(DATA + "DialogBase");
        Class<?> dialogType = Reflection.findClass(DATA + "type.DialogType");
        Class<?> actionButton = Reflection.findClass(DATA + "ActionButton");
        Class<?> dialogAction = Reflection.findClass(DATA + "action.DialogAction");
        Class<?> body = Reflection.findClass(DATA + "body.DialogBody");
        Class<?> inputClass = Reflection.findClass(DATA + "input.DialogInput");
        Class<?> afterActionClass = Reflection.findClass(DATA + "DialogBase$DialogAfterAction");
        if (factory == null || entryBuilder == null || base == null || dialogType == null
                || actionButton == null || dialogAction == null || body == null
                || inputClass == null || afterActionClass == null) {
            return false;
        }

        factoryEmpty = Reflection.method(factory, "empty");
        builderBase = Reflection.method(entryBuilder, "base", base);
        builderType = Reflection.method(entryBuilder, "type", dialogType);

        Class<?> legacy = Reflection.findClass(
                "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
        if (legacy == null) {
            return false;
        }
        legacySerializer = Reflection.method(legacy, "legacySection");
        deserialize = Reflection.method(legacy, "deserialize", String.class);
        keyOf = Reflection.method(keyClass, "key", String.class, String.class);

        dialogBaseCreate = Reflection.method(base, "create", component, component,
                boolean.class, boolean.class, afterActionClass, List.class, List.class);
        plainMessage = Reflection.method(body, "plainMessage", component, int.class);
        actionButtonCreate = Reflection.method(actionButton, "create", component, component,
                int.class, dialogAction);

        Class<?> binaryTag = Reflection.findClass("net.kyori.adventure.nbt.api.BinaryTagHolder");
        customClick = binaryTag == null ? null
                : Reflection.method(dialogAction, "customClick", keyClass, binaryTag);
        staticAction = Reflection.method(dialogAction, "staticAction", clickEvent);

        notice = Reflection.method(dialogType, "notice", actionButton);
        confirmation = Reflection.method(dialogType, "confirmation", actionButton, actionButton);
        multiAction = Reflection.method(dialogType, "multiAction", List.class, actionButton, int.class);

        Class<?> multilineClass = Reflection.findClass(DATA + "input.TextDialogInput$MultilineOptions");
        multilineCreate = multilineClass == null ? null
                : Reflection.method(multilineClass, "create", Integer.class, Integer.class);
        inputText = Reflection.method(inputClass, "text", String.class, int.class, component,
                boolean.class, String.class, int.class, multilineClass);
        inputBool = Reflection.method(inputClass, "bool", String.class, component, boolean.class,
                String.class, String.class);
        inputNumberRange = Reflection.method(inputClass, "numberRange", String.class, int.class,
                component, String.class, float.class, float.class, Float.class, Float.class);
        inputSingleOption = Reflection.method(inputClass, "singleOption", String.class, int.class,
                List.class, component, boolean.class);
        Class<?> optionEntry = Reflection.findClass(DATA + "input.SingleOptionDialogInput$OptionEntry");
        optionEntryCreate = optionEntry == null ? null
                : Reflection.method(optionEntry, "create", String.class, component, boolean.class);

        clickRunCommand = Reflection.method(clickEvent, "runCommand", String.class);
        clickSuggestCommand = Reflection.method(clickEvent, "suggestCommand", String.class);
        clickOpenUrl = Reflection.method(clickEvent, "openUrl", String.class);
        clickCopyToClipboard = Reflection.method(clickEvent, "copyToClipboard", String.class);

        return dialogCreate != null && factoryEmpty != null && builderBase != null && builderType != null
                && legacySerializer != null && deserialize != null && keyOf != null
                && dialogBaseCreate != null && plainMessage != null && actionButtonCreate != null
                && staticAction != null && notice != null && confirmation != null && multiAction != null;
    }
}
