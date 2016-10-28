/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.map.MapPalette;
import zedly.luma.HTTP.HTTPResponse;

/**
 * Asynchronously loads image resources from the Web, processes them for
 * displaying and saves them to disk.
 *
 * @author Dennis
 */
public class CanvasURLLoader extends Thread {

    private final String name;
    private final String url;
    private final int width;
    private final int height;
    private final BufferedImage scaledImg;
    private final CommandSender sender;

    public CanvasURLLoader(CommandSender sender, String name, int width, int height, String url) {
        this.name = name;
        this.url = url;
        this.width = width;
        this.height = height;
        this.sender = sender;
        scaledImg = new BufferedImage(128 * width, 128 * height, BufferedImage.TYPE_INT_ARGB);
    }

    public CanvasURLLoader(CommandSender sender, LumaCanvas canvas, String url) {
        this(sender, canvas.getName(), canvas.getWidth(), canvas.getHeight(), url);
    }

    /**
     * This thread's main logic
     */
    public void run() {
        try {
            List<BufferedImage> images = getImagesFromURL(new URL(url));
            if (images.isEmpty()) {
                sendMessage(ChatColor.GOLD + "Unable to load image!");
                return;
            }

            int frames = images.size();
            byte[] data = new byte[16384 * width * height * frames];

            for (int i = 0; i < frames; i++) {
                BufferedImage img = images.get(i);
                AffineTransformOp atOp = generateAffineTransformOp(img, width, height);
                atOp.filter(img, scaledImg);

                // Rearrange pixels such that one map tile is one consecutive block of 16k bytes,
                // translate pixels into the color palette of maps.
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        Image subImage = scaledImg.getSubimage(128 * x, 128 * y, 128, 128);
                        byte[] mapData = MapPalette.imageToBytes(subImage);
                        int mapDataOffset = 16384 * width * height * i
                                + 16384 * width * y
                                + 16384 * x;
                        for (int j = 0; j < 16384; j++) {
                            data[mapDataOffset + j] = mapData[j];
                        }
                    }
                }
            }

            byte[] fileData = data;
            int fileFrames = frames;
            // If animations are disabled, only keep the first frame in memory, but save the full animation
            if (!Settings.ANIMATIONS) {
                data = Arrays.copyOf(fileData, 16384 * width * height);
                frames = 1;
                sendMessage(ChatColor.GOLD + "Note: Animations are disabled, but will work if enabled in the config.yml");
            }

