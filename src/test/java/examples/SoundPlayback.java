package examples;

import java.io.File;
import java.io.FileNotFoundException;

import org.theko.sound.SoundPlayer;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.codec.AudioTag;

import helpers.FileChooserHelper;

public class SoundPlayback {
    public static void main(String[] args) {
        System.setProperty("org.theko.sound.effects.resampler", "CubicResampleMethod:5");
        System.out.println("Simple Sound Playback Example");
        System.out.println("Use Ctrl+C to stop playback.");
        SoundPlayer player = new SoundPlayer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (player != null && player.isOpen()) {
                player.close();
            }
        }));
        try {
            File audioFile = FileChooserHelper.chooseAudioFile();
            if (audioFile == null) {
                System.out.println("No audio file selected.");
                return;
            }
            player.open(audioFile);
            player.start();

            String info = getTrackInfo(player);
            while (player.isPlaying()) {
                System.out.print("\rPlaying: " + info + " | " + getPlaybackInfo(player));
                Thread.sleep(500);
            }
            System.out.println();
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
        } catch (AudioCodecNotFoundException e) {
            // AudioCodecNotFoundException used to handle an unsupported audio extensions
            System.err.println("Provided audio file is unsupported.");
        } catch (InterruptedException e) {
            // When the playback is interrupted
            Thread.currentThread().interrupt();
            System.err.println("Playback interrupted.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (player != null && player.isOpen()) {
                player.close();
            }
        }
    }

    private static String getTrackInfo(SoundPlayer player) {
        String title = player.getTags().getValue(AudioTag.TITLE);
        String artist = player.getTags().getValue(AudioTag.ARTIST);
        return "%s - %s".formatted(
            artist != null ? artist : "Unknown Artist",
            title != null ? title : "Unknown Title");
    }

    private static String getPlaybackInfo(SoundPlayer player) {
        int posSec = (int) player.getSecondsPosition();
        int durSec = (int) player.getDuration();

        int posMin = posSec / 60;
        int posRemSec = posSec % 60;

        int durMin = durSec / 60;
        int durRemSec = durSec % 60;

        float speed = player.getSpeedControl().getValue();

        return "%02d:%02d / %02d:%02d%s".formatted(posMin, posRemSec, durMin, durRemSec,
            speed != 1f ? " [x%.2f]".formatted(speed) : ""
        );
    }
}
