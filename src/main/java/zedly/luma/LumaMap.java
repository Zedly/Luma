package zedly.luma;

import java.lang.reflect.Field;
import java.util.Map.Entry;
import net.minecraft.server.v1_16_R2.EntityHuman;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.Packet;
import net.minecraft.server.v1_16_R2.WorldMap;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R2.map.CraftMapView;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;

/**
 * A MapRenderer that draws a specified section of a LumaCanvas onto a map.
 *
 * @author Dennis
 */
public class LumaMap extends MapRenderer {

    private final LumaCanvas lumaCanvas;
    private final int x;
    private final int y;
    private final int mapId;
    private int frameIndex = -1;
    private ClickAction action;

    // NMS Code for high FPS gifs
    private final MapView view;
    private final net.minecraft.server.v1_16_R2.ItemStack mapItem;
    private static Field worldMapField;
    private static boolean nmsEnabled = false;

    public LumaMap(LumaCanvas luCa, int x, int y, MapView view) {
        this.lumaCanvas = luCa;
        this.x = x;
        this.y = y;
        this.view = view;
        this.mapId = view.getId();

        ItemStack map = new ItemStack(Material.FILLED_MAP, 1);
        ItemMeta meta = map.getItemMeta();
        MapMeta mapMeta = (MapMeta) meta;
        mapMeta.setMapView(view);
        map.setItemMeta(meta);

        mapItem = CraftItemStack.asNMSCopy(map);
    }

    @Override
    public void render(MapView view, MapCanvas mapCanvas, Player player) {
        if (lumaCanvas.getFrameIndex() != frameIndex) {
            long nanos = System.nanoTime();
            frameIndex = lumaCanvas.getFrameIndex();
            lumaCanvas.drawTile(x, y, mapCanvas);
            LoadStatistics.updateCanvasDrawNanos(System.nanoTime() - nanos);
            LoadStatistics.countFrame();
        }
    }

    public void forceRedraw() {
        frameIndex = -1;
    }

    public void setAction(ClickAction action) {
        this.action = action;
    }

    public void clickAction(Player player, ItemFrame itemFrame) {
        if (action != null) {
            action.run(player, itemFrame);
        }
    }

    public int getMapId() {
        return mapId;
    }

    public boolean hasClickAction() {
        return action != null;
    }

    public ClickAction getClickAction() {
        return action;
    }

    public void forceDirty() {
        if (!nmsEnabled) {
            return;
        }
        try {
            WorldMap worldMap = (WorldMap) worldMapField.get(view);
            for (Entry<EntityHuman, WorldMap.WorldMapHumanTracker> ent : worldMap.humans.entrySet()) {
                WorldMap.WorldMapHumanTracker tracker = ent.getValue();
                EntityHuman human = ent.getKey();
                Packet<?> packet = tracker.a(mapItem);
                if (packet != null && human instanceof EntityPlayer) {
                    EntityPlayer ep = (EntityPlayer) human;
                    ep.playerConnection.networkManager.sendPacket(packet);
                }
            }
        } catch (Exception ex) {
        }
    }

    static {
        try {
            worldMapField = CraftMapView.class.getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            nmsEnabled = true;
        } catch (Exception ex) {
        }
    }
}
