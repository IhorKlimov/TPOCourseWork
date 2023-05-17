package com.company;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.lang.Math.abs;

public class Main {
    private static final int NUM_OF_THREADS = 2;
    private static final int NUM_OF_BLURS = 1;
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
        System.out.println("Image pixel read took: " + (System.currentTimeMillis() - start));

        int[] output = new int[input.length];

        final int pixelIndexOffset = width - filterWidth;
        final int centerOffsetX = filterWidth / 2;
        final int centerOffsetY = filter.length / filterWidth / 2;

        int h = (height - filter.length / filterWidth + 1);
        int w = (width - filterWidth + 1);

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

                        int red = (col >>> 16) & 255;
                        int green = (col >>> 8) & 255;
                        int blue = col & 255;

                        r += red * factor;
                        g += green * factor;
                        b += blue * factor;
                    }
                }
                r /= sum;
                g /= sum;
                b /= sum;

                output[x + centerOffsetX + (y + centerOffsetY) * width] = 255 << 24 | (r << 16) | (g << 8) | b;
            }
        }

        long loop = System.currentTimeMillis() - loopStart;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, output, 0, width);
        System.out.println("Total: " + (System.currentTimeMillis() - start) + " blur took: " + loop);
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

        int[] input = new int[width * height];

        List<Callable<Object>> pixelsTask = new ArrayList<>(NUM_OF_THREADS);
        int yRem = height % NUM_OF_THREADS;

        for (int i = 0; i < NUM_OF_THREADS; i++) {
            int finalI = i;
            pixelsTask.add(Executors.callable(() -> {
                int sectionHeight = height / NUM_OF_THREADS;
                int startY = sectionHeight * finalI;
                int extra = finalI == NUM_OF_THREADS - 1 ? yRem : 0;
                int[] section = image.getRGB(0, startY, width, sectionHeight + extra, null, 0, width);
                System.out.println("h check " + sectionHeight + " " + extra + " " + section.length);
                System.arraycopy(section, 0, input, sectionHeight * width * finalI, section.length);
            }));
        }

        try {
            threadPool.invokeAll(pixelsTask);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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


                                r += ((col >>> 16) & 255) * factor;
                                g += ((col >>> 8) & 255) * factor;
                                b += (col & 255) * factor;
                            }
                        }
                        r /= sum;
                        g /= sum;
                        b /= sum;

                        output[x + centerOffsetX + (y + centerOffsetY) * width] = 255 << 24 | (r << 16) | (g << 8) | b;
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

    private static BufferedImage blurImage(boolean isMultiThread) {
        int radius = 1;
        int filterWidth = radius * 2 + 1;
        int[] filter = generateMatrix(radius);
        try {
            BufferedImage image = ImageIO.read(new File("street3.jpeg"));
            long start = System.currentTimeMillis();
            for (int i = 0; i < NUM_OF_BLURS; i++) {
                if (isMultiThread) {
                    image = multiThreadBlur(image, filter, filterWidth);
                } else {
                    image = singleThreadBlur(image, filter, filterWidth);
                }
            }
            System.out.println("Total time: " + (System.currentTimeMillis() - start));
            new DisplayImage(image);
//            saveImage(blurred);
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        BufferedImage bufferedImage = blurImage(false);
        BufferedImage bufferedImage2 = blurImage(true);

        int widthOne = bufferedImage.getWidth();
        int heightOne = bufferedImage.getHeight();
        int[] pixelsOne = bufferedImage.getRGB(0, 0, widthOne, heightOne, null, 0, widthOne);

        int widthTwo = bufferedImage2.getWidth();
        int heightTwo = bufferedImage2.getHeight();
        int[] pixelsTwo = bufferedImage2.getRGB(0, 0, widthTwo, heightTwo, null, 0, widthTwo);

        System.out.println(Arrays.equals(pixelsOne, pixelsTwo));
    }

    private static int[] generateMatrix(int radius) {
        int rowLength = radius * 2 + 1;

        int[] result = new int[rowLength * rowLength];

        int center = radius;
        int centerValue = 100;
        int cellLength = centerValue / (radius + 2);

        for (int row = 0; row < rowLength; row++) {
            String res = "";
            for (int column = 0; column < rowLength; column++) {
                int currentIndex = row * rowLength + column;

                int xDelta = abs(center - column) * cellLength;
                int yDelta = abs(center - row) * cellLength;
                int distance = (int) Math.round(Math.sqrt(xDelta * xDelta + yDelta * yDelta));
                result[currentIndex] = centerValue - distance;

                res += result[currentIndex] + " ";
            }
            System.out.println(res);
        }
        return result;
    }

}