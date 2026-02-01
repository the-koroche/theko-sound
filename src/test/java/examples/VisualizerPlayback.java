package examples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import org.theko.sound.AudioMixer;
import org.theko.sound.SoundPlayer;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.codec.AudioTag;
import org.theko.sound.effects.BitcrusherEffect;
import org.theko.sound.effects.ResamplerEffect;
import org.theko.sound.event.OutputLayerEventType;
import org.theko.sound.resampling.LanczosResampleMethod;
import org.theko.sound.visualizers.ColorGradient;
import org.theko.sound.visualizers.SpectrumVisualizer;

import helpers.FileChooserHelper;

public class VisualizerPlayback {
    private SoundPlayer player = new SoundPlayer();
    private String trackInfo = "";
    private static final int messageDisplayDuration = 2000;
    private String message = "";
    private long messageTimestamp = 0;

    private JFrame frame;
    private SpectrumVisualizer spectrum;
    private BitcrusherEffect bitcrusher;

    private class OverlayPanel extends JPanel {
        Font font = new Font("Monospaced", Font.BOLD, 14);
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            g2d.setFont(font);
            drawMultilineText(g2d, trackInfo + "\n" + getPlaybackInfo(player), 10, 20, font.getSize() + 2);
            drawMessage(g2d);
        }

        private void drawMultilineText(Graphics2D g2d, String text, int x, int y, int lineHeight) {
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                g2d.drawString(lines[i], x, y + i * lineHeight);
            }
        }

        private void drawMessage(Graphics2D g2d) {
            if (!message.isEmpty() && System.currentTimeMillis() - messageTimestamp < messageDisplayDuration) {
                int msgWidth = g2d.getFontMetrics().stringWidth(message);
                int msgHeight = g2d.getFontMetrics().getHeight();

                int w = (getParent() != null ? getParent().getWidth() : getWidth());
                int h = (getParent() != null ? getParent().getHeight() : getHeight());
                int x = (w - msgWidth) / 2;
                int y = (h - msgHeight) / 2;

                float alpha = getAlpha();

                g2d.setColor(new Color(0, 0, 0, (int) (150 * alpha)));
                g2d.fillRoundRect(x - 10, y - 20, msgWidth + 20, 30, 10, 10);
                g2d.setColor(new Color(255, 255, 255, (int) (255 * alpha)));
                g2d.drawString(message, x, y);
            }
        }

        private float getAlpha() {
            if (message.isEmpty()) return 0f;
            float alpha = 1.0f - (float)(System.currentTimeMillis() - messageTimestamp) / messageDisplayDuration;
            return Math.max(0f, Math.min(1f, alpha));
        }
    }

    private VisualizerPlayback() {
        try {
            if (!openAudio()) {
                return;
            }

            ResamplerEffect resampler = new ResamplerEffect(new LanczosResampleMethod(), 2);
            player.setResamplerEffect(resampler);
            
            AudioMixer mixer = player.getInnerMixer();
            
            bitcrusher = new BitcrusherEffect();
            bitcrusher.getBitdepth().setValue(6);
            bitcrusher.getSampleRateReduction().setValue(4000f);
            bitcrusher.getEnableControl().setValue(false);
            mixer.addEffect(bitcrusher);

            spectrum = new SpectrumVisualizer(120.0f);
            spectrum.setVolumeColorProcessor(ColorGradient.VIRIDIS_COLOR_MAP.getVolumeColorProcessor());
            spectrum.setFftWindowSize(4096);
            spectrum.setFrequencyScale(1.5f);
            mixer.addEffect(spectrum);

            SwingUtilities.invokeLater(() -> {
                frame = new JFrame("Spectrum");
                frame.setSize(800, 400);
                frame.setMinimumSize(new Dimension(150, 100));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                spectrum.getPanel().setDoubleBuffered(true);
                spectrum.getPanel().setBackground(Color.BLACK);
                spectrum.getPanel().setOpaque(true);

                JPanel spectrumPanel = spectrum.getPanel();
                spectrumPanel.setLayout(new OverlayLayout(spectrumPanel));

                JPanel overlay = new OverlayPanel();
                overlay.setOpaque(false);
                spectrumPanel.add(overlay);

                frame.add(spectrumPanel);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        player.close();
                        spectrum.close();
                    }
                });
                addKeyListeners();
                frame.setFocusable(true);
                frame.requestFocusInWindow();
                frame.setVisible(true);
            });

            player.getOutputLayerListenersManager().addConsumer(
                OutputLayerEventType.DEVICE_INVALIDATED, event -> {
                    message("Audio device invalidated or inaccessible.");
                    player.stop();
                }
            );
            player.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { if (player != null)   player.close();     } catch (Exception ignored) {}
            try { if (spectrum != null) spectrum.close();   } catch (Exception ignored) {}
            try { if (frame != null)    frame.dispose();    } catch (Exception ignored) {}
        }
    }

    private boolean openAudio() {
        try {
            File audioFile = FileChooserHelper.chooseAudioFile();
            if (audioFile == null) {
                System.out.println("No audio file selected.");
                return false;
            }
            player.open(audioFile);
            trackInfo = getTrackInfo(player);
            return true;
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
            return false;
        } catch (AudioCodecNotFoundException e) {
            // AudioCodecNotFoundException used to handle an unsupported audio extensions
            System.err.println("Provided audio file is unsupported.");
            return false;
        }
    }

    private static String getTrackInfo(SoundPlayer player) {
        String title = player.getTags().getValue(AudioTag.TITLE);
        String artist = player.getTags().getValue(AudioTag.ARTIST);
        return "%s\n%s".formatted(
            title != null ? title : "Unknown Title",
            artist != null ? artist : "Unknown Artist"
        );
    }

    private static String getPlaybackInfo(SoundPlayer player) {
        int posSec = (int) player.getSecondsPosition();
        int durSec = (int) player.getDuration();

        int posMin = posSec / 60;
        int posRemSec = posSec % 60;

        int durMin = durSec / 60;
        int durRemSec = durSec % 60;

        float speed = player.getSpeedControl().getValue();

        return "%02d:%02d / %02d:%02d %s%s".formatted(posMin, posRemSec, durMin, durRemSec,
            player.isPlaying() ? "" : "[Stopped]",
            speed != 1f ? "\nSpeed: x%.2f".formatted(speed) : ""
        );
    }

    private boolean ensureOpen() {
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

    private void addKeyListeners() {
        frame.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            var speed = player.getSpeedControl();
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
                    bitcrusher.getEnableControl().setValue(!bitcrusher.getEnableControl().isEnabled());
                    message("Bitcrusher %s".formatted(bitcrusher.getEnableControl().isEnabled() ? "enabled" : "disabled"));
                }
                // Open new audio track
                case KeyEvent.VK_O -> {
                    if (openAudio()) message("Opened new audio track");
                    else message("Failed to open audio track");
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

    private void message(String msg) {
        message = msg;
        messageTimestamp = System.currentTimeMillis();   
    }

    public static void main(String[] args) {
        new VisualizerPlayback();
    }
}