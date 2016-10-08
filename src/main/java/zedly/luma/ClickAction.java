/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Basically a Runnable called when a user interacts with a map that has a click action.
 * Can't be bothered to document this. You get the idea
 * @author Dennis
 */
public abstract class ClickAction {

    private static final HashMap<String, Class<? extends ClickAction>> SUBCLASS_TYPES = new HashMap<>();

    public static ClickAction generate(String type, String[] data) {
        switch (type) {
            case "message":
                return new MessageAction(data);
        }
        return null;
    }
    
    public static ClickAction generate(String type, String data) {
        switch (type) {
            case "message":
                return new MessageAction(data.split(" "));
        }
        return null;
    }

    public abstract void run(PlayerInteractEntityEvent evt);

    public abstract String getTypeString();

    public abstract String getData();

    static {
        SUBCLASS_TYPES.put("message", MessageAction.class);
    }

}
