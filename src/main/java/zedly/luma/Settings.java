/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author Dennis
 */
public class Settings {
    
    public static int MAX_CLICK_RANGE = 25;
    public static boolean ANIMATIONS = true;
    public static int IMAGE_IDLE_UNLOAD_TIME = 1800;
    public static int STATISTICS_AVERAGE_TIME = 100;
    
    public static void loadConfigYml() {
        Luma.instance.saveDefaultConfig();
        FileConfiguration fc = Luma.instance.getConfig();
        MAX_CLICK_RANGE = fc.getInt("max-click-range", 25);
        ANIMATIONS = fc.getBoolean("animations", true);
        IMAGE_IDLE_UNLOAD_TIME = 1000 * fc.getInt("image-idle-unload-time", 1800);
        STATISTICS_AVERAGE_TIME = 20 * fc.getInt("statistics-average-time", 100);
    }
    
}
