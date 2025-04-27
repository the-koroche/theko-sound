package test.org.theko.sound;

import org.theko.sound.AudioConverter;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioFormat.Encoding;
import org.theko.sound.codec.AudioDecodeResult;

public class AudioConverterTest1 {
    public static void main(String[] args) {
        AudioDecodeResult adr = SharedFunctions.decodeAudioFile(SharedFunctions.chooseAudioFile());
        AudioFormat format = adr.getAudioFormat();
        byte[] data = adr.getBytes();
        // cut the data to 10 seconds
        int cut = format.getByteRate() * 10; // 10 seconds
        if (cut < data.length) {
            byte[] cutData = new byte[cut];
            System.arraycopy(data, 0, cutData, 0, cut);
            data = cutData;
        }

        AudioFormat newFormat = new AudioFormat(
            44100,
            16,
            format.getChannels(),
            Encoding.PCM_SIGNED,
            false // little endian
        );

        System.out.println("Original format: " + format);
        System.out.println("New format: " + newFormat);

        byte[] converted = AudioConverter.convert(data, format, newFormat);

        SharedFunctions.playAudioData(converted, newFormat); // play converted
        SharedFunctions.playAudioData(data, format); // play original
    }
}
