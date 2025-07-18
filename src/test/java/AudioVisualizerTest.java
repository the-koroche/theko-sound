import java.io.FileNotFoundException;

import javax.swing.JFrame;

import org.theko.sound.AudioMixer;
import org.theko.sound.AudioMixerOutput;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.visualizer.WaveformVisualizer;

public class AudioVisualizerTest {
    public static void main(String[] args) {
        System.setProperty("org.theko.sound.audioOutputLayer.defaultBufferSize", "1024");

        AudioMixerOutput out = null;
        try {
            try {
                out = new AudioMixerOutput();
            } catch (AudioBackendCreationException | AudioBackendNotFoundException e) {
                System.out.println("TEST PASS FAILED! Audio backend creation failed.");
                return;
            }

            AudioMixer outMixer = new AudioMixer();
            out.setMixer(outMixer);

            SoundSource sound = new SoundSource();

            try {
                sound.open(SharedFunctions.chooseAudioFile());
            } catch (FileNotFoundException | AudioCodecNotFoundException e) {
                System.out.println("TEST PASS FAILED! File or Audio codec not found.");
                if (out != null) out.close();
                return;
            }

            try {
                out.open(sound.getAudioFormat());
            } catch (AudioPortsNotFoundException | UnsupportedAudioFormatException e) {
                System.out.println("TEST PASS FAILED! Audio ports not found, or unsupported audio format.");
                if (out != null) out.close();
                return;
            }

            outMixer.addInput(sound);

            WaveformVisualizer wave = new WaveformVisualizer(30.0f);
            outMixer.addEffect(wave);

            JFrame frame = new JFrame("Audio Visualizer Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(wave.getPanel());
            frame.setSize(1920, 1080);
            frame.setUndecorated(true);
            frame.setBackground(new java.awt.Color(0, 0, 0, 0));
            frame.setVisible(true);

            sound.start();

            try {
                Thread.sleep(Math.min(50000, (int)(sound.getDuration() * 1000)));
            } catch (InterruptedException e) {
                System.out.println("Interrupted.");
            }

            sound.stop();
            sound.close();
            out.close();
            frame.dispose();
        } catch (Exception e) {
            System.out.println("TEST PASS FAILED! " + e.getMessage());
            if (out != null) out.close(); // Usually doesn't throw an exception
            return;
        }

        System.out.println("TEST PASS!");
    }
}
