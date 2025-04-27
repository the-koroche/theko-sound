package test;

import java.io.File;
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
        try {
            SoundPlayer sound = new SoundPlayer();
            sound.open(SharedFunctions.chooseAudioFile(), SoundPlayer.BUFFER_SIZE_1024MS);

            //addEffects(sound);
            
            sound.getSpeedController().setValue(1.4f);
            sound.start();

            while (sound.isPlaying()) {
                System.out.print("position: " + sound.getMicrosecondPosition() / 1_000_000.0 + " s  | mod: " + sound.getModifiedMicrosecondPosition() / 1_000_000.0 + " s    \r");
                try {
                    Thread.sleep(20); // Print every second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            sound.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
        // Example: Create a compressor effect
        AudioCompressor compressor = new AudioCompressor(format);
        compressor.getThreshold().setValue(-10.0f); // Set threshold to -30 dB
        compressor.getRatio().setValue(2.0f); // Set compression ratio to 2:1
        compressor.getAttack().setValue(0.01f); // Set attack time to 10 ms
        compressor.getRelease().setValue(0.1f); // Set release time to 100 ms
        compressor.getMakeupGain().setValue(36.0f); // Добавляем +6 dB усиления

        AudioLimiter limiter = new AudioLimiter(format);
    
        return new ArrayList<>(List.of(compressor, limiter));
    }
}
