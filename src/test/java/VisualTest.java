// package test;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.JFrame;

import org.theko.sound.PlaybackException;
import org.theko.sound.SoundPlayer;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioEffectException;
import org.theko.sound.visualizers.AudioVisualizationUtilities;
import org.theko.sound.visualizers.SpectrogramVisualizer;

public class VisualTest {
    public static void main(String[] args) {
        try (SoundPlayer sound = new SoundPlayer()) {
            String file = chooseFile();
            
            // open audio file, with 64 ms buffer size
            sound.open(new File(file), SoundSource.BUFFER_SIZE_64MS);

            // Add visualizer
            new VisualizerFrame(sound);

            // change playback speed
            // value 0.8693 used to show, how resampler can handle various values
            //sound.getSpeedController().setValue(0.9f);
            sound.start(); // start the playback

            // create new thread, to show playaback position in seconds
            new Thread(() -> {
                while (sound.isPlaying()) {
                    // sound.getMicrosecondPosition() returns the position without the speed change.
                    // sound.getModifiedMicrosecondPosition() returns the position with the speed change.
                    System.out.println((sound.getModifiedMicrosecondPosition() / 1_000_000.f) + " s");
                try {
                    Thread.sleep(500); // wait some time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }}
            }).start();

            Thread.sleep(sound.getModifiedMicrosecondLength() / 1000); // wait for sound to finish
            sound.stop(); // stop the playback
            // no need to close due to try-with-resources and AutoCloseable
            // sound.close(); // close the sound player
        } catch (PlaybackException | InterruptedException | FileNotFoundException | UnsupportedAudioEffectException e) {
            e.printStackTrace();
        }
    }

    // Helper method, to choose audio file
    private static String chooseFile() {
        FileDialog dialog = new FileDialog((Frame) null, "Select an audio file", FileDialog.LOAD);
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return null; // User closed dialog
        }

        String path = dialog.getDirectory() + dialog.getFile();
        dialog.dispose();
        return path;
    }

    private static class VisualizerFrame extends JFrame {
        private int lastX, lastY;

        public VisualizerFrame (SoundPlayer player) throws UnsupportedAudioEffectException {
            int fftishka = 16384;
            SpectrogramVisualizer spectogram = new SpectrogramVisualizer(player.getAudioFormat()); //, fftishka, (int)(fftishka*0.1));
            player.addEffect(spectogram);
            JFrame frame = new JFrame();
            //spectogram.setOrientation(Orientation.BOTTOM);
            //spectogram.setFlipFrequencies(false);
            //spectogram.setWaterfallColor(SpectrogramVisualizer.INFERNO_COLOR);
            frame.add(spectogram.getPanel());
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(true);
            frame.setBackground(new Color(0, 0, 0, 0));
            frame.setAlwaysOnTop(true);
            frame.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastX = e.getXOnScreen() - frame.getX();
                    lastY = e.getYOnScreen() - frame.getY();
                }
            });
            frame.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    frame.setLocation(e.getXOnScreen() - lastX, e.getYOnScreen() - lastY);
                }
            });
            frame.setVisible(true);
        }
    }
}
