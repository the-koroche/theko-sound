import java.awt.FileDialog;
import java.awt.Frame;
import java.io.FileInputStream;

import org.theko.sound.*;
import org.theko.sound.codec.*;
import org.theko.sound.effects.SpeedChangeEffect;
public class Test {
    public static void main(String[] args) throws Exception {

        // Select file
        String filepath = chooseFile();
        if (filepath == null) {
            System.err.println("File not selected. Exiting.");
            return;
        }

        // Decoding file
        AudioCodec codec = AudioCodecs.getCodec(AudioCodecs.fromExtension(getFileExtension(filepath)));
        AudioDecodeResult r = codec.decode(new FileInputStream(filepath));
        // print audio file info
        System.out.println(r.getInfo());

        byte[] data = r.getBytes();
        AudioFormat format = r.getAudioFormat();

              // Create output line
        try (AudioOutputLine aol = new AudioOutputLine()) {
            // Simulating hard work.
            // Bufferize the audio data
            byte[][] buffers = AudioBufferizer.bufferize(data, format, format.getByteRate()); // ~ 0.02 second

            // open aol
            aol.open(format);
            // start playback
            aol.start();

            // creating lines for mixer
            DataLine in = new DataLine(format);
            DataLine out = new DataLine(format);

            // creating mixer
            AudioMixer mixer = new AudioMixer(AudioMixer.Mode.EVENT, format);
            mixer.addInput(in);
            mixer.addOutput(out);

            SpeedChangeEffect spe = new SpeedChangeEffect(format);
            spe.getSpeedController().setValue(0.9f);
            mixer.addEffect(spe);
            
            ///mixer.addEffect(new ReverbEffect(format));
            //OscilloscopeVisualizerEffect effect = new OscilloscopeVisualizerEffect(format);
            //effect.setTransitionDuration(100);
            //effect.setWaveWidth(16);

            //mixer.getPostGain().setValue(2f);

            // set out from mixer as input to aol
            aol.setInput(out);
            int i = 0;

            /*new Thread(() -> {
                while (h.value < buffers.length) {
                    mixer.getPreGain().setValue((float)Math.abs(Math.sin(System.currentTimeMillis()/1000.)));
                }
            }).start();*/

            mixer.getPreGainController().setValue(1f);

            //in.send(data);
            while (!Thread.currentThread().isInterrupted() && i < buffers.length) {
                try {
                    // send data to 'in' line.
                    System.out.print(i + ", ");
                    in.send(buffers[i]);
                    //effect.process(buffers[Math.max(0, i)]);
                    //System.out.println("UD");
                    //Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }

            System.out.println("\nPlayback completed.");
            mixer.close();
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
    
    private static String getFileExtension(String filepath) {
        int index = filepath.lastIndexOf(".");
        if (index > 0) {
            return filepath.substring(index + 1);
        }
        return ""; // Return empty string if no extension
    }
}