import java.io.FileNotFoundException;

import org.theko.sound.AudioMixer;
import org.theko.sound.AudioMixerOutput;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.effects.AudioEqualizer;
import org.theko.sound.effects.AudioLimiter;
import org.theko.sound.effects.BitcrusherEffect;
import org.theko.sound.effects.LowPassEffect;
import org.theko.sound.effects.LowPassRCFilter;
import org.theko.sound.generator.NoiseGenerator;

public class LowPassEffectTest {
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

            AudioMixer soundMixer = new AudioMixer();
            SoundSource sound = new SoundSource();
            soundMixer.addInput(sound);
            soundMixer.getEnableEffectsControl().setValue(true);
            
            AudioEqualizer eq = new AudioEqualizer(3);
            soundMixer.addEffect(eq);

            eq.getBand(1).getFrequency().setValue(1000f);
            eq.getBand(1).getGain().setValue(0.0f);
            eq.getBand(0).getGain().setValue(2.0f);
            eq.getBand(2).getGain().setValue(0.0f);

            BitcrusherEffect bitcrusherEffect = new BitcrusherEffect();
            bitcrusherEffect.getBitdepth().setValue(4f);
            bitcrusherEffect.getSampleRateReduction().setValue(2000f);

            //soundMixer.addEffect(bitcrusherEffect);
            
            AudioMixer noiseMixer = new AudioMixer();
            noiseMixer.addInput(new NoiseGenerator());
            noiseMixer.getPostGainControl().setValue(0.02f);

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

            outMixer.addInput(soundMixer);
            //outMixer.addInput(noiseMixer);

            sound.start();

            try {
                Thread.sleep(Math.min(1000000000, (int)(sound.getDuration() * 1000)));
            } catch (InterruptedException e) {
                System.out.println("Interrupted.");
            }

            sound.stop();
            sound.close();
            out.close();
        } catch (Exception e) {
            System.out.println("TEST PASS FAILED! " + e.getMessage());
            if (out != null) out.close(); // Usually doesn't throw an exception
            return;
        }

        System.out.println("TEST PASS!");
    }
}
