/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.util.HashMap;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

/**
 * Basically a Runnable called when a user interacts with a map that has a click
 * action. Can't be bothered to document this. You get the idea
 *
 * @author Dennis
 */
public abstract class ClickAction {

    private static final HashMap<String, Class<? extends ClickAction>> SUBCLASS_TYPES = new HashMap<>();

    public static ClickAction generate(String type, String[] data) {
        switch (type.toLowerCase()) {
            case "command":
                return new CommandAction(data);
            case "heal":
                return new HealAction();
            case "message":
                return new MessageAction(data);
            case "warp":
                return new WarpAction(data);
            
        }
        return null;
    }

    public static ClickAction generate(String type, String data) {
        return generate(type, data.split(" "));
    }

    public abstract void run(Player player, ItemFrame itemFrame);

    public abstract String getTypeString();

    public abstract String getData();

    static {
        SUBCLASS_TYPES.put("message", MessageAction.class);
    }

}
