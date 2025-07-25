package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.utility.SamplesUtilities;

/**
 * Represents an audio effect that allows for real-time audio positioning
 * through gain and pan controls.
 * 
 * This effect can be applied to audio samples to adjust their perceived
 * position in the stereo field.
 * 
 * @author Theko
 * @since v2.1.0
 */
public class AudioPositioningEffect extends AudioEffect {

    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);
    protected final FloatControl panControl = new FloatControl("Pan", -1.0f, 1.0f, 0.0f);

    protected final List<AudioControl> positioningControls = List.of(
        gainControl,
        panControl
    );

    public AudioPositioningEffect () {
        super(Type.REALTIME);

        addControls(positioningControls);
    }

    @Override
    public void effectRender (float[][] samples, int sampleRate) {
        SamplesUtilities.adjustGainAndPan(samples, gainControl.getValue(), panControl.getValue());
    }

    public FloatControl getGain () {
        return gainControl;
    }

    public FloatControl getPan () {
        return panControl;
    }
}