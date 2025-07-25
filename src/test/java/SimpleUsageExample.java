import java.io.FileNotFoundException;

import org.theko.sound.AudioMixer;
import org.theko.sound.AudioMixerOutput;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.codec.AudioTag;
import org.theko.sound.effects.BitcrusherEffect;

public class SimpleUsageExample {
    public static void main(String[] args) {
        AudioMixerOutput out = null;

        try {
            // Try to initialize the audio output backend
            try {
                out = new AudioMixerOutput();
            } catch (AudioBackendCreationException | AudioBackendNotFoundException e) {
                System.out.println("Audio backend creation failed.");
                return;
            }

            // Create an audio mixer and assign it to output
            AudioMixer outMixer = new AudioMixer();
            out.setMixer(outMixer);

            // Create a sound source (will be opened later)
            SoundSource sound = new SoundSource();

            // Open an audio file using a file chooser
            try {
                sound.open(SharedFunctions.chooseAudioFile());
            } catch (FileNotFoundException | AudioCodecNotFoundException e) {
                System.out.println("File or Audio codec not found.");
                return;
            }

            // Set playback speed (can be done only after opening the file)
            sound.getSpeedControl().setValue(1.1f);

            // Create and configure a bitcrusher effect
            BitcrusherEffect bitcrusher = new BitcrusherEffect();
            bitcrusher.getBitdepth().setValue(6); // 6-bit resolution
            bitcrusher.getSampleRateReduction().setValue(4000); // Sample rate down to 4 kHz

            // Add the bitcrusher effect to the output mixer
            outMixer.addEffect(bitcrusher);

            // Open the audio output with the same format as the source
            try {
                out.open(sound.getAudioFormat());
            } catch (AudioPortsNotFoundException | UnsupportedAudioFormatException e) {
                System.out.println("Audio ports not found or unsupported audio format.");
                return;
            }

            // Connect the sound source to the mixer
            outMixer.addInput(sound);

            // Start playback
            sound.start();

            // Launch a thread to dynamically change bitcrusher mix level (like an LFO)
            Thread changingThread = new Thread(() -> {
                while (sound.isPlaying()) {
                    bitcrusher.getMixLevelControl().setValue(
                        0.5f + (float) Math.sin(System.nanoTime() / 1_000_000_000.0) * 0.5f
                    );
                }
            });
            changingThread.setDaemon(true); // Mark as daemon so it won't prevent exit
            changingThread.start();

            // Retrieve metadata and duration
            double length = sound.getDuration();
            String title = AudioTag.getValue(sound.getTags(), AudioTag.TITLE);
            String artist = AudioTag.getValue(sound.getTags(), AudioTag.ARTIST);

            System.out.println("Playing: " + title + " - " + artist);
            System.out.println();

            // Display playback status in console (updated every 500ms)
            while (sound.isPlaying()) {
                double seconds = sound.getSecondsPosition();

                System.out.print(
                    String.format(
                        "\r %02d:%02d:%02d / %02d:%02d:%02d [%.2f x] [Bitcrusher Mix: %.2f] | %s - %s",
                        (int) seconds / 3600,
                        (int) (seconds / 60) % 60,
                        (int) seconds % 60,
                        (int) length / 3600,
                        (int) (length / 60) % 60,
                        (int) length % 60,
                        sound.getSpeedControl().getValue(),
                        bitcrusher.getMixLevelControl().getValue(),
                        title,
                        artist
                    )
                );
                Thread.sleep(500);
            }

            // Stop and clean up after playback
            sound.stop();
            sound.close();
            out.close();

        } catch (Exception e) {
            // Handle any unexpected error
            System.out.println("Unexpected exception: " + e.getMessage());
            if (out != null) out.close(); // Ensure the audio system is shut down
        }
    }
}