            if (CanvasManager.hasCanvasByName(name)) {
                CanvasManager.getCanvasByName(name).setData(frames, data);
                sendMessage(ChatColor.GOLD + "Successfully changed image!");
            } else {
                CanvasManager.createCanvasForData(name, data, width, height, frames);
                sendMessage(ChatColor.GOLD + "Successfully loaded image! " + ChatColor.GRAY + "Use "
                        + ChatColor.ITALIC + "/lu print " + name + ChatColor.GRAY + " to get the created maps");
            }
            writeBlob(name, width, height, fileFrames, fileData);
        } catch (Exception ex) {
            sendMessage(ChatColor.GOLD + "Unable to load image! " + ChatColor.GRAY + ex.getMessage());
        }
    }

    /**
     * Sends a message to the user who created this thread. Runs in the server's
     * main thread for compatibility.
     *
     * @param message the message to send
     */
    private void sendMessage(String message) {
        Synchronizer.add(() -> {
            sender.sendMessage(message);
        });
    }

    /**
     * Attempts to load one or more images from the given URL. Can process JPG,
     * PNG, BMP, GIF and ZIP files. If the resource is a ZIP file, it must
     * contain JPG, PNG or BMP files named by consecutive natural numbers
     * without leading zeros.
     *
     * @param url the URL of the resource to load
     * @return a List of BufferedImages for scaling, or none if an error occurs.
     * Static images return a list with one entry.
     * @throws IOException if the resource cannot be read.
     */
    private List<BufferedImage> getImagesFromURL(URL url) throws IOException {
        System.out.println("Getting image from URL");
        ArrayList<BufferedImage> images = new ArrayList<>();
        HTTPResponse response = HTTP.get(url);
        System.out.println("HTTP response: " + response.getHeaders().get(null).get(0));
        if (!response.getHeaders().get(null).get(0).contains("OK")) {
            return images;
        }
        String mimeType = response.getHeaders().get("Content-Type").get(0);
        if (mimeType == null) {
            return images;
        }
        ByteArrayInputStream contentInput = new ByteArrayInputStream(response.getContent());
        mimeType = mimeType.split(";")[0];

        switch (mimeType) {
            case "image/jpg":
            case "image/jpeg":
            case "image/png":
            case "image/bmp":
                BufferedImage img = ImageIO.read(contentInput);
                images.add(img);
                break;
            case "image/gif":
                images.addAll(GifUtil.readGif(contentInput));
                break;
            case "application/zip":
                HashMap<String, byte[]> imageFiles = new HashMap<>();
                byte[] unzipBuffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(contentInput);
                //get the zipped file list entry
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    if (ze.isDirectory()) {
                        System.err.println("Skipping non-conforming file in ZIP..");
                        zis.closeEntry();
                        ze = zis.getNextEntry();
                        continue;
                    }
                    String fileName = ze.getName();
                    System.out.println("File: " + fileName);
                    if (!fileName.matches("^[1-9][0-9]{0,2}\\.(jpg|jpeg|png|bmp)$")) {
                        System.err.println("Skipping non-conforming file in ZIP..");
                        zis.closeEntry();
                        ze = zis.getNextEntry();
                        continue;
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len;
                    while ((len = zis.read(unzipBuffer)) > 0) {
                        baos.write(unzipBuffer, 0, len);
                    }
                    imageFiles.put(fileName.substring(0, fileName.indexOf(".")), baos.toByteArray());
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                }
                zis.close();
                try {
                    for (int i = 1;; i++) {
                        if (!imageFiles.containsKey(i + "")) {
                            return images;
                        }
                        img = ImageIO.read(new ByteArrayInputStream(imageFiles.get(i + "")));
                        images.add(img);
                    }
                } catch (IOException ex) {
                    return images;
                }
            default:
                sendMessage(ChatColor.GOLD + "Unknown file format! " + ChatColor.GRAY + "Make sure you are pasting a deep link. These usually contain a picture file ending");
                return images;

        }
        return images;
    }

    /**
     * Creates a transform operation that scales the source image to the desired
     * size
     *
     * @param srcImg the source iamge to be scaled
     * @param width the desired width in multiples of 128px
     * @param height the desired height in multiples of 128px
     * @return an AffineTransformOp that performs the desired scaling
     */
    private AffineTransformOp generateAffineTransformOp(BufferedImage srcImg, int width, int height) {
        double scaleX = (128.0 * width) / (double) srcImg.getWidth();
        double scaleY = (128.0 * height) / (double) srcImg.getHeight();
        AffineTransform at = AffineTransform.getScaleInstance(scaleX, scaleY);
        return new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
    }

    /**
     * Creates a binary file containing the compressed image data.
     *
     * @param name the name of the binary file (Luma/images/name.bin)
     * @param width the width of the image file
     * @param height the height of the image file
     * @param frames the number of frames in the image file. Static images
     * contain 1 frame.
     * @param data the binary image data consisting of 16384*width*height*frames
     * bytes
     * @throws IOException if the file cannot be written to.
     */
    private void writeBlob(String name, int width, int height, int frames, byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        byte[] deflatedBuffer = new byte[data.length];
        int deflatedLength = 0;
        while (!deflater.finished()) {
            deflatedLength += deflater.deflate(deflatedBuffer, deflatedLength, deflatedBuffer.length - deflatedLength);
        }
        deflater.end();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(Luma.LUMA_MAGIC_CONSTANT >> 24);
        baos.write(Luma.LUMA_MAGIC_CONSTANT >> 16);
        baos.write(Luma.LUMA_MAGIC_CONSTANT >> 8);
        baos.write(Luma.LUMA_MAGIC_CONSTANT);
        baos.write(width);
        baos.write(height);
        baos.write(0);
        baos.write(0);
        baos.write(0);
        baos.write(frames);

        FileOutputStream fos = new FileOutputStream(new File(Luma.instance.getDataFolder(), "images/" + name + ".bin"));
        fos.write(baos.toByteArray());
        fos.write(deflatedBuffer, 0, deflatedLength);
        fos.flush();
        fos.close();
    }
}
