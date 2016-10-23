/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Dennis
 */
public class HealAction extends ClickAction {

    public HealAction() {
    }
    
    @Override
    public String getTypeString() {
        return "Heal";
    }

    @Override
    public String getData() {
        return "";
    }

    public void run(Player player, ItemFrame itemFrame) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5);
        for (PotionEffect pe : player.getActivePotionEffects()) {
            player.removePotionEffect(pe.getType());
        }
    }

}
