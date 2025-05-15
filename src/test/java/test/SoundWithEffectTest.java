package test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.theko.sound.SoundPlayer;
import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioEffectException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.direct.javasound.JavaSoundDevice;
import org.theko.sound.effects.AudioLimiter;

import test.org.theko.sound.SharedFunctions;

public class SoundWithEffectTest {
    public static void main(String[] args) {
        SoundPlayer sound = null;
        try {
            sound = new SoundPlayer();
            sound.open(SharedFunctions.chooseAudioFile(), SoundPlayer.BUFFER_SIZE_32MS);

            JavaSoundDevice jsd = new JavaSoundDevice();
            System.out.println("The best format: " + jsd.getFormatForPort(jsd.getDefaultPort(AudioFlow.OUT, AudioFormat.LOWEST_QUALITY_FORMAT).get()));
    
            addEffects(sound);
            sound.start();

            Thread.sleep(sound.getMicrosecondLength() * 1000);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (AudioPortsNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedAudioFormatException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (sound != null) {
                sound.close();
            }
        }
    }

    private static void addEffects(SoundPlayer sound) {
        AudioFormat format = sound.getAudioFormat();
        List<AudioEffect> effects = getEffects(format);

        for (AudioEffect effect : effects) {
            try {
                sound.addEffect(effect);
            } catch (UnsupportedAudioEffectException e) {
                System.err.println("Error adding effect: " + e.getMessage());
            }
        }
    }

    private static List<AudioEffect> getEffects(AudioFormat format) {
        AudioLimiter limiter1 = new AudioLimiter(format);
    
        return new ArrayList<>(List.of(limiter1));
    }
}
