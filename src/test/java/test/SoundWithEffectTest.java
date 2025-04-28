package test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.theko.sound.SoundPlayer;
import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.UnsupportedAudioEffectException;
import org.theko.sound.effects.AudioCompressor;
import org.theko.sound.effects.AudioLimiter;

import test.org.theko.sound.SharedFunctions;

public class SoundWithEffectTest {
    public static void main(String[] args) {
        SoundPlayer sound = null;
        try {
            sound = new SoundPlayer();
            sound.open(SharedFunctions.chooseAudioFile(), SoundPlayer.BUFFER_SIZE_1024MS);
    
            addEffects(sound);
    
            sound.getFloatController("Speed").setValue(0.8f);
            sound.start();
    
            while (sound.isPlaying()) {
                printSoundPosition(sound);
                Thread.sleep(20);
            }
    
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            if (sound != null) {
                System.out.println("Closing sound...");
                sound.close();
            }
        }
    }
    
    private static void printSoundPosition(SoundPlayer sound) {
        double positionSeconds = sound.getMicrosecondPosition() / 1_000_000.0;
        double modPositionSeconds = sound.getModifiedMicrosecondPosition() / 1_000_000.0;
        System.out.print(String.format("position: %.3f s  | mod: %.3f s    \r", positionSeconds, modPositionSeconds));
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
        AudioLimiter limiter = new AudioLimiter(format);
        System.out.println(limiter.getAllControllers());
    
        return new ArrayList<>(List.of(limiter));
    }
}
