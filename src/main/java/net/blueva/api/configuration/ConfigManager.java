package net.blueva.api.configuration;

import net.blueva.api.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private FileConfiguration config;
    private final File configFile;

    public ConfigManager(String fileName, File dataFolder, Main main) {
        this.configFile = new File(dataFolder, fileName);
        this.config = YamlConfiguration.loadConfiguration(configFile);

        if (this.configFile.exists() && main.getResource(fileName) != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(main.getResource(fileName), StandardCharsets.UTF_8));
            this.config.setDefaults(defConfig);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void registerConfig() {
        if (!configFile.exists()) {
            config.options().copyDefaults(true);
            saveConfig();
        }
    }
}
