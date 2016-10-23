/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * Handles all the image data
 *
 * @author Dennis
 */
public class CanvasManager {

    private static final HashMap<String, LumaCanvas> CANVASES_BY_NAME = new HashMap<>();
    private static final HashMap<Integer, LumaCanvas> CANVASES_BY_MAP_ID = new HashMap<>();
    private static final HashMap<Integer, LumaMap> MAPS_BY_MAP_ID = new HashMap<>();
    private static final ArrayList<LumaCanvas> CANVASES = new ArrayList<>();
    private static int tickId = 0;
    public static int taskId;

    /**
     * Creates a new LumaCanvas and registers it in the internal association
     * maps.
     *
     * @param name the name of the canvas to create
     * @param data the image data to apply to the canvas
     * @param width the width of the canvas
     * @param height the height of the canvas
     * @param frames the number of frames on the canvas
     * @return the baseId of the new canvas (lowest map ID, which contains the
     * top left corner)
     */
    public static int createCanvasForData(String name, byte[] data, int width, int height, int frames) {
        ArrayList<MapView> views = new ArrayList<>();
        for (int i = 0; i < width * height; i++) {
            MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
            views.add(view);
        }
        int baseId = views.get(0).getId();
        ArrayList<LumaMap> newMaps = new ArrayList<>();
        LumaCanvas canvas = new LumaCanvas(name, baseId, width, height, 20, newMaps);
        canvas.setData(width, height, frames, data);
        canvas.setDelay(20);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MapView mapView = views.get(width * y + x);
                LumaMap lumaMap = new LumaMap(canvas, x, y, mapView.getId());
                newMaps.add(lumaMap);
                CANVASES_BY_MAP_ID.put((int) mapView.getId(), canvas);
                MAPS_BY_MAP_ID.put((int) mapView.getId(), lumaMap);
                stripRenderers(mapView);
                mapView.addRenderer(lumaMap);
            }
        }
        CANVASES.add(canvas);
        CANVASES_BY_NAME.put(name, canvas);
        saveDataYml();
        return baseId;
    }

    /**
     * Claims map IDs defined in the data.yml for images and initializes image
     * parameters. Image data is loaded on demand.
     *
     * @param name the name of the canvas to load
     * @param baseId the lowest map ID belonging to the image, containing the
     * top left corner
     * @param width the width of the image
     * @param height the height of the image
     * @param delay the refresh rate of the image
     */
    private static void preloadCanvas(String name, int baseId, int width, int height, int delay) {
        ArrayList<LumaMap> newMaps = new ArrayList<>();
        LumaCanvas canvas = new LumaCanvas(name, baseId, width, height, delay, newMaps);
        canvas.setDelay(delay);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MapView mapView = Bukkit.getMap((short) (baseId + y * width + x));
                LumaMap lumaMap = new LumaMap(canvas, x, y, mapView.getId());
                newMaps.add(lumaMap);
                CANVASES_BY_MAP_ID.put((int) mapView.getId(), canvas);
                MAPS_BY_MAP_ID.put((int) mapView.getId(), lumaMap);
                stripRenderers(mapView);
                mapView.addRenderer(lumaMap);
            }
        }
        CANVASES.add(canvas);
        CANVASES_BY_NAME.put(name, canvas);
    }

    /**
     * Checks whether this name belongs to an existing canvas
     *
     * @param name the name to search
     * @return the result
     */
    public static boolean hasCanvasByName(String name) {
        return CANVASES_BY_NAME.containsKey(name);
    }

    /**
     * Returns the canvas belonging to the given name
     *
     * @param name the name to query
     * @return the canvas, or null
     */
    public static LumaCanvas getCanvasByName(String name) {
        return CANVASES_BY_NAME.get(name);
    }

    /**
     * Returns the canvas belonging to the given map ID
     *
     * @param mapId the map ID to search
     * @return the canvas, or null
     */
    public static LumaCanvas getCanvasByMapId(int mapId) {
        return CANVASES_BY_MAP_ID.get(mapId);
    }

    /**
     * Returns the number of loaded canvases (with data in RAM)
     *
     * @return the number of loaded canvases (with data in RAM)
     */
    public static int getNumberOfLoadedCanvases() {
        int loaded = 0;
        for (LumaCanvas canvas : CANVASES) {
            if (canvas.isLoaded()) {
                loaded++;
            }
        }
        return loaded;
    }

    /**
     * Returns the total number of canvases
     *
     * @return the total number of canvases
     */
    public static int getNumberOfCanvases() {
        return CANVASES.size();
    }
    
    /**
     * Returns the number of loaded tiles (with data in RAM)
     *
     * @return the number of loaded tiles (with data in RAM)
     */
    public static int getNumberOfLoadedTiles() {
        int tiles = 0;
        for (LumaCanvas canvas : CANVASES) {
            if (canvas.isLoaded()) {
                tiles += canvas.getWidth() * canvas.getHeight();
            }
        }
        return tiles;
    }

    /**
     * Returns the total number of tiles (registered map IDs)
     * @return the total number of tiles (registered map IDs)
     */
    public static int getNumberOfTiles() {
        int tiles = 0;
        for (LumaCanvas canvas : CANVASES) {
            tiles += canvas.getWidth() * canvas.getHeight();
        }
        return tiles;
    }
    
    /**
     * Returns an estimate of the net memory load caused by loaded canvases.
     * Ignores JVM overhead.
     * @return the number of content bytes
     */
    public static int getNetMemoryLoad() {
        int bytes = 0;
        for (LumaCanvas canvas : CANVASES) {
            if (canvas.isLoaded()) {
                bytes += 16384 * canvas.getWidth() * canvas.getHeight() * canvas.getFrames();
            }
        }
        return bytes;
    }

    /**
     * Checks whether this map ID belongs to an existing image
     *
     * @param mapId the map ID to search
     * @return the result
     */
    public static boolean hasMapId(int mapId) {
        return MAPS_BY_MAP_ID.containsKey(mapId);
    }

    /**
     * Returns the LumaMap corresponding to this map ID, or null
     *
     * @param mapId the map ID to query
     * @return the corresponding LumaMap
     */
    public static LumaMap getMapById(int mapId) {
        return MAPS_BY_MAP_ID.get(mapId);
    }

    /**
     * Returns the LumaMap displayed in this ItemFrame. Returns null if the item
     * contained is not a registered map.
     *
     * @param itemFrame the item frame to search
     * @return the corresponding LumaMap
     */
    public static LumaMap getMapInItemFrame(ItemFrame itemFrame) {
        ItemStack is = itemFrame.getItem();
        if (is != null) {
            if (is.getType() == Material.MAP && hasMapId(is.getDurability())) {
                return getMapById(is.getDurability());
            }
        }
        return null;
    }

    /**
     * Ensures map ID ranges defined in the data.yml do not collide and the map
     * IDs exist.
     *
     * @param baseId the lowest map ID corresponding to the image eing loaded
     * @param length the number of tiles belonging ti this image
     * @return true if the ID range exists and is unclaimed, or false.
     */
    private static boolean isMapIdRangeAvailable(int baseId, int length) {
        for (int i = baseId; i < baseId + length; i++) {
            if (CanvasManager.hasMapId(i) || Bukkit.getMap((short) i) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Advances frames on all loaded images.
     */
    public static void advanceFrames() {
        long nanos = System.nanoTime();
        tickId++;
        for (LumaCanvas canvas : CanvasManager.CANVASES) {
            if (canvas.getDelay() != 0 && tickId % canvas.getDelay() == 0) {
                canvas.advanceFrame();
            }
        }
        LoadStatistics.updateFrameAdvanceNanos(System.nanoTime() - nanos);
    }

    /**
     * Loads image, click action and map ID range definitions from the data.yml.
     */
    public static void loadDataYml() {
        File dataFile = new File(Luma.instance.getDataFolder(), "data.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<?, ?>> canvasList = config.getMapList("canvases");
        List<Map<?, ?>> actionList = config.getMapList("actions");
        boolean dirty = false;

        if (canvasList == null) {
            canvasList = new ArrayList<>();
            System.out.println("No canvases section in data.yml. Creating");
            config.set("canvases", canvasList);
            dirty = true;
        }
        if (actionList == null) {
            actionList = new ArrayList<>();
            System.out.println("No actions section in data.yml. Creating");
            config.set("actions", actionList);
            dirty = true;
        }

        // data.yml does not contain all necessary fields. Create and save
        if (dirty) {
            try {
                config.save(dataFile);
            } catch (IOException ex) {
                return;
            }
        }

        for (Map map : canvasList) {
            try {
                String name = (String) map.get("name");
                int width = (int) map.get("width");
                int height = (int) map.get("height");
                int delay = (int) map.get("delay");
                int baseId = (int) map.get("baseId");
                if (hasCanvasByName(name)) {
                    System.out.println("Duplicate definition of image \"" + name + "\". Skipping");
                    continue;
                } else if (!isMapIdRangeAvailable(baseId, width * height)) {
                    System.out.println("Duplicate claim of map IDs by image \"" + name + "\". Skipping");
                    continue;
                } else if (baseId + width * height > 32767) {
                    System.out.println("Map ID claim of image \"" + name + "\" out of bounds. Skipping");
                    continue;
                }
                System.out.println("Loading image: " + name + "@" + baseId + "ff. " + width + "x" + height + " " + delay);
                preloadCanvas(name, baseId, width, height, delay);
            } catch (Exception ex) {
                System.err.println("Invalid canvas entry in the data.yml. Skipping");
                ex.printStackTrace();
            }
        }

        for (Map map : actionList) {
            try {
                int mapId = (int) map.get("mapId");
                String name = (String) map.get("type");
                String data = (String) map.get("data");
                if (!hasMapId(mapId)) {
                    System.out.println("Click Action defined for unregistered mapId " + mapId + ". Skipping");
                    continue;
                }
                LumaMap lumaMap = getMapById(mapId);
                lumaMap.setAction(ClickAction.generate(name, data));
            } catch (Exception ex) {
                System.err.println("Invalid canvas entry in the data.yml. Skipping");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Saves the current image, click action and map ID range definitions to the
     * data.yml
     */
    public static void saveDataYml() {
        ArrayList<Map<?, ?>> canvasList = new ArrayList<>();
        ArrayList<Map<?, ?>> actionList = new ArrayList<>();
        for (LumaCanvas canvas : CANVASES) {
            Map<String, Object> canvasParams = new HashMap<>();
            canvasParams.put("name", canvas.getName());
            canvasParams.put("width", canvas.getWidth());
            canvasParams.put("height", canvas.getHeight());
            canvasParams.put("delay", canvas.getDelay());
            canvasParams.put("baseId", canvas.getBaseId());
            canvasList.add(canvasParams);
            for (LumaMap map : canvas.getMaps()) {
                if (map.hasClickAction()) {
                    ClickAction ca = map.getClickAction();
                    HashMap<String, Object> clickActionParams = new HashMap<>();
                    clickActionParams.put("mapId", map.getMapId());
                    clickActionParams.put("type", ca.getTypeString());
                    clickActionParams.put("data", ca.getData());
                    actionList.add(clickActionParams);
                }
            }
        }
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(new File(""));
            config.set("canvases", canvasList);
            config.set("actions", actionList);
            config.save(new File(Luma.instance.getDataFolder(), "data.yml"));
        } catch (IOException ex) {
        }
    }

    /**
     * Removes server-internal MapRenderers from this MapView
     *
     * @param mapView the map view to strip
     */
    private static void stripRenderers(MapView mapView) {
        for (MapRenderer renderer : mapView.getRenderers()) {
            mapView.removeRenderer(renderer);
        }
    }
}
