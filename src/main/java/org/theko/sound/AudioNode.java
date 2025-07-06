package org.theko.sound;

// since 2.0.0
public interface AudioNode {
    
    public void render (float[][] samples, int sampleRate, int length);
}
