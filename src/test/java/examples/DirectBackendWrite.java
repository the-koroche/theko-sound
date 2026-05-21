package examples;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    public static void main(String[] args) {
        AtomicReference<AudioOutputBackend> output = new AtomicReference<>();
        AtomicReference<SoundSource> source = new AtomicReference<>();

        try {
            // Get and open the output backend
            AudioBackendInfo backendInfo = AudioBackends.getPlatformBackend();
            AudioBackend backend = AudioBackends.getBackend(backendInfo);
            output.set(backend.getOutputBackend());

            // Prepare source and mixer
            source.set(new SoundSource());
            AudioMixer mixer = new AudioMixer();
            mixer.addInput(source.get());

            File file = FileChooserHelper.chooseAudioFile();
            if (file == null) return;

            source.get().open(file);
            System.out.println("Opened file: " + file.getAbsolutePath());

            AudioFormat sourceFormat = source.get().getAudioFormat();
            System.out.println("Sound source format: " + sourceFormat);

            // Open the output port
            AudioMeasure bufferSize = AudioMeasure.ofFrames(1024).onFormat(sourceFormat);
            Optional<AudioPort> outPort = output.get().getDefaultPort(AudioFlow.OUT);

            if (outPort.isEmpty()) {
                System.err.println("Output port not found.");
                return;
            }

            AudioFormat outputFormat = output.get().open(outPort.get(), sourceFormat, (int) bufferSize.getBytes());
            System.out.println("Opened audio format: " + outputFormat);

            double resamplingFactor = (double) sourceFormat.getSampleRate() / outputFormat.getSampleRate();
            System.out.println("Resampling factor: " + resamplingFactor);

            // Launch the playback
            output.get().start();
            source.get().start();

            // Start playback thread
            AtomicBoolean isPlaying = new AtomicBoolean(true);
            Thread playbackThread = new Thread(() -> {
                float[][] buffer = new float[outputFormat.getChannels()][(int) bufferSize.getFrames()];
                ResamplingProcessor resampler = new ResamplingProcessor(new LinearResampler());
                int written = 0;

                try {
                    while (!Thread.currentThread().isInterrupted() && output.get().isOpen()
                            && isPlaying.get()) {
                        // Use source sample rate, because we are doing resampling
                        mixer.render(buffer, sourceFormat.getSampleRate());

                        float[][] resampled = resampler.resample(buffer, (float) resamplingFactor);
                        byte[] data = SamplesConverter.toBytes(resampled, outputFormat);

                        written += output.get().write(data, 0, data.length);
                        System.out.print("\rWrote " + written + " bytes | " + getPlaybackInfo(source.get()));
                    }
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        e.printStackTrace();
                    }
                }
            }, "Playback-Thread");

            // print before "\rWrote ..." to avoid clearing this line
            System.out.println("Playback started. Press Enter to stop...");

            playbackThread.start();
            System.in.read();

            isPlaying.set(false);

            playbackThread.interrupt();
            playbackThread.join(2000);

            int clearLineWidth = 20;
            String clearLine = " ".repeat(clearLineWidth);
            System.out.println("\rPlayback stopped." + clearLine);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (source != null) source.get().close();
            if (output != null) output.get().close();
        }
    }

    private static String getPlaybackInfo(SoundSource sound) {
        int posSec = (int) sound.getSecondsPosition();
        int durSec = (int) sound.getDuration();

        int posMin = posSec / 60;
        int posRemSec = posSec % 60;

        int durMin = durSec / 60;
        int durRemSec = durSec % 60;

        return "%02d:%02d / %02d:%02d".formatted(
            posMin, posRemSec, durMin, durRemSec);
    }
}