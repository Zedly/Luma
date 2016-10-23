/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import org.bukkit.ChatColor;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

/**
 * A click action that runs a basic command for the player clicking.
 * @author Dennis
 */
public class CommandAction extends ClickAction {

    private final String command;

    public CommandAction(String[] message) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < message.length; i++) {
            sb.append(message[i].replace("&", ChatColor.COLOR_CHAR + "")).append(" ");
        }
        this.command = sb.toString();
    }

    @Override
    public String getTypeString() {
        return "Command";
    }

    @Override
    public String getData() {
        return command;
    }

    public void run(Player player, ItemFrame itemFrame) {
        player.performCommand(command);
    }
}
