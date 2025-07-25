package org.theko.sound.generator;

import org.theko.sound.AudioNode;

public abstract class AudioGenerator implements AudioNode {

    @Override
    public abstract void render (float[][] samples, int sampleRate);
}
