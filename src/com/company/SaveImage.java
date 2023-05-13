package com.company;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SaveImage {
    public static void saveImage(BufferedImage bufferedImage) {
        File output = new File("blurred.png");
        try {
            ImageIO.write(getScaledImage(bufferedImage), "png", output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage getScaledImage(BufferedImage srcImg ) {
        int w = srcImg.getWidth();
        int h = srcImg.getHeight();
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }
}
