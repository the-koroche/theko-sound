package test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.theko.sound.SoundPlayer;

import test.org.theko.sound.SharedFunctions;

public class MultipleSounds {
    private static final int SOUNDS_COUNT = 2;

    public static void main(String[] args) {
        List<SoundPlayer> sounds = new ArrayList<>();
        try {
            for (int i = 0; i < SOUNDS_COUNT; i++) {
                SoundPlayer sound = new SoundPlayer();
                sound.open(SharedFunctions.chooseAudioFile(), SoundPlayer.BUFFER_SIZE_128MS);
                sounds.add(sound);
            }
    
            for (SoundPlayer sound : sounds) {
                sound.start();
            }
            Thread.sleep(20000);
    
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            for (SoundPlayer sound : sounds) {
                if (sound != null) {
                    sound.close();
                }
            }
        }
    }
}
