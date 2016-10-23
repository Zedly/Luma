/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.zip.Inflater;

/**
 * Asynchronously loads image blobs from files into canvases when requested
 *
 * @author Dennis
 */
public class ThreadAsyncLazyFileLoader extends Thread {

    private final Inflater inflater = new Inflater();
    private final LinkedList<LumaCanvas> canvasQueue = new LinkedList<>();
    private boolean alive = true;

    public void run() {
        try {
            LumaCanvas canvas;
            while (true) {
                synchronized (this) {
                    while (alive && canvasQueue.isEmpty()) {
                        wait();
                    }
                    if (!alive) {
                        return;
                    }
                    canvas = canvasQueue.remove();
                }
                loadCanvasFromFile(canvas);
            }
        } catch (InterruptedException ex) {
        }
    }

    private void loadCanvasFromFile(final LumaCanvas canvas) {
        System.out.println("Lazily loading " + canvas.getName());
        try {
            File file = new File(Luma.instance.getDataFolder(), "images/" + canvas.getName() + ".bin");
            if (!file.exists()) {
                Synchronizer.add(() -> {
                    canvas.setNullData();
                });
            }
            int size = (int) file.length();
            byte[] data = new byte[size];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(data);
            dis.close();
            dis = new DataInputStream(new ByteArrayInputStream(data));
            if (dis.readInt() != Luma.LUMA_MAGIC_CONSTANT) {
                Synchronizer.add(() -> {
                    canvas.setNullData();
                });
                return;
            }
            int width = dis.readByte();
            int height = dis.readByte();
            int frames = (Settings.ANIMATIONS ? dis.readInt() : 1); // If animations disabled, only load first frame
            byte[] uncompressedData = new byte[16384 * width * height * frames];

            inflater.reset();
            inflater.setInput(data, 10, data.length - 10);
            inflater.inflate(uncompressedData);

            Synchronizer.add(() -> {
                canvas.setData(width, height, frames, uncompressedData);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Synchronizer.add(() -> {
                canvas.setNullData();
            });
        }
    }

    public synchronized void shutdown() {
        alive = false;
        notifyAll();
    }

    public synchronized void addCanvasToLoad(LumaCanvas canvas) {
        canvasQueue.add(canvas);
        notifyAll();
    }

}
