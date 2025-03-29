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

    protected AudioFormat processAudioFormat;

    private final Map<DataLine, DataLineAdapter> inputListeners = new ConcurrentHashMap<>();

    protected final FloatController preGain;
    protected final FloatController postGain;
    protected final FloatController pan;

    private static final Cleaner cleaner = Cleaner.create(Thread.ofVirtual().factory());

    public AudioMixer(Mode mixerMode, AudioFormat processAudioFormat) {
        this.mixerMode = mixerMode;
        this.processAudioFormat = processAudioFormat;
        inputs = new CopyOnWriteArrayList<>();
        outputs = new CopyOnWriteArrayList<>();
        effects = new CopyOnWriteArrayList<>();

        mixerThread = (mixerMode == Mode.THREAD) ? new Thread(this::processLoop, "AudioMixer") : null;
        if (mixerThread != null) mixerThread.start();

        this.preGain = new FloatController("PRE-GAIN", 0.0f, 1.0f, 1.0f);
        this.postGain = new FloatController("POST-GAIN", 0.0f, 1.0f, 1.0f);
        this.pan = new FloatController("PAN", -1.0f, 1.0f, 0.0f);

        cleaner.register(this, this::shutdown);
    }

    public void addInput(DataLine input) throws UnsupportedAudioFormatException {
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
        if (effect.getType() == AudioEffect.Type.OFFLINE_PROCESSING) {
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

    public FloatController getPreGainController() {
        return preGain;
    }

    public FloatController getPostGainController() {
        return postGain;
    }

    public FloatController getPanController() {
        return pan;
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
        if (inputs.isEmpty() || outputs.isEmpty()) return;
    
        float[][][] samples = new float[inputs.size()][][];
        int maxLength = 0;
    
        for (int i = 0; i < inputs.size(); i++) {
            DataLine dl = inputs.get(i);
            if (dl != null) {
                samples[i] = SampleConverter.toSamples(dl.receiveWithTimeout(50, TimeUnit.MILLISECONDS), dl.getAudioFormat());
                if (samples[i] != null) {
                    for (float[] channel : samples[i]) {
                        maxLength = Math.max(maxLength, channel.length);
                    }
                }
            }
        }
    
        if (maxLength == 0) return; // Нет данных — выход
    
        int channels = processAudioFormat.getChannels();
        float[][] mixed = new float[channels][maxLength];
    
        // Смешивание с использованием preGain
        for (int k = 0; k < samples.length; k++) {
            if (samples[k] == null) continue;
            for (int ch = 0; ch < Math.min(channels, samples[k].length); ch++) {
                for (int j = 0; j < samples[k][ch].length; j++) {
                    mixed[ch][j] += samples[k][ch][j] * preGain.getValue();
                }
            }
        }
    
        if (!effects.isEmpty()) {
            mixed = applyEffects(mixed);
        }

        float leftVol = 1.0f;
        float rightVol = 1.0f;
    
        // Переход от панорамы (-1.0 для полного левого, 1.0 для полного правого)
        if (pan.getValue() < 0) {
            leftVol = 1.0f;
            rightVol = 1.0f + pan.getValue(); // Уменьшаем громкость правого канала при панораме влево
        } else if (pan.getValue() > 0) {
            leftVol = 1.0f - pan.getValue(); // Уменьшаем громкость левого канала при панораме вправо
            rightVol = 1.0f;
        } else {
            leftVol = 1.0f;
            rightVol = 1.0f;
        }
    
        // Нормализация и отправка данных
        float gain = postGain.getValue();
        for (DataLine output : outputs) {
            if (output != null) {
                for (int ch = 0; ch < mixed.length; ch++) {
                    for (int j = 0; j < mixed[ch].length; j++) {
                        mixed[ch][j] = Math.max(-1.0f, Math.min(1.0f, mixed[ch][j] * gain));
                    }
                }
                output.send(SampleConverter.fromSamples(mixed, output.getAudioFormat(), leftVol, rightVol));
            }
        }
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
