/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Dennis
 */
public class Luma extends JavaPlugin {

    public static final int LUMA_MAGIC_CONSTANT = 0x4C554D41;
    public static final String logo = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Luma" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
    public static Luma instance;
    public static LumaCanvas brokenFileIcon;
    public static LumaCanvas loadingIcon;
    public static boolean[] fontMap;
    public static ThreadAsyncLazyFileLoader lazyFileLoader;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        /*
        if(!Storage.loadResources() || !setUp()) {
            System.err.println("Could not load internal resources. Not starting!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
         */
        CanvasManager.loadDataYml();
        int taskid = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, Synchronizer.instance(), 1, 1);
        Synchronizer.setTaskId(taskid);
        lazyFileLoader = new ThreadAsyncLazyFileLoader();
        lazyFileLoader.start();

        CanvasManager.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            CanvasManager.advanceFrames();
        }, 1, 1);

        /*
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            while (!Storage.synchQueue.isEmpty()) {
                Storage.synchQueue.poll().run();
            }
        }, 1, 1);
        Bukkit.getPluginManager().registerEvents(Tricorder.getWatcher(), this);
         */
    }

    @Override
    public void onDisable() {
        lazyFileLoader.shutdown();
        lazyFileLoader = null;
        Bukkit.getScheduler().cancelTask(Synchronizer.getTaskId());
        Bukkit.getScheduler().cancelTask(CanvasManager.taskId);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return CommandProcessor.onCommand(sender, command, label, args);
    }

    private static boolean loadResources() {
        try {
            BufferedImage fontMapImg = ImageIO.read(Luma.class.getResourceAsStream("/fontmap.png"));
            fontMap = new boolean[fontMapImg.getWidth() * fontMapImg.getHeight()];
            for (int j = 0; j < fontMapImg.getHeight(); j++) {
                for (int i = 0; i < fontMapImg.getWidth(); i++) {
                    if (fontMapImg.getRGB(i, j) == 0xFF000000) {
                        fontMap[fontMapImg.getWidth() * j + i] = true;
                    } else {
                        fontMap[fontMapImg.getWidth() * j + i] = false;
                    }
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

}
