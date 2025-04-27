package test.org.theko.sound;

import org.theko.sound.AudioBufferizer;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioOutputLine;

import org.theko.sound.AudioDeviceCreationException;
import org.theko.sound.AudioDeviceNotFoundException;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.codec.AudioDecodeResult;

public class AudioBufferizerTest {
    public static void main(String[] args) {
        AudioDecodeResult adr = SharedFunctions.decodeAudioFile(SharedFunctions.chooseAudioFile());
        AudioFormat format = adr.getAudioFormat();
        byte[] data = adr.getBytes();

        int bufferSize = format.getByteRate(); // 1 second of audio data

        // Bufferize the audio data
        byte[][] buffers = AudioBufferizer.bufferize(data, format, bufferSize);
        // Print the number of buffers created
        System.out.println("Number of buffers created: " + buffers.length);

        // Play the buffers
        try {
            AudioOutputLine aol = new AudioOutputLine();
            aol.open(format);
            aol.start();

            int n = 0;
            while (n < buffers.length) {
                System.out.println("Playing buffer " + (n) + " of " + buffers.length);
                aol.write(buffers[n], 0, buffers[n].length);
                n++;
            }

            aol.stop();
            aol.close();
        } catch (UnsupportedAudioFormatException |
                    AudioDeviceNotFoundException |
                    AudioDeviceCreationException |
                    AudioPortsNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
