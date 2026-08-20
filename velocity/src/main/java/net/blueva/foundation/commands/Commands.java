package net.blueva.foundation.commands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

/**
 * Basic literal command registration over Velocity's {@link CommandManager}.
 * This covers plain "one alias, raw String[] args" commands only - for
 * Brigadier argument parsing/suggestions, register a
 * {@code com.velocitypowered.api.command.BrigadierCommand} directly.
 */
public class Commands {

    protected Commands() {
    }

    public static void register(ProxyServer proxyServer, Object plugin, String name, Executor executor, String... aliases) {
        CommandManager commandManager = proxyServer.getCommandManager();
        CommandMeta meta = commandManager.metaBuilder(name)
                .aliases(aliases)
                .plugin(plugin)
                .build();
        commandManager.register(meta, new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                executor.execute(invocation.source(), invocation.arguments());
            }

            @Override
            public boolean hasPermission(Invocation invocation) {
                return executor.hasPermission(invocation.source(), invocation.arguments());
            }
        });
    }

    public static void unregister(ProxyServer proxyServer, String name) {
        proxyServer.getCommandManager().unregister(name);
    }

    public interface Executor {
        void execute(CommandSource source, String[] args);

        default boolean hasPermission(CommandSource source, String[] args) {
            return true;
        }
    }
}
