import java.io.FileNotFoundException;

import org.theko.sound.AudioMixer;
import org.theko.sound.AudioMixerOutput;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.visualizers.AudioVisualizer;
import org.theko.sound.visualizers.SpectrumVisualizer;

import visual.VisualFrame;

public class AudioVisualizerTest {
    public static void main(String[] args) {
        System.setProperty("org.theko.sound.audioOutputLayer.defaultBufferSize", "8192");
        
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

            AudioVisualizer vis = new SpectrumVisualizer(60.0f);
            outMixer.addEffect(vis);

            VisualFrame frame = new VisualFrame("Audio Visualizer", vis.getPanel(), 640, 480);

            sound.start();

            try {
                while (sound.isPlaying()) {
                    Thread.sleep(100);
                    //wave.getDuration().setValue(wave.getDuration().getValue() * 0.95f);
                }
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
