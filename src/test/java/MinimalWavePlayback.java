import java.io.File;
import java.io.FileNotFoundException;

import org.theko.sound.SoundPlayer;
import org.theko.sound.codec.AudioCodecNotFoundException;

import helpers.FileChooserHelper;

/*
 * Simple example: plays a WAV file using SoundPlayer.
 * Use Ctrl+C to stop playback.
 */
public class MinimalWavePlayback {

    public static void main(String[] args) {
        try (SoundPlayer player = new SoundPlayer()) {
            File audioFile = FileChooserHelper.chooseAudioFile();
            if (audioFile == null) {
                System.out.println("No audio file selected.");
                return;
            }
            player.open(audioFile);
            player.startAndWait(); // Start playback and wait for it to finish
        } catch (AudioCodecNotFoundException e) {
            // AudioCodecNotFoundException used to handle an unsupported audio extensions,
            // such as mp3 or flac
            System.err.println("Provided audio file is unsupported.");
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
        } catch (InterruptedException e) {
            // When the playback is interrupted
            Thread.currentThread().interrupt();
            System.err.println("Playback interrupted.");
        }
    }
}