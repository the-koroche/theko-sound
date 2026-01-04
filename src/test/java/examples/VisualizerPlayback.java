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
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.theko.sound.resampling.LanczosResampleMethod;
//import org.theko.sound.visualizers.ColorGradient;
import org.theko.sound.visualizers.SpectrumVisualizer;

import helpers.FileChooserHelper;

public class VisualizerPlayback {
    public static void main(String[] args) {
        SoundPlayer player = new SoundPlayer();
        SpectrumVisualizer spectrum = new SpectrumVisualizer(120.0f);
        JFrame frame = new JFrame("Spectrum");
        AtomicBoolean isStopped = new AtomicBoolean(false);

        try {
            File audioFile = FileChooserHelper.chooseAudioFile();
            if (audioFile == null) {
                System.out.println("No audio file selected.");
                return;
            }
            player.open(audioFile);
            String info = getTrackInfo(player);

            ResamplerEffect resampler = new ResamplerEffect(new LanczosResampleMethod(), 2);
            player.setResamplerEffect(resampler);
            
            AudioMixer mixer = player.getInnerMixer();
            
            BitcrusherEffect bitcrusher = new BitcrusherEffect();
            bitcrusher.getBitdepth().setValue(6);
            bitcrusher.getSampleRateReduction().setValue(4000f);
            bitcrusher.getEnableControl().setValue(false);
            mixer.addEffect(bitcrusher);

            //spectrum.setVolumeColorProcessor(ColorGradient.VIRIDIS_COLOR_MAP.getVolumeColorProcessor());
            spectrum.setFftWindowSize(8192);
            spectrum.setFrequencyScale(1.5f);
            mixer.addEffect(spectrum);

            SwingUtilities.invokeLater(() -> {
                frame.setSize(800, 400);
                frame.setMinimumSize(new Dimension(150, 100));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                spectrum.getPanel().setDoubleBuffered(true);
                spectrum.getPanel().setBackground(Color.BLACK);
                spectrum.getPanel().setOpaque(true);
                JPanel spectrumPanel = spectrum.getPanel();
                spectrumPanel.setLayout(new OverlayLayout(spectrumPanel));

                JPanel overlay = new JPanel() {
                    Font font = new Font("Monospaced", Font.BOLD, 14);

                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g.setColor(Color.WHITE);
                        g.setFont(font);
                        drawMultilineText(g, info + "\n" + getPlaybackInfo(player), 10, 20, 16);
                    }

                    private void drawMultilineText(Graphics g, String text, int x, int y, int lineHeight) {
                        String[] lines = text.split("\n");
                        for (int i = 0; i < lines.length; i++) {
                            g.drawString(lines[i], x, y + i * lineHeight);
                        }
                    }
                };
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
                frame.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_SPACE:
                                if (player.isPlaying()) {
                                    isStopped.set(true);
                                    player.stop();
                                } else {
                                    player.start();
                                    isStopped.set(false);
                                }
                                break;
                            case KeyEvent.VK_A:
                                player.getSpeedControl().setValue(0.5f);
                                break;
                            case KeyEvent.VK_D:
                                player.getSpeedControl().setValue(2.0f);
                                break;
                            case KeyEvent.VK_S:
                                player.getSpeedControl().setValue(player.getSpeedControl().getValue() - 0.01f);
                                break;
                            case KeyEvent.VK_W:
                                player.getSpeedControl().setValue(player.getSpeedControl().getValue() + 0.01f);
                                break;
                            
                            case KeyEvent.VK_R:
                                player.setFramePosition(0);
                                break;
                            
                            case KeyEvent.VK_B:
                                bitcrusher.getEnableControl().setValue(!bitcrusher.getEnableControl().isEnabled());
                                break;
                        }
                    }

                    @Override public void keyReleased(KeyEvent e) {
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_A: case KeyEvent.VK_D:
                                player.getSpeedControl().setValue(1.0f);
                                break;
                        }
                    }
                });
                frame.setVisible(true);
            });

            player.start();
            while (player.isOpen() && (player.isPlaying() || isStopped.get())) {
                Thread.sleep(1000);
            }
            System.out.println();
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
        } catch (AudioCodecNotFoundException e) {
            // AudioCodecNotFoundException used to handle an unsupported audio extensions
            System.err.println("Provided audio file is unsupported.");
        } catch (InterruptedException e) {
            // When the playback is interrupted
            Thread.currentThread().interrupt();
            System.err.println("Playback interrupted.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Shutting down...");
            if (player != null && player.isOpen()) {
                player.close();
            }
            spectrum.close();
            frame.dispose();
        }
    }

    private static String getTrackInfo(SoundPlayer player) {
        String title = player.getTags().getValue(AudioTag.TITLE);
        String artist = player.getTags().getValue(AudioTag.ARTIST);
        return "%s - %s".formatted(
            artist != null ? artist : "Unknown Artist",
            title != null ? title : "Unknown Title");
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
            player.isPlaying() ? "" : "[Paused]",
            speed != 1f ? "\nSpeed: x%.2f".formatted(speed) : ""
        );
    }
}
