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
import org.theko.sound.events.SoundSourceEventType;
import org.theko.sound.resamplers.LinearResampler;
import org.theko.sound.resamplers.ResamplingProcessor;
import org.theko.sound.samples.SamplesConverter;

import helpers.FileChooserHelper;

public class DirectBackendWrite {

    private static final String clearLine = "\r\u001B[2K";

    public static void main(String[] args) {
        AtomicReference<AudioOutputBackend> output = new AtomicReference<>();
        AtomicReference<SoundSource> source = new AtomicReference<>();

        try {
            // Get and open the output backend
                // Get the default platform-specific audio backend    
                AudioBackendInfo backendInfo = AudioBackends.getPlatformBackend();
                // Create instance of the backend
                AudioBackend backend = AudioBackends.getBackend(backendInfo);
                // Create and assign the output backend
                output.set(backend.getOutputBackend());

            // Prepare source and mixer
                // Create empty sound source
                source.set(new SoundSource());
                // Create mixer, add source
                AudioMixer mixer = new AudioMixer();
                mixer.addInput(source.get());

            // Select audio file
            File file = FileChooserHelper.chooseAudioFile();
            if (file == null) return;

            // Open the sound source with the selected file
            source.get().open(file);
            System.out.printf("Opened file: %s\n", file.getAbsolutePath());

            // Get the audio format of selected file
            AudioFormat sourceFormat = source.get().getAudioFormat();
            System.out.printf("Source audio format: %s\n", sourceFormat);

            // Open the output port
                // Calculate the default output buffer size
                AudioMeasure bufferSize = AudioMeasure.ofFrames(1024).onFormat(sourceFormat);
                // Get default output port
                Optional<AudioPort> outPort = output.get().getDefaultPort(AudioFlow.OUT);

                // If default output port is not found,
                // use the first available output port
                if (outPort.isEmpty())
                    outPort = output.get().getAvailablePorts(AudioFlow.OUT).stream().findFirst();

                // If still nothing is found, exit
                if (outPort.isEmpty()) {
                    System.err.println("No output ports available.");
                    return;
                }

            // Open the output port
                int bufferSizeBytes = (int) bufferSize.getBytes();
                // Get opened audio format, and it may be different from the source format
                AudioFormat outputFormat = output.get().open(outPort.get(), sourceFormat, bufferSizeBytes);
                System.out.printf("Opened audio format: %s\n", outputFormat);

                // Calculate resampling factor
                double resamplingFactor = (double) sourceFormat.getSampleRate() / outputFormat.getSampleRate();
                System.out.printf("Resampling factor: %.2f\n", resamplingFactor);

                // Check if resampling factor is valid
                if (!Double.isFinite(resamplingFactor)) {
                    System.err.println("Resampling factor is not finite.");
                    return;
                }

                // Calculate buffers
                // Render buffer used by the mixer to render samples
                int renderBufferSize = (int) bufferSize.getFrames();
                // Resampled buffer size is target output length
                int resampledBufferSize = (int) (renderBufferSize / resamplingFactor);
                // Output byte buffer length for direct write
                int outputByteBufferSize = resampledBufferSize * outputFormat.getFrameSize();

                System.out.printf("Render buffer size: %d\n", renderBufferSize);
                System.out.printf("Resampled length: %d\n", resampledBufferSize);
                System.out.printf("Output byte buffer length: %d\n", outputByteBufferSize);

            // Start playback thread
            AtomicBoolean isPlaying = new AtomicBoolean(true);
            Thread playbackThread = new Thread(() -> {

                float[][] renderBuffer = new float[outputFormat.getChannels()][renderBufferSize];
                float[][] resampleOutputBuffer = new float[outputFormat.getChannels()][resampledBufferSize];
                byte[] outputBytes = new byte[outputByteBufferSize];

                ResamplingProcessor resampler = new ResamplingProcessor(new LinearResampler());
                int written = 0;

                try {
                    while (!Thread.interrupted() && isPlaying.get()) {
                        // Use source sample rate, because we are doing resampling later
                        mixer.render(renderBuffer, sourceFormat.getSampleRate());

                        // Resample the samples, if resampling factor is not 1
                        if (resamplingFactor != 1)
                            resampler.resample(renderBuffer, resampleOutputBuffer, resamplingFactor);
                        else
                            // Just copy if resampling factor is 1
                            resampleOutputBuffer = renderBuffer;

                        // Convert float[][] PCM  to byte[] PCM
                        SamplesConverter.toBytes(resampleOutputBuffer, outputBytes, outputFormat);

                        // Write data to output
                        written += output.get().write(outputBytes, 0, outputBytes.length);
                        System.out.printf("%sWrote %d bytes | %s",
                                clearLine, written, getPlaybackInfo(source.get()));
                    }
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        // If not an interruption, print stack trace
                        e.printStackTrace();
                    } else {
                        // Else, if an interruption, interrupt this thread
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Playback-Thread");

            // Subscribe to stop event
            // Remove it if you want playback to continue
            source.get().addConsumer(SoundSourceEventType.DATA_ENDED, (type, event) -> {
                isPlaying.set(false);
                playbackThread.interrupt();
            });

            // Launch the playback
            output.get().start();
            source.get().start();

            // Print before "\rWrote ..." to avoid clearing this line
            System.out.println("Playback started. Press Enter to stop...");

            // Start playback
            playbackThread.start();
            // Wait for user input
            System.in.read();

            // Interrupt playback
            isPlaying.set(false);
            playbackThread.interrupt();
            playbackThread.join(2000);

            System.out.printf("%sPlayback stopped.", clearLine);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the source and output
            if (source != null) source.get().close();
            if (output != null) output.get().close();
        }
    }

    // Returns playback info, like "01:23 / 02:57"
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