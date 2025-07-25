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

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(layered);
        setSize(1000, 750);
        setUndecorated(true);
        setBackground(new java.awt.Color(62, 70, 75, 127));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        int roundRadius = 20;

        int w = getWidth();
        int h = getHeight();

        setShape(new RoundRectangle2D.Float(0, 0, w, h, roundRadius, roundRadius));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                vignette.setSize(layered.getSize());
                mainContent.setSize(layered.getSize());

                int w = getWidth();
                int h = getHeight();

                setShape(new RoundRectangle2D.Float(0, 0, w, h, roundRadius, roundRadius));
            }
        });

        final Point clickPoint = new Point();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clickPoint.setLocation(e.getPoint());
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point screen = e.getLocationOnScreen();
                setLocation(screen.x - clickPoint.x, screen.y - clickPoint.y);
            }
        });
    }
}