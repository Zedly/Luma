/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Chat-based user interaction
 *
 * @author Dennis
 */
public class CommandProcessor {

    public static boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Luma.logo + " Available commands:");
            sender.sendMessage(ChatColor.GOLD + "/lu create [name] [dims] [URL]");
            sender.sendMessage(ChatColor.GOLD + "/lu info ([name])");
            sender.sendMessage(ChatColor.GOLD + "/lu print [name]");
            sender.sendMessage(ChatColor.GOLD + "/lu set-action [id] [type] {data}");
            sender.sendMessage(ChatColor.GOLD + "/lu set-source [name] [URL]");
            sender.sendMessage(ChatColor.GOLD + "/lu set-speed [name] [speed]");
            sender.sendMessage(ChatColor.GOLD + "/lu stats");
            sender.sendMessage("");
            return true;
        }
        switch (args[0]) {
            case "create":
                if (args.length != 4) {
                    sender.sendMessage(ChatColor.GOLD + "/lu create [name] [dims] [URL]");
                    sender.sendMessage(ChatColor.GRAY + "Generates a new image with the given dimensions from the given web resource.");
                    sender.sendMessage(ChatColor.GOLD + "Example: " + ChatColor.GRAY + "/lu create creeperface 2x2 http://i.imgur.com/KtVdxxX.jpg");
                    sender.sendMessage("");
                    break;
                }
                if (!args[1].matches("^[a-zA-Z0-9_]+$")) {
                    sender.sendMessage(ChatColor.GOLD + "Invalid image name! " + ChatColor.GRAY + "Must be alphanumeric");
                    break;
                }
                if (CanvasManager.hasCanvasByName(args[1])) {
                    sender.sendMessage(ChatColor.GOLD + "Image already exists! " + ChatColor.GRAY + "Use set-source to change");
                    break;
                }
                String[] dims = args[2].split("x");
                if (dims.length == 2) {
                    try {
                        int width = Integer.parseInt(dims[0]);
                        int height = Integer.parseInt(dims[1]);
                        if (width <= 0 || height <= 0 || width > 10 || height > 10) {
                            sender.sendMessage(ChatColor.GOLD + "Invalid dimensions! " + ChatColor.GRAY + " Width and height must be 1-10.");
                            break;
                        }
                        sender.sendMessage(ChatColor.GOLD + "Loading...");
                        new CanvasURLLoader(sender, args[1], width, height, args[3]).start();
                        break;
                    } catch (NumberFormatException ex) {
                    }
                }
                sender.sendMessage(ChatColor.GOLD + "Invalid dimensions! " + ChatColor.GRAY + "Must be [width]x[height]");
                sender.sendMessage(ChatColor.GOLD + "Example: " + ChatColor.GRAY + "/lu create creeperface 2x2 http://i.imgur.com/KtVdxxX.jpg");
                sender.sendMessage("");
                break;
            case "info":
                LumaCanvas canvas = null;
                if (args.length == 2) {
                    if (!CanvasManager.hasCanvasByName(args[1])) {
                        sender.sendMessage(ChatColor.GOLD + "Image does not exist! " + ChatColor.GRAY + "Hold a piece of the picture or look at the picture to get information about it");
                        break;
                    }
                    canvas = CanvasManager.getCanvasByName(args[1]);
                } else if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack is = player.getInventory().getItemInMainHand();
                    if (is != null && is.getType() == Material.MAP && CanvasManager.hasMapId(is.getDurability())) {
                        canvas = CanvasManager.getCanvasByMapId(is.getDurability());
                    } else {
                        ItemFrame itemFrame = LongRangeAimUtil.getMapInView(player);
                        if (itemFrame != null) {
                            is = itemFrame.getItem();
                            if (is != null && is.getType() == Material.MAP && CanvasManager.hasMapId(is.getDurability())) {
                                canvas = CanvasManager.getCanvasByMapId(is.getDurability());
                            }
                        }
                    }
                }
                if (canvas != null) {
                    int width = canvas.getWidth();
                    int height = canvas.getHeight();
                    int frames = canvas.getFrames();
                    sender.sendMessage(Luma.logo + " About this image:");
                    sender.sendMessage(ChatColor.GOLD + "  Name: " + ChatColor.GRAY + canvas.getName());
                    sender.sendMessage(ChatColor.GOLD + "  Loaded: " + ChatColor.GRAY + (canvas.isLoaded()
                            ? ChatColor.GREEN + "Yes " + ChatColor.GRAY + "(" + (16 * width * height * frames) + "K)" : "No"));
                    sender.sendMessage(ChatColor.GOLD + "  Size: " + ChatColor.GRAY + width + "x" + height + " tiles"
                            + (canvas.isLoaded() && canvas.getFrames() > 1 ? ", " + frames + " frames" : ""));
                    sender.sendMessage(ChatColor.GOLD + "  Map IDs: " + ChatColor.GRAY
                            + canvas.getBaseId() + "-" + (canvas.getBaseId() + canvas.getWidth() * canvas.getHeight() - 1));
                    if (canvas.getFrames() > 1) {
                        sender.sendMessage(ChatColor.GOLD + "  Refresh Rate: " + ChatColor.GRAY + canvas.getDelay() + " ticks per frame");
                    }
                    sender.sendMessage(ChatColor.GOLD + "  Click Actions: ");
                    boolean hasActions = false;
                    for (LumaMap map : canvas.getMaps()) {
                        if (map.hasClickAction()) {
                            hasActions = true;
                            ClickAction ca = map.getClickAction();
                            sender.sendMessage(ChatColor.GOLD + "    - " + map.getMapId() + ": " + ChatColor.GRAY + ca.getTypeString() + " \"" + ca.getData() + "\"");
                        }
                    }
                    if (!hasActions) {
                        sender.sendMessage(ChatColor.GRAY + "    (None)");
                    }

                    sender.sendMessage("");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "/lu info ([name])");
                    sender.sendMessage(ChatColor.GRAY + "Displays information about an existing image.");
                    if (sender instanceof Player) {
                        sender.sendMessage(ChatColor.GRAY + "Hold a part of an image or look at it to see information without knowing its name");
                    }
                }
                break;
            case "list":
                int page = 0;
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]) - 1;
                    } catch (NumberFormatException ex) {
                    }
                }
                List<String> imageNames = CanvasManager.getCanvasIds();
                if (page < 0 || page * 20 >= imageNames.size()) {
                    page = 0;
                }
                
                Iterator<String> nameIterator = imageNames.iterator();
                
                for (int i = 0; i < 20 * page; i++) {
                    nameIterator.next();
                }
                
                StringBuilder sb = new StringBuilder();
                boolean comma = false;
                for (int i = 0; i < 20 && nameIterator.hasNext(); i++) {
                    if (comma) {
                        sb.append(", ");
                    }
                    String name = nameIterator.next();
                    canvas = CanvasManager.getCanvasByName(name);
                    sb.append(ChatColor.GOLD).append(name)
                            .append(ChatColor.GRAY).append(" (")
                            .append(canvas.getWidth()).append("x").append(canvas.getHeight()).append(")");
                    comma = true;
                }
                sender.sendMessage(Luma.logo + "Page " + ChatColor.GOLD + (page + 1) 
                        + ChatColor.GRAY + " of " + ChatColor.GOLD + (imageNames.size() / 20 + 1) 
                        + ChatColor.GRAY + ":");
                sender.sendMessage(sb.toString());
                break;
            case "print":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.GOLD + "Only works ingame!");
                    break;
                }
                Player player = (Player) sender;
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.GOLD + "/lu print [name]");
                    sender.sendMessage(ChatColor.GRAY + "Spawns all the maps belonging to an image into your inventory.");
                    sender.sendMessage(ChatColor.GOLD + "Example: " + ChatColor.GRAY + "/lu print creeperface");
                    sender.sendMessage("");
                    break;
                }
                if (!CanvasManager.hasCanvasByName(args[1])) {
                    sender.sendMessage(ChatColor.GOLD + "Image does not exist! " + ChatColor.GRAY + "Use " + ChatColor.ITALIC + "create" + ChatColor.GRAY + " to create it.");
                    break;
                }
                canvas = CanvasManager.getCanvasByName(args[1]);
                int requiredSlots = canvas.getWidth() * canvas.getHeight();
                int freeSlots = 0;
                for (ItemStack stack : player.getInventory().getStorageContents()) {
                    if (stack == null) {
                        freeSlots++;
                    }
                }
                if (requiredSlots > freeSlots) {
                    sender.sendMessage(ChatColor.GOLD + "Not enough space! " + ChatColor.GRAY + "You need enough inventory space to hold " + requiredSlots + " items.");
                    break;
                }
                for (int i = canvas.getBaseId(); i < canvas.getBaseId() + requiredSlots; i++) {
                    player.getInventory().addItem(new ItemStack(Material.MAP, 1, (short) i));
                }
                sender.sendMessage(ChatColor.GOLD + "Maps printed! ");
                break;
            case "set-action":
                if (args.length <= 2) {
                    sender.sendMessage(ChatColor.GOLD + "/lu set-action [type] [data]");
                    sender.sendMessage(ChatColor.GRAY + "Sets a click action for the map held or looked at");
                    sender.sendMessage(ChatColor.GOLD + "Example: " + ChatColor.GRAY + "/lu set-action message &lBOOM You are dead");
                    sender.sendMessage("");
                    break;
                }

                player = (Player) sender;
                LumaMap lumaMap = null;
                ItemStack is = player.getInventory().getItemInMainHand();
                if (is != null && is.getType() == Material.MAP && CanvasManager.hasMapId(is.getDurability())) {
                    lumaMap = CanvasManager.getMapById(is.getDurability());
                } else {
                    ItemFrame itemFrame = LongRangeAimUtil.getMapInView(player);
                    if (itemFrame != null) {
                        lumaMap = CanvasManager.getMapInItemFrame(itemFrame);
                    }
                }
                if (lumaMap == null) {
                    sender.sendMessage(ChatColor.GOLD + "No map selected! " + ChatColor.GRAY + " Hold the map or look directly at it while setting an action");
                    break;
                } else {
                    ClickAction action = ClickAction.generate(args[1], Arrays.copyOfRange(args, 2, args.length));
                    if (action == null) {
                        sender.sendMessage(ChatColor.GOLD + "Invalid Action Type! " + ChatColor.GRAY + " See /lu set-action for a list of options.");
                        sender.sendMessage(ChatColor.GOLD
                                + "Example: " + ChatColor.GRAY + "/lu set-action message &lBOOM You are dead");
                        sender.sendMessage("");
                    } else {
                        lumaMap.setAction(action);
                        CanvasManager.saveDataYml();
                        sender.sendMessage(ChatColor.GOLD + "Click action updated!");
                        break;
                    }
                }
                break;
            case "set-source":
                if (args.length
                        != 3) {
                    sender.sendMessage(ChatColor.GOLD + "/lu set-source [name] [URL]");
                    sender.sendMessage(ChatColor.GRAY + "Assigns a new resource to the given image. Aspect ratio cannot be changed!");
                    sender.sendMessage(ChatColor.GOLD + "Example: " + ChatColor.GRAY + "/lu set-source creeperface http://i.imgur.com/KtVdxxX.jpg");
                    sender.sendMessage("");
                    break;
                }

                if (!CanvasManager.hasCanvasByName(args[1])) {
                    sender.sendMessage(ChatColor.GOLD + "Image does not exist! " + ChatColor.GRAY + "Use " + ChatColor.ITALIC + "create" + ChatColor.GRAY + " to create it.");
                    break;
                }
                canvas = CanvasManager.getCanvasByName(args[1]);

                sender.sendMessage(ChatColor.GOLD
                        + "Loading...");
                new CanvasURLLoader(sender, canvas, args[2]).start();
                break;
            case "set-speed":
                if (args.length
                        != 3) {
                    sender.sendMessage(ChatColor.GOLD + "/lu set-speed [name] [speed]");
                    sender.sendMessage(ChatColor.GRAY + "Sets an animation's refresh rate in ticks per frame.");
                    sender.sendMessage(ChatColor.GRAY + "One second = 20 ticks. Maps redraw every 10 ticks on a wall and every 4 ticks in a player's inventory.");
                    sender.sendMessage(ChatColor.GRAY + "Changing the refresh rate on a static image has no apparent effect, but will be applied when set-source is used to load an animation.");
                    sender.sendMessage(ChatColor.GOLD + "Example: " + ChatColor.GRAY + "/lu set-speed creeperface 20");
                    sender.sendMessage("");
                    break;
                }

                if (!CanvasManager.hasCanvasByName(args[1])) {
                    sender.sendMessage(ChatColor.GOLD + "Image does not exist! " + ChatColor.GRAY + "Use " + ChatColor.ITALIC + "create" + ChatColor.GRAY + " to create it.");
                    break;
                }
                canvas = CanvasManager.getCanvasByName(args[1]);

                try {
                    int newDelay = Integer.parseInt(args[2]);
                    if (newDelay > 0) {
                        canvas.setDelay(Integer.parseInt(args[2]));
                        if (canvas.getFrames() <= 1) {
                            sender.sendMessage(ChatColor.GRAY + "Changing the refresh rate on a static image has no apparent effect, but will be applied when set-source is used to load an animation.");
                        }
                        if (newDelay < 10) {
                            sender.sendMessage(ChatColor.GOLD + "Fast refresh rate! " + ChatColor.GRAY + "This image will skip frames when on a wall.");
                        }
                        CanvasManager.saveDataYml();
                        sender.sendMessage(ChatColor.GOLD + "Changed refresh rate to " + newDelay + " ticks per frame.");
                        break;
                    }

                } catch (NumberFormatException ex) {
                }
                sender.sendMessage(ChatColor.GOLD
                        + "Invalid refresh rate! " + ChatColor.GRAY + "Must be a natural number of ticks (1/20 second)");

                break;
            case "stats":
                sender.sendMessage(Luma.logo + " Load statistics:");
                sender.sendMessage(ChatColor.GOLD + "  Images: " + ChatColor.GRAY + CanvasManager.getNumberOfCanvases()
                        + " (" + CanvasManager.getNumberOfLoadedCanvases() + " loaded)");
                sender.sendMessage(ChatColor.GOLD + "  Tiles: " + ChatColor.GRAY + CanvasManager.getNumberOfTiles()
                        + " (" + CanvasManager.getNumberOfLoadedTiles() + " loaded)");
                sender.sendMessage(ChatColor.GOLD + "  FPS (cum.): " + ChatColor.GRAY
                        + LoadStatistics.averageCumulativeFPS());
                sender.sendMessage(ChatColor.GOLD + "  RAM (est.): " + ChatColor.GRAY
                        + (Math.round(CanvasManager.getNetMemoryLoad() / 100000.0) / 10.0) + "M");

                long cpu = (LoadStatistics.averageCanvasDrawNanos() + LoadStatistics.averageFrameAdvanceNanos());
                sender.sendMessage(ChatColor.GOLD + "  CPU (est.): " + ChatColor.GRAY
                        + (Math.round(cpu / 1e4) / 100.0) + "ms/tick ("
                        + (Math.round(cpu / 5e3) / 100.0) + "%)");
                sender.sendMessage("");
                break;
        }

        return true;
    }
}
