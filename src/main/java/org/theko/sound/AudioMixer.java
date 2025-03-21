package org.theko.sound;

import java.lang.ref.Cleaner;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.theko.sound.control.FloatController;
import org.theko.sound.effects.VisualAudioEffect;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;

public class AudioMixer implements AutoCloseable {

    public enum Mode {
        THREAD, EVENT
    }

    protected final Mode mixerMode;
    protected final List<DataLine> inputs;
    protected final List<DataLine> outputs;
    protected final List<AudioEffect> effects;
    private final Thread mixerThread;

    protected AudioFormat audioFormat;
    protected AudioFormat outAudioFormat;

    private final Map<DataLine, DataLineAdapter> inputListeners = new ConcurrentHashMap<>();

    protected final FloatController preGain;
    protected final FloatController postGain;

    private static final Cleaner cleaner = Cleaner.create();

    public AudioMixer(Mode mixerMode) {
        this.mixerMode = mixerMode;
        inputs = new CopyOnWriteArrayList<>();
        outputs = new CopyOnWriteArrayList<>();
        effects = new CopyOnWriteArrayList<>();

        mixerThread = (mixerMode == Mode.THREAD) ? new Thread(this::processLoop, "AudioMixer") : null;
        if (mixerThread != null) mixerThread.start();

        this.preGain = new FloatController("PRE-GAIN", 0.0f, 1.0f, 1.0f);
        this.postGain = new FloatController("POST-GAIN", 0.0f, 1.0f, 1.0f);

        cleaner.register(this, this::shutdown);
    }

    public void addInput(DataLine input) throws UnsupportedAudioFormatException {
        if (audioFormat == null || inputs.isEmpty()) {
            audioFormat = input.getAudioFormat();
        }
        if (!input.getAudioFormat().equals(audioFormat)) {
            throw new UnsupportedAudioFormatException("Audio formats are not equal.");
        }
        inputs.add(input);
        if (mixerMode == Mode.EVENT) {
            DataLineAdapter adapter = new DataLineAdapter() {
                public void onSend(DataLineEvent e) {
                    try {
                        process();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new MixingException(ex);
                    }
                }
            };
            input.addDataLineListener(adapter);
            inputListeners.put(input, adapter);
        }
    }

    public void addOutput(DataLine output) {
        outputs.add(output);
    }

    public void addEffect(AudioEffect effect) throws UnsupportedAudioEffectException {
        if (effect.getType() == AudioEffect.Type.PROCESS) {
            throw new UnsupportedAudioEffectException("Only AudioEffect.Type.REALTIME is supported to use in mixer.");
        }
        effects.add(effect);
    }

    @Deprecated
    public void addEffect(VisualAudioEffect effect) throws UnsupportedAudioEffectException {
        addEffect(effect);
    }

    public void removeInput(DataLine input) {
        inputs.remove(input);
        if (mixerMode == Mode.EVENT) {
            DataLineAdapter adapter = inputListeners.remove(input);
            if (adapter != null) {
                input.removeDataLineListener(adapter);
            }
        }
    }

    public void removeOutput(DataLine output) {
        outputs.remove(output);
    }

    public void removeEffect(AudioEffect effect) {
        effects.remove(effect);
    }

    public FloatController getPreGain() {
        return preGain;
    }

    public FloatController getPostGain() {
        return postGain;
    }

    private void processLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (!inputs.isEmpty()) {
                    process();
                } else {
                    Thread.yield();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void process() throws InterruptedException {
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return;
        }

        byte[][] data = new byte[inputs.size()][]; 
        int maxLength = 0;

        for (int i = 0; i < inputs.size(); i++) {
            DataLine dl = inputs.get(i);
            if (dl != null) {
                data[i] = dl.receiveWithTimeout(500, TimeUnit.MILLISECONDS);
                if (data[i] != null) {
                    maxLength = Math.max(maxLength, data[i].length);
                }
            }
        }

        if (maxLength == 0) return; // Нет данных — выход

        byte[] mixed = new byte[maxLength];

        for (int i = 0; i < maxLength; i++) {
            int sum = 0;
            int count = 0;

            for (int j = 0; j < inputs.size(); j++) {
                if (data[j] != null && i < data[j].length) {
                    sum += data[j][i];
                    count++;
                }
            }

            double avg = ((double) sum / Math.max(1, count));
            mixed[i] = clampToByte(avg);
        }

        //long t1 = System.nanoTime();
        float[][] sample = SampleConverter.toSamples(mixed, audioFormat, 0.5f);//preGain.getValue());
        sample = applyEffects(sample);
        mixed = SampleConverter.fromSamples(sample, audioFormat);//, postGain.getValue());
        //System.out.println("ELAPSED: " + (System.nanoTime() - t1) + " ns.");

        for (int i = 0; i < mixed.length; i++) {
            mixed[i] = clampToByte(mixed[i] * postGain.getValue());
        }

        for (DataLine output : outputs) {
            if (output != null) {
                if (!output.sendWithTimeout(mixed, 200, TimeUnit.MILLISECONDS)) {
                    System.err.println("Warning: Data dropped for output: " + output);
                }
            }
        }
    }

    private static byte clampToByte(double value) {
        return (byte) Math.max(Math.min(Math.round(value), 127), -128);
    }

    private float[][] applyEffects(float[][] sample) {
        // Обрабатываем эффекты по очереди, а не параллельно
        for (AudioEffect effect : effects) {
            if (effect != null) {
                sample = effect.process(sample);
            }
        }
        return sample;
    }

    public void shutdown() {
        if (mixerThread != null) {
            mixerThread.interrupt();
            try {
                mixerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (Entry<DataLine, DataLineAdapter> input : inputListeners.entrySet()) {
            input.getKey().removeDataLineListener(input.getValue());
        }
    }

    @Override
    public void close() {
        shutdown();
    }
}
