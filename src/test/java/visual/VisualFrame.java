package visual;

import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

public class VisualFrame extends JFrame {

    public VisualFrame (String title, JPanel panel, int width, int height) {
        super(title);

        JPanel mainContent = panel;
        mainContent.setOpaque(false);

        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(new OverlayLayout(layered));

        FrameOutlinePanel vignette = new FrameOutlinePanel();
        vignette.setOpaque(false);

        layered.add(vignette, JLayeredPane.PALETTE_LAYER);
        layered.add(mainContent, JLayeredPane.DEFAULT_LAYER);

        layered.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                vignette.setSize(layered.getSize());
                mainContent.setSize(layered.getSize());
            }
        });

        vignette.setAlignmentX(0f);
        vignette.setAlignmentY(0f);
        mainContent.setAlignmentX(0f);
        mainContent.setAlignmentY(0f);

        JFrame frame = new JFrame("Audio Visualizer Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(layered);
        frame.setSize(1000, 750);
        frame.setUndecorated(true);
        frame.setBackground(new java.awt.Color(62, 70, 75, 127));
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);

        int roundRadius = 20;

        int w = frame.getWidth();
        int h = frame.getHeight();

        frame.setShape(new RoundRectangle2D.Float(0, 0, w, h, roundRadius, roundRadius));

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                vignette.setSize(layered.getSize());
                mainContent.setSize(layered.getSize());

                int w = frame.getWidth();
                int h = frame.getHeight();

                frame.setShape(new RoundRectangle2D.Float(0, 0, w, h, roundRadius, roundRadius));
            }
        });

        final Point clickPoint = new Point();
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clickPoint.setLocation(e.getPoint());
            }
        });
        frame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point screen = e.getLocationOnScreen();
                frame.setLocation(screen.x - clickPoint.x, screen.y - clickPoint.y);
            }
        });

        frame.setVisible(true);
    }
}