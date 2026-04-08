package examples;
import java.io.FileNotFoundException;
import java.util.Arrays;

import org.theko.sound.AudioMixer;
import org.theko.sound.AudioOutputLayer;
import org.theko.sound.LFO;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.WaveformType;
import org.theko.sound.backends.AudioBackendCreationException;
import org.theko.sound.backends.AudioBackendNotFoundException;
import org.theko.sound.codecs.AudioCodecNotFoundException;
import org.theko.sound.codecs.AudioTag;
import org.theko.sound.effects.BitcrusherEffect;
import org.theko.sound.events.OutputLayerEventType;

import helpers.FileChooserHelper;

public class UsageExample {
    public static void main(String[] args) {
        AudioOutputLayer out = null;

        try {
            // Try to initialize the audio output backend
            try {
                out = new AudioOutputLayer();
            } catch (AudioBackendCreationException | AudioBackendNotFoundException e) {
                System.err.println("Audio backend creation failed.");
                return;
            }

            // Create an audio mixer and assign it to output
            AudioMixer outMixer = new AudioMixer();
            out.setRootNode(outMixer);

            // Create a sound source (will be opened later)
            SoundSource sound = new SoundSource();

            // Open an audio file using a file chooser
            try {
                sound.open(FileChooserHelper.chooseAudioFile());
            } catch (FileNotFoundException | AudioCodecNotFoundException e) {
                System.err.println("File or Audio codec not found.");
                return;
            }

            // Set playback speed (can be done only after opening the file)
            sound.getSpeedControl().setValue(0.9f);

            // Create and configure a bitcrusher effect
            BitcrusherEffect bitcrusher = new BitcrusherEffect();
            bitcrusher.getBitdepth().setValue(6); // 6-bit resolution
            bitcrusher.getSampleRateReduction().setValue(4000); // Sample rate down to 4 kHz

            // Add the bitcrusher effect to the output mixer
            outMixer.addEffect(bitcrusher);

            // Open the audio output with the same format as the source
            try {
                out.open(sound.getAudioFormat());
            } catch (UnsupportedAudioFormatException e) {
                System.err.println("Unsupported audio format.");
                return;
            }

            // Connect the sound source to the mixer
            outMixer.addInput(sound);

            // Start playback
            sound.start();
            out.start();

            // Create an LFO to modulate the bitcrusher's mix level
            LFO lfo = new LFO(Arrays.asList(bitcrusher.getMixLevelControl()));
            lfo.getWaveformType().setEnumValue(WaveformType.TRIANGLE);
            lfo.getSpeed().setValue(0.05f);
            lfo.start();

            out.addConsumer(OutputLayerEventType.STOPPED, (event, type) -> {
                System.out.println("\nPlayback stopped.");
                sound.stop();
                lfo.stop();
            });

            // Retrieve metadata and duration
            double length = sound.getDuration();
            String title = sound.getMetadata().getValue(AudioTag.TITLE);
            String artist = sound.getMetadata().getValue(AudioTag.ARTIST);

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
                Thread.sleep(100);
            }

            // Stop and clean up after playback
            sound.stop();
            sound.close();
            out.close();

        } catch (Exception e) {
            // Handle any unexpected exceptions that may occur during the process
            System.err.println("Exception: " + e.getMessage());
            if (out != null) {
                out.close(); // Ensure the audio system is shut down
            }
        }
    }
}
