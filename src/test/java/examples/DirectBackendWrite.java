package examples;

import java.io.File;
import java.util.Optional;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioMeasure;
import org.theko.sound.AudioMixer;
import org.theko.sound.AudioPort;
import org.theko.sound.SoundSource;
import org.theko.sound.backends.AudioBackend;
import org.theko.sound.backends.AudioBackendInfo;
import org.theko.sound.backends.AudioBackends;
import org.theko.sound.backends.AudioOutputBackend;
import org.theko.sound.resamplers.LinearResampler;
import org.theko.sound.resamplers.ResamplingProcessor;
import org.theko.sound.samples.SamplesConverter;

import helpers.FileChooserHelper;

public class DirectBackendWrite {

    static class Wrap<T> {
        T x;
        public Wrap(T val) { this.x = val; }
        public T get() { return x; }
        public void set(T val) { this.x = val; }
    }

    public static void main(String[] args) {
        Wrap<AudioOutputBackend> outputWrap = new Wrap<>(null);
        SoundSource source = null;

        try {
            // Get and open the output backend
            AudioBackendInfo backendInfo = AudioBackends.getPlatformBackend();
            AudioBackend backend = AudioBackends.getBackend(backendInfo);
            outputWrap.set(backend.getOutputBackend());

            // Prepare source and mixer
            source = new SoundSource();
            AudioMixer mixer = new AudioMixer();
            mixer.addInput(source);

            File file = FileChooserHelper.chooseAudioFile();
            if (file == null) return;

            source.open(file);
            System.out.println("Opened file: " + file.getAbsolutePath());

            AudioFormat sourceFormat = source.getAudioFormat();
            System.out.println("Sound source format: " + sourceFormat);

            // Open the output port
            AudioMeasure bufferSize = AudioMeasure.ofFrames(1024).onFormat(sourceFormat);
            Optional<AudioPort> outPort = outputWrap.get().getDefaultPort(AudioFlow.OUT);

            if (outPort.isEmpty()) {
                System.err.println("Output port not found.");
                return;
            }

            AudioFormat outputFormat = outputWrap.get().open(outPort.get(), sourceFormat, (int) bufferSize.getBytes());
            System.out.println("Opened audio format: " + outputFormat);

            double resamplingFactor = (double) sourceFormat.getSampleRate() / outputFormat.getSampleRate();
            System.out.println("Resampling factor: " + resamplingFactor);

            // Launch the playback
            outputWrap.get().start();
            source.start();

            // Start playback thread
            Wrap<Boolean> isPlaying = new Wrap<Boolean>(true);
            Thread playbackThread = new Thread(() -> {
                float[][] buffer = new float[outputFormat.getChannels()][(int) bufferSize.getFrames()];
                ResamplingProcessor resampler = new ResamplingProcessor(new LinearResampler());

                try {
                    while (!Thread.currentThread().isInterrupted() && outputWrap.get().isOpen()
                            && isPlaying.get()) {
                        // Use source sample rate, because we are doing resampling
                        mixer.render(buffer, sourceFormat.getSampleRate());

                        float[][] resampled = resampler.resample(buffer, (float) resamplingFactor);
                        byte[] data = SamplesConverter.toBytes(resampled, outputFormat);

                        outputWrap.get().write(data, 0, data.length);
                    }
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        e.printStackTrace();
                    }
                }
            }, "Playback-Thread");
            playbackThread.start();

            System.out.println("Playback started. Press Enter to stop...");
            System.in.read();

            isPlaying.set(false);

            playbackThread.interrupt();
            playbackThread.join(2000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (source != null) source.close();
            if (outputWrap != null) outputWrap.get().close();
        }
    }
}