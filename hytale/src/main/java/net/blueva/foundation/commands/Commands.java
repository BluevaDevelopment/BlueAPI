package net.blueva.foundation.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Basic literal command registration over Hytale's {@link CommandRegistry}.
 * This covers plain "one command, no argument parsing" registration only -
 * for typed arguments/suggestions, extend {@link AbstractCommand} directly
 * and use its {@code withRequiredArg}/{@code withOptionalArg} builders.
 */
public class Commands {

    protected Commands() {
    }

    public static CommandRegistration register(CommandRegistry commandRegistry, String name, Executor executor, String... aliases) {
        AbstractCommand command = new AbstractCommand(name) {
            @Override
            protected CompletableFuture<Void> execute(CommandContext context) {
                return executor.execute(context);
            }
        };
        if (aliases.length > 0) {
            command.addAliases(aliases);
        }
        return commandRegistry.registerCommand(command);
    }

    public interface Executor {
        CompletableFuture<Void> execute(CommandContext context);
    }
}
