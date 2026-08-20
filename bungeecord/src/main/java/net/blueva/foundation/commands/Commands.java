package net.blueva.foundation.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

/** Basic literal command registration over BungeeCord's {@link net.md_5.bungee.api.plugin.PluginManager}. */
public class Commands {

    protected Commands() {
    }

    public static void register(Plugin plugin, String name, Executor executor, String... aliases) {
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Command(name, null, aliases) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                executor.execute(sender, args);
            }
        });
    }

    public static void register(Plugin plugin, String name, String permission, Executor executor, String... aliases) {
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Command(name, permission, aliases) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                executor.execute(sender, args);
            }
        });
    }

    public interface Executor {
        void execute(CommandSender sender, String[] args);
    }
}
