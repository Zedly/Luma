/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.api.InvalidWorldException;
import com.earth2me.essentials.commands.WarpNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;

/**
 * This click action teleports the user to a warp provided by the Essentials
 * plugin.
 *
 * @author Dennis
 */
public class WarpAction extends ClickAction {

    private static Essentials es;
    private final String warpName;

    public WarpAction(String[] message) {
        warpName = message[0];
        if (es == null) {
            Plugin essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (essentialsPlugin != null && essentialsPlugin instanceof Essentials) {
                es = (Essentials) essentialsPlugin;
                System.out.println("Essentials hooked!");
            }
        }
    }

    @Override
    public String getTypeString() {
        return "Warp";
    }

    @Override
    public String getData() {
        return warpName;
    }

    public void run(Player player, ItemFrame itemFrame) {
        if (es == null) {
            return;
        }
        try {
            Location warpLocation = es.getWarps().getWarp(warpName);
            player.teleport(warpLocation);
        } catch (WarpNotFoundException ex) {
        } catch (net.ess3.api.InvalidWorldException ex) {
            Logger.getLogger(WarpAction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
