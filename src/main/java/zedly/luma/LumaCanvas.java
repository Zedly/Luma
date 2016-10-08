/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.util.List;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;

/**
 * A data structure holding all the imformation in a single image or animation.
 * LumaMaps relay calls to their render() methods to these objects.
 *
 * @author Dennis
 */
public class LumaCanvas {

    private int width;
    private int height;
    private int frames;
    private int delay;
    private byte[] backBuffer;
    private int frameIndex = 0;
    private CanvasState state = CanvasState.DORMANT;
    private final MapCursorCollection mcc = new MapCursorCollection();
    private final List<LumaMap> maps;
    private final String name;
    private final int baseId;
    private long lastUseTime;

    public LumaCanvas(String name, int baseId, int width, int height, List<LumaMap> maps) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.baseId = baseId;
        this.maps = maps;
        while (mcc.size() != 0) {
            mcc.getCursor(0);
        }
        lastUseTime = System.currentTimeMillis();
    }

    /**
     * Draws a section of this image onto a MapCanvas. Loads its content
     * asynchronously when first requested and displays a loading animation
     * until the content is available
     *
     * @param x the x offset of the desired section in multiples of 128px
     * @param y the y offset of the desired section in multiples of 128px
     * @param output the MapCanvas to draw onto
     */
    public void drawTile(int x, int y, MapCanvas output) {
        lastUseTime = System.currentTimeMillis();
        output.setCursors(mcc);
        if (state == CanvasState.DORMANT) {
            Luma.lazyFileLoader.addCanvasToLoad(this);
            state = CanvasState.LOADING;
        } else if (state == CanvasState.LOADING) {
            Luma.loadingIcon.drawTile(0, 0, output);
        } else if (backBuffer == null
                || x < 0 || y < 0 || x >= width || y >= height) {
            Luma.brokenFileIcon.drawTile(0, 0, output);
        } else {
            int bufferOffset = 16384 * (width * (height * frameIndex + y) + x);
            for (int i = 0; i < 16384; i++) {
                output.setPixel(i % 128, i / 128, backBuffer[bufferOffset++]);
            }
        }
    }

    /**
     * Changes this canvas's image data and puts it into the "loaded" state.
     *
     * @param width the new width of this image
     * @param height the new height of this image
     * @param frames the new number of frames of this image
     * @param data the new image data to apply to this image
     */
    public void setData(int width, int height, int frames, byte[] data) {
        this.backBuffer = data;
        this.width = width;
        this.height = height;
        this.frames = frames;
        this.frameIndex = 0;
        for (LumaMap map : maps) {
            map.forceRedraw();
        }
        state = CanvasState.LOADED;
    }

    /**
     * Sets the refresh rate of this canvas in ticks per frame
     *
     * @param delay the new delay between frame advances
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Rotates the image's frame index. Unloads the image data if the image has
     * not been visible for a while
     */
    public void advanceFrame() {
        if (state == CanvasState.DORMANT) {
            return;
        }
        if (System.currentTimeMillis() - lastUseTime > 1800000) {
            backBuffer = null;
            frameIndex = 0;
            state = CanvasState.DORMANT;
        }
        if (frames != 0) {
            frameIndex = (frameIndex + 1) % frames;
        }
    }

    /**
     * Retrieves the name associated with this image
     *
     * @return the name associated with this image
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the baseId associated with this image
     *
     * @return the baseId associated with this image
     */
    public int getBaseId() {
        return baseId;
    }

    /**
     * Retrieves the width of this image
     *
     * @return the width of this image
     */
    public int getWidth() {
        return width;
    }

    /**
     * Retrieves the height of this image
     *
     * @return the height of this image
     */
    public int getHeight() {
        return height;
    }

    /**
     * Retrieves the number of frames of this image. For static images, this
     * returns 1. For unloaded images, this returns 0.
     *
     * @return the number of frames of this image
     */
    public int getFrames() {
        return frames;
    }

    /**
     * Retrieves the position of this image in its sequence of frames For static
     * images, this always returns 0
     *
     * @return the position of this image in its sequence of frames
     */
    public int getFrameIndex() {
        return frameIndex;
    }

    /**
     * Retrieves the refresh rate of this image in ticks per frame
     *
     * @return the refresh rate of this image in ticks per frame
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Returns whether or not this canvas is loaded.
     * If it has not been viewed for a while, it returns to the dormant state to save memory.
     * @return true if the data belonging to this image is in memory.
     */
    public boolean isLoaded() {
        return state != CanvasState.DORMANT;
    }

    public List<LumaMap> getMaps() {
        return maps;
    }

    private enum CanvasState {
        DORMANT, LOADING, LOADED
    }
}
