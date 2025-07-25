import java.io.FileNotFoundException;

import org.theko.sound.AudioMixer;
import org.theko.sound.AudioMixerOutput;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.effects.ChannelDelayEffect;
import org.theko.sound.effects.IncompatibleEffectTypeException;
import org.theko.sound.effects.MultipleVaryingSizeEffectsException;

public class ChannelDelayEffectTest {
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
            
            ChannelDelayEffect delay = new ChannelDelayEffect();
            delay.getDelayLeft().setValue(0.1f);
            delay.getDelayRight().setValue(0.0f);

            try {
                outMixer.addEffect(delay);
            } catch (IncompatibleEffectTypeException | MultipleVaryingSizeEffectsException e) {
                System.out.println("TEST PASS FAILED! Incompatible effect type.");
                if (out != null) out.close();
                return;
            }

            sound.start();

            try {
                //Thread.sleep(Math.min(10000, (int)(sound.getDuration() * 1000)));
                Thread.sleep((int)(sound.getDuration() * 1000));
            } catch (InterruptedException e) {
                System.out.println("Interrupted.");
            }

            sound.stop();
            sound.close();
            out.close();
        } catch (Exception e) {
            System.out.println("TEST PASS FAILED! " + e.getMessage());
            if (out != null) out.close();
            return;
        }

        System.out.println("TEST PASS!");
    }
}
