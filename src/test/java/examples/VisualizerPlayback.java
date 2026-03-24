package examples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;

import org.theko.sound.AudioMixer;
import org.theko.sound.SoundPlayer;
import org.theko.sound.codecs.AudioCodecNotFoundException;
import org.theko.sound.codecs.AudioTag;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.effects.BitcrusherEffect;
import org.theko.sound.effects.ResamplerEffect;
import org.theko.sound.events.OutputLayerEventType;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.resamplers.CubicResampleMethod;
import org.theko.sound.visualizers.AudioVisualizer;
import org.theko.sound.visualizers.ColorGradient;
import org.theko.sound.visualizers.SpectrumVisualizer;

import helpers.FileChooserHelper;

public class VisualizerPlayback {
    private static final int messageDisplayDuration = 2000;
    private String message = "";
    private long messageTimestamp = 0;

    private SoundPlayer player = new SoundPlayer();
    private String trackInfo = "";

    private JFrame frame;
    private AudioVisualizer visualizer;
    private BitcrusherEffect bitcrusher;

    private class OverlayPanel extends JPanel {
        final Font font = new Font("Monospaced", Font.BOLD, 14);
        final Color alphaBlack = new Color(0, 0, 0, 63);
        final Color alphaLightGray = new Color(128, 128, 128, 128);
        final Color alphaOrange = new Color(255, 128, 0, 128);
        final Color alphaMagenta = new Color(0, 255, 255, 128);
        final Color alphaWhite = new Color(255, 255, 255, 128);
        final Color alphaPurple = new Color(255, 0, 191, 128);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setFont(font);
            drawStatus(g2d);

            g2d.setColor(Color.WHITE);
            drawMultilineText(g2d, getPlayerInfo(), 10, g2d.getFontMetrics().getHeight(), font.getSize() + 2);
            drawMessage(g2d);
        }

