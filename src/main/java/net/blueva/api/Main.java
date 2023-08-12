package net.blueva.api;

import net.blueva.api.configuration.ConfigManager;
import net.blueva.api.libraries.bStats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    public String pluginversion = getDescription().getVersion();
    public ConfigManager settingsFile;

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + " ____  _");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "| __ )| |_   _  _____   ____ _");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "|  _ \\| | | | |/ _ \\ \\ / / _` |");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "| |_) | | |_| |  __/\\ V | (_| |");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "|____/|_|\\__,_|\\___| \\_/ \\__,_|");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "BlueAPI | V. " + pluginversion + " | blueva.net");

        settingsFile = new ConfigManager("settings.yml", getDataFolder(), this.getClass());
        settingsFile.registerConfig();

        if(settingsFile.getConfig().getBoolean("metrics")) {
            int pluginId = 19470;
            Metrics metrics = new Metrics(this, pluginId);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + " ____  _");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "| __ )| |_   _  _____   ____ _");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "|  _ \\| | | | |/ _ \\ \\ / / _` |");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "| |_) | | |_| |  __/\\ V | (_| |");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "|____/|_|\\__,_|\\___| \\_/ \\__,_|");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "BlueAPI | V. " + pluginversion + " | blueva.net");
    }

}