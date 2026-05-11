package examples;

import java.io.FileNotFoundException;
import java.util.Arrays;

import org.theko.sound.AudioClassRegister;
import org.theko.sound.AudioMixer;
import org.theko.sound.AudioOutputLayer;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.LFO;
import org.theko.sound.SoundSource;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.Waveform;
import org.theko.sound.backends.AudioBackendCreationException;
import org.theko.sound.backends.AudioBackends;
import org.theko.sound.codecs.AudioCodecNotFoundException;
import org.theko.sound.codecs.AudioCodecs;
import org.theko.sound.codecs.AudioTag;
import org.theko.sound.effects.BitcrusherEffect;
import org.theko.sound.events.OutputLayerEventType;
import org.theko.sound.properties.AudioSystemProperties;

import helpers.FileChooserHelper;

public class UsageExample {
    public static void main(String[] args) {
        // Force static initialization
        AudioSystemProperties.runStaticInit();
        AudioClassRegister.registerClasses();

        // Print all available audio backends and codecs
        System.out.println("Available audio backends: ");
        System.out.println(
            AudioBackends.getAllBackends().stream()
                .map(x -> x.toString())
                .reduce((a, b) -> a + "\n" + b)
                .orElse(""));
        System.out.println("Available audio codecs: ");
        System.out.println(
            AudioCodecs.getAllCodecs().stream()
                .map(x -> x.toString())
                .reduce((a, b) -> a + "\n" + b)
                .orElse(""));

        // Create an audio output layer and a sound source
        try (AudioOutputLayer aol = new AudioOutputLayer(); SoundSource sound = new SoundSource()) {

            AudioMixer out = new AudioMixer(); // create master mixer
            aol.setRootNode(out); // set master mixer as output node

            // Open an audio file using a file chooser
            // If file is unsupported or not found, an exception will be thrown
            // Note: after FileChooser call, AWT thread will not stop.
            sound.open(FileChooserHelper.chooseAudioFile());
            out.addInput(sound);

            // Open the audio output with the same format as the source
            // Uses default output device and buffer size
            // Can throw UnsupportedAudioFormatException, AudioPortsNotFoundException
            aol.open(sound.getAudioFormat());

            // Get metadata and duration
            double length = sound.getDuration();
            String title = sound.getMetadata().getValue(AudioTag.TITLE);
            String artist = sound.getMetadata().getValue(AudioTag.ARTIST);
            if (title == null || title.isEmpty()) {
                title = "Unknown Title";
            }
            if (artist == null || artist.isEmpty()) {
                artist = "Unknown Artist";
            }

            // Change the speed of playback
            sound.getSpeedControl().setValue(3.9f);

            // Create bitcrusher effect
            BitcrusherEffect bitcrusher = new BitcrusherEffect();
            bitcrusher.getBitdepth().setValue(6); // 6-bit resolution
            bitcrusher.getSampleRateReduction().setValue(4000); // Sample rate down to 4 kHz

            // Add bitcrusher to output
            // Can throw IncompatibleEffectTypeException, MultipleVaryingSizeEffectsException
            out.addEffect(bitcrusher);

            // Create an LFO to modulate the bitcrusher's mix level
            LFO lfo = new LFO(Arrays.asList(bitcrusher.getMixLevelControl()));
            lfo.setWaveformType(Waveform.TRIANGLE);
            lfo.getSpeed().setValue(0.2f);
            lfo.start();

            // Add stop event listener to stop LFO thread
            // Stopped event is triggered when AOL is stopped or closed
            aol.addConsumer(OutputLayerEventType.STOPPED, (event, type) -> {
                System.out.println("Playback stopped.");
                sound.stop();
                lfo.stop();
            });

            sound.start();
            aol.start(); // start audio output

            // Display playback information
            System.out.println("Playing: " + title + " - " + artist);

            // Display playback status in console (updated every 500ms)
            while (sound.isPlaying()) {
                double seconds = sound.getSecondsPosition();

                System.out.print(
                    String.format(
                        "\r%02d:%02d:%02d / %02d:%02d:%02d [%.2f x] [Bitcrusher Mix: %.2f] | %s - %s",
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
            System.out.println();

            // Stop and clean up after playback
            aol.stop(); // Stop playback thread, allowing to exit
            System.exit(0); // Exit application
        } catch (AudioBackendCreationException  e) {
            System.err.println("Audio backend creation failed.");
        } catch (FileNotFoundException | AudioCodecNotFoundException e) {
            System.err.println("File or audio codec not found.");
        } catch (UnsupportedAudioFormatException | AudioPortsNotFoundException e) {
            System.err.println("Unsupported audio format or output device not found, while opening audio file.");
        } catch (InterruptedException e) {
            // When the playback is interrupted
            Thread.currentThread().interrupt();
            System.err.println("Interrupted.");
        }
    }
}