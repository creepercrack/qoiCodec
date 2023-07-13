package qoiCodec;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Viewer extends JPanel {
    private BufferedImage image;

    public Viewer(BufferedImage image) {
        this.image = image;
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
}
