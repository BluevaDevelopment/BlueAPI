package net.blueva.api.configuration;

import net.blueva.api.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private FileConfiguration config;
    private final File configFile;

    public ConfigManager(String fileName, File dataFolder, Class<?> mainClass) {
        this.configFile = new File(dataFolder, fileName);
        this.config = YamlConfiguration.loadConfiguration(configFile);

        if (this.configFile.exists() && mainClass.getResource("/" + fileName) != null) {
            try (InputStream inputStream = mainClass.getResourceAsStream("/" + fileName)) {
                if(inputStream != null) {
                    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
                    this.config.setDefaults(defConfig);
                }
            } catch (IOException e) {

            }
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
