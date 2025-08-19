/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.ChannelsCountMismatchException;
import org.theko.sound.LengthMismatchException;
import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.utility.ArrayUtilities;
import org.theko.sound.utility.SamplesUtilities;

/**
 * StereoWidthEffect is an audio effect that adjusts the stereo width of audio samples.
 * It allows for real-time manipulation of the stereo field, enhancing or narrowing the perceived
 * spatial width of the sound.
 * 
 * This effect can be applied to audio samples in real-time, making it useful for creative sound design
 * and spatial audio applications.
 * 
 * @since v2.1.0
 * @author Theko
 */
public class StereoWidthEffect extends AudioEffect {
    protected final FloatControl stereoWidth = new FloatControl("Stereo Width", -1.0f, 1.0f, 0.0f);

    protected final List<AudioControl> stereoWidthControls = List.of(
        stereoWidth
    );

    public StereoWidthEffect() {
        super(Type.REALTIME);

        addControls(stereoWidthControls);
    }

    public FloatControl getStereoWidthControl() {
        return stereoWidth;
    }

    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        float[][] separated = SamplesUtilities.stereoSeparation(samples, stereoWidth.getValue());
        try {
            ArrayUtilities.copyArray(separated, samples);
        } catch (LengthMismatchException | ChannelsCountMismatchException e) {
            throw new RuntimeException("Error applying stereo width effect: " + e.getMessage(), e);
        }
    }
}