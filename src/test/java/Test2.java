import java.awt.FileDialog;
import java.awt.Frame;
import java.io.FileInputStream;

import org.theko.sound.*;
import org.theko.sound.AudioFormat.Encoding;
import org.theko.sound.codec.*;

import static org.theko.sound.SampleConverter.*;

public class Test2 {
    public static void main(String[] args) throws Exception {

        // Select file
        String filepath = chooseFile();
        if (filepath == null) {
            System.err.println("File not selected. Exiting.");
            return;
        }

        // Decoding file
        AudioDecodeResult r = AudioCodecs.load("WAVE").decode(new FileInputStream(filepath));
        // print audio file info
        System.out.println(r);

        byte[] data = r.getBytes();
        AudioFormat format = r.getAudioFormat();

        try (AudioOutputLine aol = new AudioOutputLine()) {
            aol.open(format);
            aol.start();
            
            DataLine out = new DataLine(format);

            aol.setInput(out);

            float[][] amp = toSamples(data, format, 0.5f);
            //System.out.println(amp[2000]);
            /*for (int i = 0; i < amp.length; i++) {
                amp[i] *= 0.5f;
            }*/
            data = fromSamples(amp, format);

            out.send(data);

            System.out.println("\nPlayback completed.");
        }
    }

    private static String chooseFile() {
        FileDialog dialog = new FileDialog((Frame) null, "Select an audio file", FileDialog.LOAD);
        dialog.setFile("*.wav"); // Extension filter.
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return null; // User closed dialog
        }

        String path = dialog.getDirectory() + dialog.getFile();
        dialog.dispose();
        return path;
    }
}