import java.awt.FileDialog;
import java.awt.Frame;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.theko.sound.*;
import org.theko.sound.codec.*;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.javasound.JavaSoundOutput;

public class PlaybackTest {
    public static void main(String[] args) throws
            AudioCodecNotFoundException,
            FileNotFoundException,
            AudioCodecException,
            AudioDeviceException,
            AudioPortsNotFoundException,
            UnsupportedAudioFormatException,
            InterruptedException {

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
/*
        // TODO: Change to auto selection
        // Init default audio device
        JavaSoundOutput jso = new JavaSoundOutput();

        // Create output line
        try (AudioOutputLine aol = new AudioOutputLine(jso)) {
            // Simulating hard work.
            // Bufferize the audio data
            byte[][] buffers = AudioBufferizer.bufferize(data, format, format.getByteRate()); // ~ 1 second

            // open aol
            aol.open(new ArrayList<>(jso.getAllPorts()).get(0), format);
            // start playback
            aol.start();

            // creating lines for mixer
            DataLine in = new DataLine(format);
            DataLine out = new DataLine(format);

            // creating mixer
            AudioMixer mixer = new AudioMixer(AudioMixer.Mode.EVENT);
            mixer.addInput(in);
            mixer.addOutput(out);
            
            //mixer.addEffect(new ReverbEffect(format, 1090, 0.1f));

            //mixer.getPostGain().setValue(2f);

            // set out from mixer as input to aol
            aol.setInput(out);
            int i = 0;
            while (!Thread.currentThread().isInterrupted() && i < buffers.length) {
                try {
                    // send data to 'in' line.
                    System.out.print(i + ", ");
                    in.send(buffers[i]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }

            System.out.println("\nPlayback completed.");

            in.close();
            out.close();
            mixer.close();
            aol.close(); 
            jso.close();
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                System.out.println("Thread: " + t.getName() + " (State: " + t.getState() + ")");
            }
            
            System.gc();
        }*/
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