/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Dennis
 */
public class LongRangeAimUtil {

    /**
     * Calculates whether a player is looking at a LumaMap and returns it.
     * Returns an ItemFrame entity if it is in the player's line of sight and
     * contains a map registered as a Luma image tile, otherwise null.
     *
     * @param player the player whose line of sight to search
     * @return the ItemFrame entity in line of sight, or null
     */
    public static ItemFrame getMapInView(Player player) {
        List<Block> lastTwoBlocks = player.getLastTwoTargetBlocks((HashSet<Material>) null, Settings.MAX_CLICK_RANGE);
        if (lastTwoBlocks.size() < 2) {
            return null;
        }
        Block baseBlock = lastTwoBlocks.get(1);
        Block searchBlock = lastTwoBlocks.get(0);
        BlockFace bf = searchBlock.getFace(baseBlock);
        Location loc = getBlockCenterLocation(searchBlock.getLocation());
        Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5);
        for (Entity ent : entities) {
            if (ent instanceof ItemFrame) {
                ItemFrame itemFrame = (ItemFrame) ent;
                if (itemFrame.getAttachedFace() == bf) {
                    ItemStack is = itemFrame.getItem();
                    if (is.getType() == Material.FILLED_MAP && CanvasManager.hasMap(is)) {
                        return itemFrame;
                    }
                }
            }
        }
        return null;
    }

    private static Location getBlockCenterLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5);
    }

}
