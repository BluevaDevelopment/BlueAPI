package net.blueva.api.configuration;

import java.io.File;

public class ConfigUtils {
    public static void generateFolder(String folderName, File dataFolder) {
        File folder = new File(dataFolder+"/"+folderName);
        if(!folder.exists()) {
            folder.mkdirs();
        }
    }
}
