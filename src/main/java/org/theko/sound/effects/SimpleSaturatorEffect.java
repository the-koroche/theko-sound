package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;

/**
 * A simple saturator effect.
 * 
 * @since v2.1.1
 * @author Theko
 */
public class SimpleSaturatorEffect extends AudioEffect {
    protected final FloatControl saturation = new FloatControl("Saturation", 0.0f, 20.0f, 1.5f);
    protected final FloatControl dry = new FloatControl("Dry", 0.0f, 1.25f, 1.0f);
    protected final FloatControl wet = new FloatControl("Wet", 0.0f, 1.25f, 0.5f);

    protected final List<AudioControl> saturatorControls = List.of(saturation, dry, wet);

    public SimpleSaturatorEffect () {
        super(Type.REALTIME);

        addControls(saturatorControls);
    }

    public FloatControl getSaturation () {
        return saturation;
    }

    public FloatControl getDry () {
        return dry;
    }

    public FloatControl getWet () {
        return wet;
    }

    @Override
    public void effectRender (float[][] samples, int sampleRate) {
        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < samples[ch].length; i++) {
                float wet = (float)Math.tanh(samples[ch][i] * saturation.getValue());
                samples[ch][i] = wet * dry.getValue() + samples[ch][i] * dry.getValue();
            }
        }
    }
}
