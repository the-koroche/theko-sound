import org.theko.sound.AudioFormat;
import org.theko.sound.AudioInputLine;
import org.theko.sound.AudioMixer;
import org.theko.sound.AudioMixerOutput;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.visualizers.AudioVisualizer;
import org.theko.sound.visualizers.SpectrumVisualizer;

import visual.VisualFrame;

public class AudioInputLineTest {
    public static void main(String[] args) {
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
            
            AudioInputLine input = new AudioInputLine();
            input.open(AudioFormat.NORMAL_QUALITY_FORMAT);

            outMixer.addInput(input);

            try {
                out.open(AudioFormat.NORMAL_QUALITY_FORMAT);
            } catch (AudioPortsNotFoundException | UnsupportedAudioFormatException e) {
                System.out.println("TEST PASS FAILED! Audio ports not found, or unsupported audio format.");
                if (out != null) out.close();
                return;
            }

            input.start();

            AudioVisualizer vis = new SpectrumVisualizer(60.0f);
            outMixer.addEffect(vis);

            VisualFrame v = new VisualFrame("Microphone In", vis.getPanel(), 640, 320);
            v.setVisible(true);

            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("TEST PASS FAILED! Audio input line interrupted.");
                if (out != null) out.close();
                return;
            }

            input.stop();
            input.close();
            out.close();

            vis.close();
            v.setVisible(false);
            v.dispose();
        } catch (Exception e) {
            System.out.println("TEST PASS FAILED! " + e.getMessage());
            e.printStackTrace();
            if (out != null) out.close(); // Usually doesn't throw an exception
            return;
        }

        System.out.println("TEST PASS!");
    }
}
