package net.blueva.foundation.menus.bedrock;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/**
 * The reflective bridge onto Cumulus, GeyserMC's form library.
 *
 * <p>BlueFoundation never compiles against Cumulus. It does not have to:
 * {@code Forms.fromJson} takes the very JSON that goes to the client, which is
 * exactly what {@link Form#toJson()} already produces, and hands back a
 * Cumulus form whose raw response consumer is a plain
 * {@link BiConsumer}. So the whole integration is two reflective lookups and
 * a lambda, with our own {@link Form} still owning every handler.</p>
 *
 * <p>Shared by the Floodgate and Geyser transports, which differ only in who
 * they hand the finished Cumulus form to.</p>
 */
final class CumulusBridge {

    private static volatile boolean resolved;
    private static volatile boolean usable;
    private static Method fromJson;
    private static Class<?> formType;
    private static Class<?> formClass;

    private CumulusBridge() {
    }

    /**
     * @return whether Cumulus is on the classpath in a shape we understand
     */
    static boolean available() {
        if (resolved) {
            return usable;
        }
        synchronized (CumulusBridge.class) {
            if (resolved) {
                return usable;
            }
            resolved = true;
            usable = resolve();
            return usable;
        }
    }

    /**
     * @return the {@code org.geysermc.cumulus.form.Form} interface, or {@code null}
     */
    static Class<?> formClass() {
        return available() ? formClass : null;
    }

    /**
     * Turn one of our forms into a Cumulus form whose response comes back to
     * {@link Form#handle(Player, String)} on the player's own thread.
     *
     * @param plugin the plugin that will own the response task
     * @param player who the form is for
     * @param form   the form
     * @return a Cumulus form instance, or {@code null} if Cumulus is unusable
     */
    static Object toCumulus(final Plugin plugin, final Player player, final Form form) {
        if (!available()) {
            return null;
        }
        Object type = constantNamed(cumulusTypeName(form.type()));
        if (type == null) {
            return null;
        }
        BiConsumer<Object, String> responseHandler = new BiConsumer<Object, String>() {
            @Override
            public void accept(Object ignored, final String raw) {
                dispatch(plugin, player, form, raw);
            }
        };
        try {
            return fromJson.invoke(null, form.toJson(), type, responseHandler);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Run a form response where it is safe to touch the player: on the main
     * thread, or on the player's region thread under Folia.
     */
    private static void dispatch(Plugin plugin, final Player player, final Form form, final String raw) {
        if (Scheduler.isPrimaryThread()) {
            form.handle(player, raw);
            return;
        }
        Scheduler.runAtEntity(plugin, player, new Runnable() {
            @Override
            public void run() {
                form.handle(player, raw);
            }
        });
    }

    /**
     * Match against Cumulus' enum by name. The wire ordinals are fixed by the
     * protocol, but the enum being reflected on belongs to somebody else's jar
     * and is not ours to assume the shape of.
     */
    private static Object constantNamed(String wanted) {
        Object[] constants = formType.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant instanceof Enum && wanted.equals(((Enum<?>) constant).name())) {
                return constant;
            }
        }
        return null;
    }

    private static String cumulusTypeName(FormType type) {
        switch (type) {
            case MODAL:
                return "MODAL_FORM";
            case CUSTOM:
                return "CUSTOM_FORM";
            case SIMPLE:
            default:
                return "SIMPLE_FORM";
        }
    }

    private static boolean resolve() {
        Class<?> forms = Reflection.findClass("org.geysermc.cumulus.Forms");
        formClass = Reflection.findClass("org.geysermc.cumulus.form.Form");
        formType = Reflection.findClass("org.geysermc.cumulus.form.util.FormType");
        if (forms == null || formClass == null || formType == null) {
            return false;
        }
        fromJson = Reflection.method(forms, "fromJson", String.class, formType, BiConsumer.class);
        return fromJson != null;
    }
}
