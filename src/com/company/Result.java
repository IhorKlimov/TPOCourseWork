package com.company;

import java.awt.image.BufferedImage;

public class Result {
    private BufferedImage image;
    private int[] pixels;

    public Result(BufferedImage image, int[] pixels) {
        this.image = image;
        this.pixels = pixels;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int[] getPixels() {
        return pixels;
    }
}
