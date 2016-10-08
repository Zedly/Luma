/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * A click action that sends a simple message to the user
 * @author Dennis
 */
public class MessageAction extends ClickAction {

    private final String message;

    public MessageAction(String[] message) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < message.length; i++) {
            sb.append(message[i].replace("&", ChatColor.COLOR_CHAR + "")).append(" ");
        }
        this.message = sb.toString();
    }

    @Override
    public String getTypeString() {
        return "Message";
    }

    @Override
    public String getData() {
        return message;
    }

    public void run(PlayerInteractEntityEvent evt) {
        evt.getPlayer().sendMessage(message);
    }

}
