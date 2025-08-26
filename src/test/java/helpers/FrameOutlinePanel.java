package helpers;

import javax.swing.*;
import java.awt.*;

public class FrameOutlinePanel extends JPanel {

    private final BasicStroke stroke = new BasicStroke(5.0f);

    public FrameOutlinePanel () {
        setOpaque(false);
    }

    @Override
    protected void paintComponent (Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setStroke(stroke);

        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
    }
}