        private void drawMultilineText(Graphics2D g2d, String text, int x, int y, int lineHeight) {
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                g2d.drawString(lines[i], x, y + i * lineHeight);
            }
        }

        private void drawStatus(Graphics2D g2d) {
            FontMetrics fm = g2d.getFontMetrics();

            int y = fm.getHeight();
            int x = getWidth() - fm.stringWidth("IPLBT__"); // add padding

            x = drawStatusText(!player.isInitialized() || !player.hasAudioData(),
                    g2d, fm, alphaLightGray, "I", x, y);
            x = drawStatusText(player.isPlaying(),
                    g2d, fm, alphaWhite, "P", x, y);
            x = drawStatusText(player.isLooping(),
                    g2d, fm, alphaMagenta, "L", x, y);
            x = drawStatusText(bitcrusher.getEnableControl().getValue(),
                    g2d, fm, alphaPurple, "B", x, y);
            x = drawStatusText(frame.isAlwaysOnTop(),
                    g2d, fm, alphaOrange, "T", x, y);
        }

        private int drawStatusText(boolean cond, Graphics2D g2d, FontMetrics fm,
                            Color color, String txt, int x, int y) {
            if (!cond) return x;

            int w = fm.stringWidth(txt);
            // background rect
            g2d.setColor(alphaBlack);
            g2d.fillRect(x, y - fm.getAscent(), w, fm.getHeight());
            g2d.setColor(color);
            g2d.drawString(txt, x, y);
            return x + w;
        }

        private void drawMessage(Graphics2D g2d) {
            if (!message.isEmpty() && System.currentTimeMillis() - messageTimestamp < messageDisplayDuration) {
                int msgWidth = g2d.getFontMetrics().stringWidth(message);
                int msgHeight = g2d.getFontMetrics().getHeight();

                int w = getWidth();
                int h = getHeight();
                int x = (w - msgWidth) / 2;
                int y = (h - msgHeight) / 2;

                float alpha = getMessageAlpha();

                g2d.setColor(new Color(0, 0, 0, (int) (150 * alpha)));
                g2d.fillRoundRect(x - 10, y - 20, msgWidth + 20, 30, 10, 10);
                g2d.setColor(new Color(255, 255, 255, (int) (255 * alpha)));
                g2d.drawString(message, x, y);
            }
        }

        private float getMessageAlpha() {
            if (message.isEmpty()) return 0f;
            float alpha = 1.0f - (float)(System.currentTimeMillis() - messageTimestamp) / messageDisplayDuration;
            return Math.max(0f, Math.min(1f, alpha));
        }
    }

    private void message(String msg) {
        message = msg;
        messageTimestamp = System.currentTimeMillis();
    }

    private VisualizerPlayback() {
        try {
            player.initialize();
            openAudio(null, false);

            ResamplerEffect resampler = new ResamplerEffect(new CubicResampleMethod());
            player.setResamplerEffect(resampler);

            AudioMixer mixer = player.getInnerMixer();

            bitcrusher = new BitcrusherEffect();
            bitcrusher.getBitdepth().setValue(6);
            bitcrusher.getSampleRateReduction().setValue(4000f);
            bitcrusher.getEnableControl().setValue(false);
            mixer.addEffect(bitcrusher);

            SpectrumVisualizer spectrum = new SpectrumVisualizer(120.0f);
            spectrum.setVolumeColorProcessor(ColorGradient.VIRIDIS_COLOR_MAP.getVolumeColorProcessor());
            spectrum.setFftWindowSize(4096);
            spectrum.setFrequencyScale(1.5f);
            mixer.addEffect(spectrum);

            visualizer = spectrum;

            SwingUtilities.invokeLater(() -> {
                frame = new JFrame(getReadableName(visualizer.getClass().getSimpleName()));
                frame.setSize(800, 400);
                frame.setMinimumSize(new Dimension(150, 100));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                JPanel visualizerPanel = visualizer.getPanel();
                visualizerPanel.setDoubleBuffered(true);
                visualizerPanel.setOpaque(true);
                visualizerPanel.setBackground(Color.BLACK);
                visualizerPanel.setLayout(new OverlayLayout(visualizerPanel));

                JPanel overlay = new OverlayPanel();
                overlay.setOpaque(false);
                visualizerPanel.add(overlay);

                frame.add(visualizerPanel);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        player.close();
                        visualizer.close();
                    }
                });
                addKeyListeners();
                addDragNDrop();
                frame.setFocusable(true);
                frame.requestFocusInWindow();
                frame.setVisible(true);
            });

            player.getOutputLayerListeners().addConsumer(
                OutputLayerEventType.DEVICE_INVALIDATED, (type, event) -> {
                    message("Audio device invalidated or inaccessible.");
                    player.stop();
                }
            );
            player.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean openAudio(File file, boolean background) throws FileNotFoundException, AudioCodecNotFoundException {
        final File audioFile = (file == null ? FileChooserHelper.chooseAudioFile() : file);
        if (audioFile == null)
            return false;

        // if the player was not initialized, then start it
        boolean wasPlaying = player.isPlaying() || (!player.isInitialized() || !player.hasAudioData());

        if (background) {
            String frameTitle = frame.getTitle();
            frame.setTitle("Opening...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {

                @Override
                protected Void doInBackground() throws Exception {
                    player.open(audioFile);
                    if (wasPlaying)
                        player.start();
                    trackInfo = getTrackInfo(player);
                    return null;
                }

                @Override
                protected void done() {
                    frame.setTitle(frameTitle);
                    try {
                        get();
                    } catch (Exception e) {
                        message("Failed to open audio: " + e.getMessage());
                    }
                }
            };
            worker.execute();
            frame.requestFocusInWindow();
        } else {
            player.open(audioFile);
            if (wasPlaying)
                player.start();
            trackInfo = getTrackInfo(player);
        }
        return true;
    }

    private String getPlayerInfo() {
        if (!player.isInitialized() || !player.hasAudioData()) {
            return "Not opened.\nPress 'O' to open an audio file.";
        }
        String playbackInfo = getPlaybackInfo(player);
        if (trackInfo == null || trackInfo.isEmpty()) {
            trackInfo = getTrackInfo(player);
        }
        return "%s\n%s".formatted(trackInfo, playbackInfo);
    }

    private static String getTrackInfo(SoundPlayer player) {
        String title = player.getMetadata().getValue(AudioTag.TITLE);
        String artist = player.getMetadata().getValue(AudioTag.ARTIST);
        return "%s\n%s".formatted(
            title != null ? title : "Unknown Title",
            artist != null ? artist : "Unknown Artist"
        );
    }

    private static String getPlaybackInfo(SoundPlayer player) {
        int pos = (int) player.getSecondsPosition();
        int dur = (int) player.getDuration();

        float speed = player.getSpeedControl().getValue();
        boolean isPlaying = player.isPlaying();

        String stopped = isPlaying ? "" : " [Stopped]";
        String speedStr = speed != 1f ? "\nSpeed: x%.2f".formatted(speed) : "";

        String realDur = "";
        if (speed != 1f) {
            int adj = (int) (dur / speed);
            realDur = "(" + formatTime(adj) + ")";
        }

        return "%s / %s %s%s%s".formatted(
                formatTime(pos), formatTime(dur), realDur, stopped, speedStr
        );
    }

    private static String formatTime(int seconds) {
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }

    private String getReadableName(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.replaceAll("(?<!^)([A-Z])", " $1");
    }

    private boolean ensureOpen() {
        if (!player.isInitialized() || !player.hasAudioData()) {
            message("Not opened.\nPress 'O' to open an audio file.");
            return false;
        }
        if (!player.isOpen()) {
            System.out.println("Reopening output layer...");
            try {
                player.reopen();
                return true;
            } catch (Exception ex) {
                message("Failed to open output layer.");
                return false;
            }
        }
        return true;
    }

    // UI methods

    private void addKeyListeners() {
        frame.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            FloatControl speed = player.getSpeedControl();
            switch (e.getKeyCode()) {
                // Play / Pause toggle
                case KeyEvent.VK_SPACE, KeyEvent.VK_PAUSE, KeyEvent.VK_P -> {
                    if (!ensureOpen()) return;
                    if (player.isPlaying()) player.stop();
                    else player.start();
                }
                // Speed controls
                case KeyEvent.VK_A, KeyEvent.VK_LEFT ->     speed.setValue(0.5f);
                case KeyEvent.VK_D, KeyEvent.VK_RIGHT ->    speed.setValue(2.0f);
                case KeyEvent.VK_S, KeyEvent.VK_DOWN ->     speed.setValue(speed.getValue() - 0.01f);
                case KeyEvent.VK_W, KeyEvent.VK_UP ->       speed.setValue(speed.getValue() + 0.01f);
                // Reset position
                case KeyEvent.VK_R, KeyEvent.VK_HOME, KeyEvent.VK_BACK_SPACE -> {
                    player.setFramePosition(0);
                    message("Restarted track");
                }
                // Loop toggle
                case KeyEvent.VK_L -> {
                    player.setLoop(!player.isLooping());
                    message("Looping %s".formatted(player.isLooping() ? "enabled" : "disabled"));
                }
                // Bitcrusher toggle
                case KeyEvent.VK_B -> {
                    bitcrusher.getEnableControl().setValue(!bitcrusher.getEnableControl().getValue());
                    message("Bitcrusher %s".formatted(bitcrusher.getEnableControl().getValue() ? "enabled" : "disabled"));
                }
                // Open new audio track
                case KeyEvent.VK_O -> {
                    try {
                        if (openAudio(null, true)) message("Opening new audio track");
                        else message("No audio file selected");
                    } catch (FileNotFoundException ex) {
                        message("File not found");
                    } catch (AudioCodecNotFoundException ex) {
                        message("Unsupported audio codec");
                    }
                }
                // Toggle always on top
                case KeyEvent.VK_T -> {
                    frame.setAlwaysOnTop(!frame.isAlwaysOnTop());
                    message("Always on top %s".formatted(frame.isAlwaysOnTop() ? "enabled" : "disabled"));
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            var speed = player.getSpeedControl();
            switch (e.getKeyCode()) {
                case KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT ->
                    speed.setValue(1.0f);
            }
        }
    });
    }

    private void addDragNDrop() {
        visualizer.getPanel().setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    Transferable t = support.getTransferable();
                    List<File> f = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    openAudio(f.get(0), true);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }

    public static void main(String[] args) {
        AudioSystemProperties.runStaticInit();
        new VisualizerPlayback();
    }
}