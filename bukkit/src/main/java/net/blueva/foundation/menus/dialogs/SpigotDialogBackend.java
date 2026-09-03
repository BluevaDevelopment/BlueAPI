package net.blueva.foundation.menus.dialogs;

import net.blueva.foundation.menus.bedrock.Json;
import net.blueva.foundation.messages.Messages;
import net.blueva.foundation.reflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Draws dialogs through Spigot's API, {@code net.md_5.bungee.api.dialog}.
 *
 * <p>Spigot ships these classes from 1.21.6 in the separate
 * {@code net.md-5:bungeecord-dialog} artifact. Paper does not - it has its own
 * dialog API instead - so a server has one of the two, never both, and
 * {@link Dialogs} tries each.</p>
 *
 * <p>Everything is reflective because BlueFoundation compiles against
 * 1.8.8, where none of this exists.</p>
 */
final class SpigotDialogBackend implements DialogBackend {

    private static final String PACKAGE = "net.md_5.bungee.api.dialog.";

    private volatile boolean resolved;
    private volatile boolean usable;

    private Method showDialog;
    private Method clearDialog;
    private Method fromLegacy;
    private Method fromLegacyText;
    private Constructor<?> textComponentFromArray;

    private Class<?> actionClass;
    private Constructor<?> dialogBase;
    private Constructor<?> plainMessageBody;
    private Constructor<?> actionButton;
    private Constructor<?> staticAction;
    private Constructor<?> customClickAction;
    private Constructor<?> clickEvent;
    private Class<?> clickEventAction;
    private Class<?> afterActionEnum;

    private Constructor<?> noticeDialog;
    private Constructor<?> confirmationDialog;
    private Constructor<?> multiActionDialog;

    private Constructor<?> textInput;
    private Constructor<?> textInputMultiline;
    private Constructor<?> booleanInput;
    private Constructor<?> numberRangeInput;
    private Constructor<?> singleOptionInput;
    private Constructor<?> inputOption;

