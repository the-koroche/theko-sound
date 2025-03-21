package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.SampleConverter;

import java.util.ArrayList;
import java.util.List;

public class ReverbEffect extends AudioEffect {
    private final int sampleRate;
    private final int numChannels;
    
    private final List<CircularBuffer> delayBuffers;
    private final int[] delayTimesInSamples;
    private final float[] decayFactors;
    
    public ReverbEffect(AudioFormat audioFormat) {
        super(audioFormat);
        this.sampleRate = (int) audioFormat.getSampleRate();
        this.numChannels = audioFormat.getChannels();
        
        // Параметры реверберации
        double[] delayTimesInSeconds = {0.05, 0.10, 0.15};
        float[] decays = {0.6f, 0.4f, 0.2f};
        
        this.delayTimesInSamples = new int[delayTimesInSeconds.length];
        for(int i = 0; i < delayTimesInSeconds.length; i++) {
            delayTimesInSamples[i] = (int)(delayTimesInSeconds[i] * sampleRate);
        }
        this.decayFactors = decays;
        
        int maxDelay = 0;
        for(int delay : delayTimesInSamples) {
            if(delay > maxDelay) maxDelay = delay;
        }
        
        this.delayBuffers = new ArrayList<>();
        for(int i = 0; i < numChannels; i++) {
            delayBuffers.add(new CircularBuffer(maxDelay));
        }
    }

    @Override
    public byte[] process(byte[] data) {
        float[] amplitudes = SampleConverter.toAmplitude(data, audioFormat);
        float[] processed = applyReverb(amplitudes);
        return SampleConverter.fromAmplitude(amplitudes, audioFormat);
    }

    private float[] applyReverb(float[] input) {
        float[] output = new float[input.length];
        int samplesPerChannel = input.length / numChannels;
        
        for(int ch = 0; ch < numChannels; ch++) {
            CircularBuffer buffer = delayBuffers.get(ch);
            
            for(int i = 0; i < samplesPerChannel; i++) {
                int idx = i * numChannels + ch;
                float original = input[idx];
                float sum = original;
                
                // Добавляем задержанные сигналы
                for(int d = 0; d < delayTimesInSamples.length; d++) {
                    float delayed = buffer.getDelayed(delayTimesInSamples[d]);
                    sum += delayed * decayFactors[d];
                }
                
                // Ограничение амплитуды
                sum = Math.max(-1.0f, Math.min(1.0f, sum));
                
                // Обновляем буфер
                buffer.put(original);
                output[idx] = sum;
            }
        }
        
        return output;
    }

    private static class CircularBuffer {
        private final float[] buffer;
        private int pointer;
        
        public CircularBuffer(int size) {
            this.buffer = new float[size];
            this.pointer = 0;
        }
        
        public void put(float sample) {
            buffer[pointer] = sample;
            pointer = (pointer + 1) % buffer.length;
        }
        
        public float getDelayed(int delay) {
            int index = (pointer - delay + buffer.length) % buffer.length;
            return buffer[index];
        }
    }
}