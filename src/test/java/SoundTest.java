// package test;

// import some file, frames staff.
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;

import org.theko.sound.PlaybackException;
import org.theko.sound.SoundPlayer;
import org.theko.sound.SoundSource;

public class SoundTest {
    public static void main(String[] args) {
        try (SoundPlayer sound = new SoundPlayer()) {
            String file = chooseFile();
            
            // open audio file, with 64 ms buffer size
            sound.open(new File(file), SoundSource.BUFFER_SIZE_64MS);

            // change playback speed
            // sound.getSpeedController().setValue(1.0f);

            // Add effect
            //ReverbEffect reverb = new ReverbEffect(sound.getAudioFormat());
            //sound.addEffect(reverb);
            sound.start(); // start the playback

            // create new thread, to show playaback position in seconds
            new Thread(() -> {
                while (sound.isPlaying()) {
                    // sound.getMicrosecondPosition() returns the position without the speed change.
                    // sound.getModifiedMicrosecondPosition() returns the position with the speed change.
                    System.out.print(String.format("\r%.2f s / %.2f s",
                            sound.getMicrosecondPosition() / 1_000_000d,
                            sound.getMicrosecondLength() / 1_000_000d));
                try {
                    Thread.sleep(50); // wait some time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }}
            }).start();

            Thread.sleep(10000);
            for (float f = 1.0f; f > 0; f -= 0.005) {
                sound.getSpeedController().setValue(f);
                Thread.sleep(500);
            }

            sound.stop(); // stop the playback
            // no need to close due to try-with-resources and AutoCloseable
            // sound.close(); // close the sound player
        } catch (PlaybackException | InterruptedException | FileNotFoundException /*| UnsupportedAudioEffectException*/ e) {
            e.printStackTrace();
        }
    }

    // Helper method, to choose audio file
    private static String chooseFile() {
        FileDialog dialog = new FileDialog((Frame) null, "Select an audio file", FileDialog.LOAD);
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return null; // User closed dialog
        }

        String path = dialog.getDirectory() + dialog.getFile();
        dialog.dispose();
        return path;
    }
}
