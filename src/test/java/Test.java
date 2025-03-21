import java.awt.FileDialog;
import java.awt.Frame;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.theko.sound.*;
import org.theko.sound.codec.*;
import org.theko.sound.codec.formats.ZWAVCodec;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.javasound.JavaSoundOutput;
import org.theko.sound.effects.InvertEffect;
import org.theko.sound.effects.OscilloscopeVisualizerEffect;
import org.theko.sound.effects.ReverbEffect;

public class Test {
    public static void main(String[] args) throws Exception {

        // Select file
        String filepath = chooseFile();
        if (filepath == null) {
            System.err.println("File not selected. Exiting.");
            return;
        }

        // Decoding file
        AudioDecodeResult r = AudioCodecs.fromName("WAVE").getCodecClass().newInstance().decode(new FileInputStream(filepath));
        // print audio file info
        System.out.println(r);

        byte[] data = r.getBytes();
        AudioFormat format = r.getAudioFormat();

        float[][] s = SampleConverter.toSamples(data, format);
        AudioFormat target = new AudioFormat(format.getSampleRate(), 8, format.getChannels(), AudioFormat.Encoding.ULAW, false);
        data = SampleConverter.fromSamples(s, target);
        s = SampleConverter.toSamples(data, target);
        data = SampleConverter.fromSamples(s, format);

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
            AudioMixer mixer = new AudioMixer(AudioMixer.Mode.EVENT);
            mixer.addInput(in);
            mixer.addOutput(out);
            
            ///mixer.addEffect(new ReverbEffect(format));
            //OscilloscopeVisualizerEffect effect = new OscilloscopeVisualizerEffect(format);
            //effect.setTransitionDuration(100);
            //effect.setWaveWidth(16);

            //mixer.getPostGain().setValue(2f);

            // set out from mixer as input to aol
            aol.setInput(out);
            int i = 0;
            Holder<Integer> h = new Holder<>(0);

            /*new Thread(() -> {
                while (h.value < buffers.length) {
                    mixer.getPreGain().setValue((float)Math.abs(Math.sin(System.currentTimeMillis()/1000.)));
                }
            }).start();*/

            mixer.getPreGain().setValue(0.001f);

            //in.send(data);
            while (!Thread.currentThread().isInterrupted() && i < buffers.length) {
                try {
                    // send data to 'in' line.
                    System.out.print(i + ", ");
                    in.send(buffers[i]);
                    h.value = i;
                    //effect.process(buffers[Math.max(0, i)]);
                    //System.out.println("UD");
                    //Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }

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
    
    public static class Holder<T> {
        private T value; // хранимый объект
    
        public Holder(T value) {
            this.value = value;
        }
    
        public T get() {
            return value;
        }
    
        public void set(T value) {
            this.value = value;
        }
    }
    
}