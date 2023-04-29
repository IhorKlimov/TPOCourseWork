package com.company;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class Main {
    private static final int NUM_OF_THREADS = 4;
    private static final int NUM_OF_BLURS = 8;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(NUM_OF_THREADS);


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
        System.out.println("Reading pixels took: " + (System.currentTimeMillis() - start));

        int[] output = new int[input.length];

        final int pixelIndexOffset = width - filterWidth;
        final int centerOffsetX = filterWidth / 2;
        final int centerOffsetY = filter.length / filterWidth / 2;

        int h = (height - filter.length / filterWidth + 1);
        int w = (width - filterWidth + 1);

        System.out.println(System.currentTimeMillis() - start);

        long loopStart = System.currentTimeMillis();

        List<Callable<Object>> tasks = new ArrayList<>(NUM_OF_THREADS);
        int sectionHeight = h / NUM_OF_THREADS;
        int yRemainder = h - sectionHeight * NUM_OF_THREADS;

        for (int i = 0; i < NUM_OF_THREADS; i++) {
            int yStart = i * (h / NUM_OF_THREADS);
            int yEnd = i == NUM_OF_THREADS - 1 ? yStart + sectionHeight + yRemainder : yStart + sectionHeight;

            tasks.add(Executors.callable(() -> {
                for (int y = yStart; y < yEnd; y++) {
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
            }));
        }

        try {
            threadPool.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long loop = System.currentTimeMillis() - loopStart;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, output, 0, width);
        System.out.println(System.currentTimeMillis() - start + " " + loop);
        return result;
    }

    private static void blurImage(boolean isMultiThread) {
        int[] filter = {
                1, 2, 1,
                2, 4, 2,
                1, 2, 1};
        int filterWidth = 3;
        try {
            BufferedImage img = ImageIO.read(new File("street.jpeg"));
            BufferedImage blurred = img;
            long start = System.currentTimeMillis();
            for (int i = 0; i < NUM_OF_BLURS; i++) {
                if (isMultiThread) {
                    blurred = multiThreadBlur(blurred, filter, filterWidth);
                } else {
                    blurred = singleThreadBlur(blurred, filter, filterWidth);
                }
            }
            System.out.println("Total time: " + (System.currentTimeMillis() - start));
            new DisplayImage(blurred);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        blurImage(true);
    }

}