    @Override
    public String name() {
        return "spigot";
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
            plugin.getLogger().warning("[BlueFoundation] Could not show dialog through the Spigot API: " + e);
            return false;
        }
    }

    @Override
    public boolean clear(Player player) {
        if (!resolve() || clearDialog == null) {
            return false;
        }
        try {
            clearDialog.invoke(player);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private Object build(Plugin plugin, Dialog dialog) throws Exception {
        List<Object> bodies = new ArrayList<Object>();
        for (String line : dialog.body()) {
            bodies.add(plainMessageBody.newInstance(component(line), Integer.valueOf(200)));
        }

        List<Object> inputs = new ArrayList<Object>();
        for (DialogInput input : dialog.inputs()) {
            Object built = input(input);
            if (built != null) {
                inputs.add(built);
            }
        }

        Object base = dialogBase.newInstance(
                component(dialog.title()),
                dialog.externalTitle() == null ? null : component(dialog.externalTitle()),
                inputs,
                bodies,
                Boolean.valueOf(dialog.canCloseWithEscape()),
                Boolean.valueOf(dialog.pause()),
                enumConstant(afterActionEnum, dialog.afterAction().name()));

        List<DialogButton> buttons = dialog.buttons();
        switch (dialog.type()) {
            case NOTICE:
                return noticeDialog.newInstance(base,
                        buttons.isEmpty() ? null : button(plugin, dialog, buttons.get(0)));
            case CONFIRMATION:
                if (buttons.size() < 2) {
                    throw new IllegalStateException("A confirmation dialog needs two buttons");
                }
                return confirmationDialog.newInstance(base,
                        button(plugin, dialog, buttons.get(0)),
                        button(plugin, dialog, buttons.get(1)));
            case MULTI_ACTION:
            default:
                List<Object> actions = new ArrayList<Object>();
                for (DialogButton button : buttons) {
                    actions.add(button(plugin, dialog, button));
                }
                return multiActionDialog.newInstance(base, actions, Integer.valueOf(dialog.columns()),
                        dialog.exitButton() == null ? null : button(plugin, dialog, dialog.exitButton()));
        }
    }

    private Object button(Plugin plugin, Dialog dialog, DialogButton button) throws Exception {
        return actionButton.newInstance(
                component(button.label()),
                button.tooltip() == null ? null : component(button.tooltip()),
                Integer.valueOf(button.width()),
                action(plugin, dialog, button.action()));
    }

    private Object action(Plugin plugin, Dialog dialog, DialogAction action) throws Exception {
        switch (action.kind()) {
            case RUN_COMMAND:
                return staticAction.newInstance(click("RUN_COMMAND", withSlash(action.value())));
            case SUGGEST_COMMAND:
                return staticAction.newInstance(click("SUGGEST_COMMAND", withSlash(action.value())));
            case OPEN_URL:
                return staticAction.newInstance(click("OPEN_URL", action.value()));
            case COPY_TO_CLIPBOARD:
                return staticAction.newInstance(click("COPY_TO_CLIPBOARD", action.value()));
            case CALLBACK:
                return customClickAction.newInstance(DialogCallbacks.register(plugin, dialog, action));
            case NONE:
            default:
                // The API has no "do nothing" action and rejects a null one, so
                // an empty implementation of the interface stands in for it.
                return Proxy.newProxyInstance(actionClass.getClassLoader(),
                        new Class<?>[]{actionClass}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) {
                                if ("hashCode".equals(method.getName())) {
                                    return Integer.valueOf(System.identityHashCode(proxy));
                                }
                                if ("equals".equals(method.getName())) {
                                    return Boolean.valueOf(proxy == args[0]);
                                }
                                if ("toString".equals(method.getName())) {
                                    return "NoAction";
                                }
                                return null;
                            }
                        });
        }
    }

    private Object input(DialogInput input) throws Exception {
        switch (input.kind()) {
            case TEXT:
                Object multiline = null;
                if (input.maxLines() != null || input.height() != null) {
                    multiline = textInputMultiline.newInstance(input.maxLines(), input.height());
                }
                return textInput.newInstance(input.key(), Integer.valueOf(input.width()),
                        component(input.label()), Boolean.valueOf(!input.label().isEmpty()),
                        input.initialText(), Integer.valueOf(input.maxLength()), multiline);
            case BOOLEAN:
                return booleanInput.newInstance(input.key(), component(input.label()),
                        Boolean.valueOf(input.initialBoolean()), input.onTrue(), input.onFalse());
            case NUMBER_RANGE:
                return numberRangeInput.newInstance(input.key(), Integer.valueOf(input.width()),
                        component(input.label()), input.labelFormat(),
                        Float.valueOf(input.start()), Float.valueOf(input.end()),
                        input.initialNumber(), input.step());
            case SINGLE_OPTION:
            default:
                List<Object> options = new ArrayList<Object>();
                for (DialogInput.Option option : input.options()) {
                    options.add(inputOption.newInstance(option.id(), component(option.label()),
                            Boolean.valueOf(option.initial())));
                }
                return singleOptionInput.newInstance(input.key(), Integer.valueOf(input.width()),
                        component(input.label()), Boolean.valueOf(!input.label().isEmpty()), options);
        }
    }

    private Object click(String action, String value) throws Exception {
        return clickEvent.newInstance(enumConstant(clickEventAction, action), value);
    }

    /**
     * Turn MiniMessage into the {@code BaseComponent} the bungee API wants, by
     * way of legacy colour codes - the one text format every version of
     * BlueFoundation and every version of that API agree on.
     */
    private Object component(String miniMessage) throws Exception {
        String legacy = Messages.legacySection(miniMessage == null ? "" : miniMessage);
        if (fromLegacy != null) {
            return fromLegacy.invoke(null, legacy);
        }
        Object array = fromLegacyText.invoke(null, legacy);
        return textComponentFromArray.newInstance(array);
    }

    private void listen(Plugin plugin) {
        DialogCallbacks.listen(plugin, "org.bukkit.event.player.PlayerCustomClickEvent", new EventExecutor() {
            @Override
            public void execute(org.bukkit.event.Listener listener, Event event) {
                handle(event);
            }
        });
    }

    private void handle(Event event) {
        try {
            Method getId = event.getClass().getMethod("getId");
            Object id = getId.invoke(event);
            if (id == null) {
                return;
            }
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Object player = getPlayer.invoke(event);
            if (!(player instanceof Player)) {
                return;
            }
            final Map<String, Object> values = readData(event);
            DialogCallbacks.fire(id.toString(), (Player) player, new DialogCallbacks.ResponseSource() {
                @Override
                public String read(DialogInput input) {
                    Object value = values.get(input.key());
                    return value == null ? null : String.valueOf(value);
                }
            });
        } catch (Throwable e) {
            // A Spigot build whose event moved is not worth crashing a click over.
        }
    }

    /**
     * The event carries the player's answers as a Gson {@code JsonElement}.
     * Rather than reach for Gson - which is relocated on old servers and
     * absent from BlueFoundation's runtime contract - it is round-tripped
     * through its own {@code toString} and read back with our parser.
     */
    private Map<String, Object> readData(Event event) {
        try {
            Method getData = event.getClass().getMethod("getData");
            Object data = getData.invoke(event);
            if (data == null) {
                return Collections.emptyMap();
            }
            Object parsed = Json.parse(data.toString());
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                return map;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyMap();
    }

    private static String withSlash(String command) {
        String trimmed = command == null ? "" : command.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static Object enumConstant(Class<?> type, String name) {
        Object[] constants = type.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant instanceof Enum && name.equals(((Enum<?>) constant).name())) {
                return constant;
            }
        }
        return null;
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
        Class<?> dialogClass = Reflection.findClass(PACKAGE + "Dialog");
        Class<?> baseComponent = Reflection.findClass("net.md_5.bungee.api.chat.BaseComponent");
        Class<?> textComponent = Reflection.findClass("net.md_5.bungee.api.chat.TextComponent");
        if (dialogClass == null || baseComponent == null || textComponent == null) {
            return false;
        }

        showDialog = Reflection.method(Player.class, "showDialog", dialogClass);
        if (showDialog == null) {
            return false;
        }
        clearDialog = Reflection.method(Player.class, "clearDialog");

        fromLegacy = Reflection.method(textComponent, "fromLegacy", String.class);
        fromLegacyText = Reflection.method(textComponent, "fromLegacyText", String.class);
        textComponentFromArray = constructor(textComponent,
                Reflection.findClass("[Lnet.md_5.bungee.api.chat.BaseComponent;"));
        if (fromLegacy == null && (fromLegacyText == null || textComponentFromArray == null)) {
            return false;
        }

        Class<?> dialogBaseClass = Reflection.findClass(PACKAGE + "DialogBase");
        actionClass = Reflection.findClass(PACKAGE + "action.Action");
        afterActionEnum = Reflection.findClass(PACKAGE + "DialogBase$AfterAction");
        Class<?> body = Reflection.findClass(PACKAGE + "body.DialogBody");
        Class<?> input = Reflection.findClass(PACKAGE + "input.DialogInput");
        clickEventAction = Reflection.findClass("net.md_5.bungee.api.chat.ClickEvent$Action");
        if (dialogBaseClass == null || actionClass == null || afterActionEnum == null
                || body == null || input == null || clickEventAction == null) {
            return false;
        }

        dialogBase = constructor(dialogBaseClass, baseComponent, baseComponent, List.class, List.class,
                boolean.class, boolean.class, afterActionEnum);
        plainMessageBody = constructor(Reflection.findClass(PACKAGE + "body.PlainMessageBody"),
                baseComponent, int.class);
        actionButton = constructor(Reflection.findClass(PACKAGE + "action.ActionButton"),
                baseComponent, baseComponent, int.class, actionClass);
        staticAction = constructor(Reflection.findClass(PACKAGE + "action.StaticAction"),
                Reflection.findClass("net.md_5.bungee.api.chat.ClickEvent"));
        customClickAction = constructor(Reflection.findClass(PACKAGE + "action.CustomClickAction"), String.class);
        clickEvent = constructor(Reflection.findClass("net.md_5.bungee.api.chat.ClickEvent"),
                clickEventAction, String.class);

        noticeDialog = constructor(Reflection.findClass(PACKAGE + "NoticeDialog"),
                dialogBaseClass, Reflection.findClass(PACKAGE + "action.ActionButton"));
        confirmationDialog = constructor(Reflection.findClass(PACKAGE + "ConfirmationDialog"),
                dialogBaseClass, Reflection.findClass(PACKAGE + "action.ActionButton"),
                Reflection.findClass(PACKAGE + "action.ActionButton"));
        multiActionDialog = constructor(Reflection.findClass(PACKAGE + "MultiActionDialog"),
                dialogBaseClass, List.class, int.class, Reflection.findClass(PACKAGE + "action.ActionButton"));

        Class<?> multilineClass = Reflection.findClass(PACKAGE + "input.TextInput$Multiline");
        textInput = constructor(Reflection.findClass(PACKAGE + "input.TextInput"),
                String.class, int.class, baseComponent, boolean.class, String.class, int.class, multilineClass);
        textInputMultiline = constructor(multilineClass, Integer.class, Integer.class);
        booleanInput = constructor(Reflection.findClass(PACKAGE + "input.BooleanInput"),
                String.class, baseComponent, boolean.class, String.class, String.class);
        numberRangeInput = constructor(Reflection.findClass(PACKAGE + "input.NumberRangeInput"),
                String.class, int.class, baseComponent, String.class, float.class, float.class,
                Float.class, Float.class);
        singleOptionInput = constructor(Reflection.findClass(PACKAGE + "input.SingleOptionInput"),
                String.class, int.class, baseComponent, boolean.class, List.class);
        inputOption = constructor(Reflection.findClass(PACKAGE + "input.InputOption"),
                String.class, baseComponent, boolean.class);

        return dialogBase != null && plainMessageBody != null && actionButton != null
                && staticAction != null && customClickAction != null && clickEvent != null
                && noticeDialog != null && confirmationDialog != null && multiActionDialog != null;
    }

    private static Constructor<?> constructor(Class<?> type, Class<?>... parameters) {
        if (type == null) {
            return null;
        }
        for (Class<?> parameter : parameters) {
            if (parameter == null) {
                return null;
            }
        }
        try {
            Constructor<?> found = type.getConstructor(parameters);
            found.setAccessible(true);
            return found;
        } catch (Throwable e) {
            return null;
        }
    }
}
