package zedly.luma;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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

    public LumaMap(LumaCanvas luCa, int x, int y, int mapId) {
        this.lumaCanvas = luCa;
        this.x = x;
        this.y = y;
        this.mapId = mapId;
    }

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

}
