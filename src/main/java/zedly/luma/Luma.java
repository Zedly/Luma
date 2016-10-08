/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
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
        getDataFolder().mkdir();
        new File(getDataFolder(), "images").mkdir();
        if(!loadResources()) {
            Bukkit.getPluginManager().disablePlugin(this);
        }
        CanvasManager.loadDataYml();
        int taskid = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, Synchronizer.instance(), 1, 1);
        Synchronizer.setTaskId(taskid);
        lazyFileLoader = new ThreadAsyncLazyFileLoader();
        lazyFileLoader.start();
        Bukkit.getPluginManager().registerEvents(Watcher.instance(), this);
        CanvasManager.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            CanvasManager.advanceFrames();
        }, 1, 1);
    }

    @Override
    public void onDisable() {
        lazyFileLoader.shutdown();
        lazyFileLoader = null;
        HandlerList.unregisterAll(Watcher.instance());
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

            byte[] temp = new byte[16384];
            DataInputStream dis = new DataInputStream(Luma.class.getResourceAsStream("/error"));
            dis.readFully(temp);
            dis.close();
            brokenFileIcon = new LumaCanvas("Error", 1, 1, 1, 20, new ArrayList<>());
            brokenFileIcon.setData(1, 1, 1, temp);

            temp = new byte[65536];
            dis = new DataInputStream(Luma.class.getResourceAsStream("/loading"));
            dis.readFully(temp);
            loadingIcon = new LumaCanvas("Loading", 1, 1, 4, 10, new ArrayList<>());
            loadingIcon.setData(1, 1, 4, temp);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

}
