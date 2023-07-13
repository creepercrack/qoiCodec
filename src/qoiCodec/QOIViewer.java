package qoiCodec;

import javax.swing.*;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class QOIViewer {
    public static void main(String[] args) {
        File file = new File("qoi_test_images/wikipedia_008.qoi");

        try (DataInputStream qoiInputImage = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (!Arrays.equals(qoiInputImage.readNBytes(4), "qoif".getBytes())) return;
            int width = qoiInputImage.readInt();
            int height = qoiInputImage.readInt();
            byte channels = qoiInputImage.readByte();
            byte colorspace = qoiInputImage.readByte();

            System.out.printf("%d %d %d %d%n", width, height, channels, colorspace);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream qoiOutputImage = new DataOutputStream(baos);

            byte r = 0;
            byte g = 0;
            byte b = 0;
            byte a = -1;
            int[] seenPixels = new int[64];

            while (qoiInputImage.available() > 0) {
                byte flag = qoiInputImage.readByte();

                switch (flag) {
                    case -2 -> {
                        // QOI_OP_RGB

                        r = qoiInputImage.readByte();
                        g = qoiInputImage.readByte();
                        b = qoiInputImage.readByte();

                        //System.out.printf("QOI_OP_RGB %d %d %d %d%n", r, g, b, a);
                    }

                    case -1 -> {
                        // QOI_OP_RGBA

                        r = qoiInputImage.readByte();
                        g = qoiInputImage.readByte();
                        b = qoiInputImage.readByte();
                        a = qoiInputImage.readByte();

                        //System.out.printf("QOI_OP_RGBA %d %d %d %d%n", r, g, b, a);
                    }

                    default -> {
                        byte data = (byte) (flag & 0x3F);
                        flag = (byte) (flag >> 6);

                        switch (flag) {
                            case 0 -> {
                                // QOI_OP_INDEX

                                r = (byte) (seenPixels[data] >> 24 & 0xFF);
                                g = (byte) (seenPixels[data] >> 16 & 0xFF);
                                b = (byte) (seenPixels[data] >> 8  & 0xFF);
                                a = (byte) (seenPixels[data]       & 0xFF);

                                //System.out.printf("QOI_OP_INDEX %d %d %d %d%n", r, g, b, a);
                            }

                            case 1 -> {
                                // QOI_OP_DIFF

                                r += (data >> 4 & 0x03) - 2;
                                g += (data >> 2 & 0x03) - 2;
                                b += (data      & 0x03) - 2;

                                //System.out.printf("QOI_OP_DIFF %d %d %d %d%n", r, g, b, a);
                            }

                            case -2 -> {
                                // QOI_OP_LUMA
                                byte data2 = qoiInputImage.readByte();
                                byte gDiff = (byte) (data - 32);

                                r += gDiff + (data2 >> 4 & 0x0F) - 8;
                                g += gDiff;
                                b += gDiff + (data2      & 0x0F) - 8;

                                //System.out.printf("QOI_OP_LUMA %d %d %d %d%n", r, g, b, a);
                            }

                            case -1 -> {
                                // QOI_OP_RUN

                                for (int i = 0; i <= data; i++) {
                                    qoiOutputImage.writeByte(a);
                                    qoiOutputImage.writeByte(b);
                                    qoiOutputImage.writeByte(g);
                                    qoiOutputImage.writeByte(r);

                                    //System.out.printf("QOI_OP_RUN %d %d %d %d%n", r, g, b, a);

                                    int j = ((r & 0xFF) * 3 + (g & 0xFF) * 5 + (b & 0xFF) * 7 + (a & 0xFF) * 11) % 64;
                                    seenPixels[j] = ByteBuffer.wrap(new byte[]{r, g, b, a}).getInt();
                                }

                                continue;
                            }
                        }
                    }
                }

                qoiOutputImage.writeByte(a);
                qoiOutputImage.writeByte(b);
                qoiOutputImage.writeByte(g);
                qoiOutputImage.writeByte(r);

                int i = ((r & 0xFF) * 3 + (g & 0xFF) * 5 + (b & 0xFF) * 7 + (a & 0xFF) * 11) % 64;
                seenPixels[i] = ByteBuffer.wrap(new byte[]{r, g, b, a}).getInt();
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            byte[] src = baos.toByteArray();
            byte[] dst = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(src, 0, dst, 0, dst.length);

            JFrame window = new JFrame();
            window.setTitle(String.format("[%dx%d] %s", width, height, file.getName()));
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            window.add(new Viewer(image));
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
