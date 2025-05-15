package test;

import java.io.FileNotFoundException;

import org.theko.sound.SoundPlayer;

import test.org.theko.sound.SharedFunctions;

public class SoundPlayTest {
    public static void main(String[] args) {
        SoundPlayer sound = null;
        try {
            sound = new SoundPlayer();
            sound.open(SharedFunctions.chooseAudioFile(), SoundPlayer.BUFFER_SIZE_1024MS);
    
            sound.start();
            Thread.sleep(sound.getMicrosecondLength() / 1000);
    
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (sound != null) {
                sound.close();
            }
        }
    }
}
