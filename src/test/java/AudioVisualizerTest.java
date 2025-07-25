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
import org.theko.sound.visualizers.WaveformVisualizer;

import visual.VisualFrame;

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

            AudioVisualizer spectrumVisualizer = new SpectrumVisualizer(60.0f);
            outMixer.addEffect(spectrumVisualizer);
            VisualFrame spectrumFrame = new VisualFrame("Spectrum", spectrumVisualizer.getPanel(), 720, 480);
            spectrumFrame.setVisible(true);

            AudioVisualizer waveformVisualizer = new WaveformVisualizer(60.0f);
            outMixer.addEffect(waveformVisualizer);
            VisualFrame waveformFrame = new VisualFrame("Waveform", waveformVisualizer.getPanel(), 320, 160);
            waveformFrame.setVisible(true);

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

            spectrumVisualizer.close();
            spectrumFrame.dispose();

            waveformVisualizer.close();
            waveformFrame.dispose();
        } catch (Exception e) {
            System.out.println("TEST PASS FAILED! " + e.getMessage());
            if (out != null) out.close(); // Usually doesn't throw an exception
            return;
        }

        System.out.println("TEST PASS!");
    }
}
