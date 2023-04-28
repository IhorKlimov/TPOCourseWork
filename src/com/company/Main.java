package com.company;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class Main {

    public static BufferedImage singleThreadBlur(BufferedImage image, int[] filter, int filterWidth) {
        if (filter.length % filterWidth != 0) {
            throw new IllegalArgumentException("filter contains a incomplete row");
        }

        long start = System.currentTimeMillis();

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int sum = IntStream.of(filter).sum();

        int[] input = image.getRGB(0, 0, width, height, null, 0, width);
        System.out.println(System.currentTimeMillis() - start);

        int[] output = new int[input.length];

        final int pixelIndexOffset = width - filterWidth;
        final int centerOffsetX = filterWidth / 2;
        final int centerOffsetY = filter.length / filterWidth / 2;

        int h = (height - filter.length / filterWidth + 1);
        int w = (width - filterWidth + 1);
//        System.out.println(h + " " + w);

        System.out.println(System.currentTimeMillis() - start);

        long loopStart = System.currentTimeMillis();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0;
                int g = 0;
                int b = 0;
                for (int filterIndex = 0, pixelIndex = y * width + x;
                     filterIndex < filter.length;
                     pixelIndex += pixelIndexOffset) {
                    for (int fx = 0; fx < filterWidth; fx++, pixelIndex++, filterIndex++) {
                        int col = input[pixelIndex];
                        int factor = filter[filterIndex];

                        // sum up color channels seperately
                        r += ((col >>> 16) & 0xFF) * factor;
                        g += ((col >>> 8) & 0xFF) * factor;
                        b += (col & 0xFF) * factor;
                    }
                }
                r /= sum;
                g /= sum;
                b /= sum;
                // combine channels with full opacity
                output[x + centerOffsetX + (y + centerOffsetY) * width] = (r << 16) | (g << 8) | b | 0xFF000000;
            }
        }

        long loop = System.currentTimeMillis() - loopStart;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, output, 0, width);
        System.out.println(System.currentTimeMillis() - start + " " + loop);
        return result;
    }

    public static BufferedImage multiThreadBlur(BufferedImage image, int[] filter, int filterWidth) {
        if (filter.length % filterWidth != 0) {
            throw new IllegalArgumentException("filter contains a incomplete row");
        }

        long start = System.currentTimeMillis();

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int sum = IntStream.of(filter).sum();

        int[] input = image.getRGB(0, 0, width, height, null, 0, width);
        System.out.println(System.currentTimeMillis() - start);

        int[] output = new int[input.length];

        final int pixelIndexOffset = width - filterWidth;
        final int centerOffsetX = filterWidth / 2;
        final int centerOffsetY = filter.length / filterWidth / 2;

        int h = (height - filter.length / filterWidth + 1);
        int w = (width - filterWidth + 1);

        System.out.println(System.currentTimeMillis() - start);

        long loopStart = System.currentTimeMillis();


        ArrayList<Thread> threads = new ArrayList<>();
        int numOfThreads = 4;

        for (int i = 0; i < numOfThreads; i++) {
            int rowLength = (int) Math.sqrt(numOfThreads);
            int sectionWidth = rowLength == 0 ? w : w / rowLength;
            int sectionHeight = rowLength == 0 ? h : h / rowLength;

            int currentRow = rowLength == 0 ? 0 : i / rowLength;
            int currentColumn = rowLength == 0 ? 0 : i % rowLength;

            int xStart = currentColumn * sectionWidth;
            int yStart = currentRow * sectionHeight;
            int xEnd = xStart + sectionWidth;
            int yEnd = yStart + sectionHeight;

            System.out.println(xStart + " -> " + xEnd + " " + yStart + " -> " + yEnd + " " + currentRow + " " + currentColumn);
            threads.add(new Thread(() -> {
                for (int y = yStart; y < yEnd; y++) {
                    for (int x = xStart; x < xEnd; x++) {
                        int r = 0;
                        int g = 0;
                        int b = 0;
                        for (int filterIndex = 0, pixelIndex = y * width + x;
                             filterIndex < filter.length;
                             pixelIndex += pixelIndexOffset) {
                            for (int fx = 0; fx < filterWidth; fx++, pixelIndex++, filterIndex++) {
                                int col = input[pixelIndex];
                                int factor = filter[filterIndex];

                                // sum up color channels seperately
                                r += ((col >>> 16) & 0xFF) * factor;
                                g += ((col >>> 8) & 0xFF) * factor;
                                b += (col & 0xFF) * factor;
                            }
                        }
                        r /= sum;
                        g /= sum;
                        b /= sum;
                        // combine channels with full opacity
                        output[x + centerOffsetX + (y + centerOffsetY) * width] = (r << 16) | (g << 8) | b | 0xFF000000;
                    }
                }
            }));
        }

        threads.forEach(thread -> thread.start());
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        long loop = System.currentTimeMillis() - loopStart;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, output, 0, width);
        System.out.println(System.currentTimeMillis() - start + " " + loop);
        return result;
    }

    public static void main(String[] args) {
//        singleThreadBlur();
        multiThreadBlur();

//        long start = System.currentTimeMillis();
//
//        // 11_136
//
//        ArrayList<Thread> threads = new ArrayList<>();
//
//        threads.add(new Thread(() -> {
//            for (int i = 0; i < 25_000_000; i++) {
//                double d = i * Math.PI / Math.sin(i * Math.PI);
//
//            }
//        }));
//        threads.add(new Thread(() -> {
//            for (int i = 0; i < 25_000_000; i++) {
//                double d = i * Math.PI / Math.sin(i * Math.PI);
//
//            }
//        }));
//
//        threads.add(new Thread(() -> {
//            for (int i = 0; i < 25_000_000; i++) {
//                double d = i * Math.PI / Math.sin(i * Math.PI);
//
//            }
//        }));
//
//        threads.add(new Thread(() -> {
//            for (int i = 0; i < 25_000_000; i++) {
//                double d = i * Math.PI / Math.sin(i * Math.PI);
//
//            }
//        }));
//
//
//        threads.forEach(thread -> thread.start());
//        threads.forEach(thread -> {
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//
//        System.out.println(System.currentTimeMillis() - start);
    }

    private static void singleThreadBlur() {
        //        int[] filter = {1, 2, 1, 2, 4, 2, 1, 2, 1};
        int[] filter = {
                1, 2, 1,
                2, 4, 2,
                1, 2, 1};
        int filterWidth = 3;
        try {
            BufferedImage img = ImageIO.read(new File("beach2.jpeg"));
            BufferedImage blurred = img;
            for (int i = 0; i < 1; i++) {
                blurred = singleThreadBlur(blurred, filter, filterWidth);
            }

            new DisplayImage(blurred);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void multiThreadBlur() {
        //        int[] filter = {1, 2, 1, 2, 4, 2, 1, 2, 1};
        int[] filter = {
                1, 2, 1,
                2, 4, 2,
                1, 2, 1};
        int filterWidth = 3;
        try {
            BufferedImage img = ImageIO.read(new File("beach2.jpeg"));
            BufferedImage blurred = img;
            for (int i = 0; i < 1; i++) {
                blurred = multiThreadBlur(blurred, filter, filterWidth);
            }
            new DisplayImage(blurred);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
