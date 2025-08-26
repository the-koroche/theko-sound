package helpers;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class VisualFrame extends JFrame {

    private final int roundRadius;
    private boolean halfOpacity = false;

    private final Point clickPoint = new Point();
    private final int resizeMargin;
    private int resizeDir = 0;

    private boolean toggleToCenter = false;

    private Runnable onClose = null;

    public VisualFrame(String title, JPanel panel, int width, int height,
                       int roundRadius, int resizeMargin) {
        super(title);
        this.roundRadius = roundRadius;
        this.resizeMargin = resizeMargin;

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
        setSize(width, height);
        setUndecorated(true);
        setBackground(new Color(62, 70, 75, 127));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setOpacity(1.0f);

        updateShape();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                vignette.setSize(layered.getSize());
                mainContent.setSize(layered.getSize());
                updateShape();

                if (toggleToCenter) {
                    setLocationRelativeTo(null);
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (toggleToCenter) {
                    setLocationRelativeTo(null);
                }
            }
        });

        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem alwaysOnTopItem = new JMenuItem("Always on Top");
        alwaysOnTopItem.addActionListener(ev -> setAlwaysOnTop(!isAlwaysOnTop()));
        JMenuItem halfOpacityItem = new JMenuItem("Half-Opacity");
        halfOpacityItem.addActionListener(ev -> toggleOpacity());
        JMenuItem moveCenterItem = new JMenuItem("Move to Center");
        moveCenterItem.addActionListener(ev -> setLocationRelativeTo(null));
        JMenuItem toggleToCenterItem = new JMenuItem("Toggle to Center");
        toggleToCenterItem.addActionListener(ev -> toggleToCenter = !toggleToCenter);
        
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(ev -> doClose());

        contextMenu.add(alwaysOnTopItem);
        contextMenu.addSeparator();
        contextMenu.add(halfOpacityItem);
        contextMenu.addSeparator();
        contextMenu.add(moveCenterItem);
        contextMenu.add(toggleToCenterItem);
        contextMenu.addSeparator();
        contextMenu.add(closeItem);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clickPoint.setLocation(e.getPoint());
                resizeDir = getResizeDirection(e.getPoint());

                if (SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(VisualFrame.this, e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (resizeDir != 0) {
                        doResize(e);
                    } else {
                        Point screen = e.getLocationOnScreen();
                        setLocation(screen.x - clickPoint.x, screen.y - clickPoint.y);
                    }
                }
            }
        });
    }

    public VisualFrame (String title, JPanel panel, int width, int height) {
        this(title, panel, width, height, 7, 3);
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void updateShape() {
        setShape(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), roundRadius, roundRadius));
    }

    private void toggleOpacity() {
        halfOpacity = !halfOpacity;
        setOpacity(halfOpacity ? 0.5f : 1.0f);
    }

    private void doClose() {
        if (onClose != null) {
            onClose.run();
        }
        dispose();
    }

    private int getResizeDirection(Point p) {
        int w = getWidth();
        int h = getHeight();
        boolean left = p.x <= resizeMargin;
        boolean right = p.x >= w - resizeMargin;
        boolean top = p.y <= resizeMargin;
        boolean bottom = p.y >= h - resizeMargin;

        //  1=left, 2=right, 4=top, 8=bottom
        int dir = 0;
        if (left) dir |= 1;
        if (right) dir |= 2;
        if (top) dir |= 4;
        if (bottom) dir |= 8;
        return dir;
    }

    private void doResize(MouseEvent e) {
        Rectangle bounds = getBounds();
        Point screen = e.getLocationOnScreen();

        if ((resizeDir & 1) != 0) { // left
            int newW = bounds.x + bounds.width - screen.x;
            bounds.width = newW;
            bounds.x = screen.x;
        }
        if ((resizeDir & 2) != 0) { // right
            bounds.width = screen.x - bounds.x;
        }
        if ((resizeDir & 4) != 0) { // top
            int newH = bounds.y + bounds.height - screen.y;
            bounds.height = newH;
            bounds.y = screen.y;
        }
        if ((resizeDir & 8) != 0) { // bottom
            bounds.height = screen.y - bounds.y;
        }
        setBounds(bounds);
    }
}
