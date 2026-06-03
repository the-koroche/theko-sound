/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

package org.theko.sound.dsp;

import java.util.Objects;

import org.theko.sound.controls.FloatControl;

/**
 * An abstract audio filter with cutoff control and
 * a filter type (lowpass, highpass, etc).
 *
 * @since 0.3.1-beta
 */
public abstract class CutoffAudioFilter implements AudioFilter {

    /** The filter type (lowpass, highpass, etc) */
    protected final FilterType filterType;

    /**
     * Base constructor for a cutoff audio filter.
     * @param filterType the filter type
     */
    public CutoffAudioFilter(FilterType filterType) {
        this.filterType = Objects.requireNonNull(filterType);
    }

    /**
     * @return the filter type
     */
    public FilterType getFilterType() {
        return filterType;
    }

    /**
     * @return the cutoff control
     */
    public abstract FloatControl getCutoff();

    /**
     * Sets the cutoff value.
     * @param cutoff the cutoff value
     */
    public void setCutoffValue(float cutoff) {
        getCutoff().setValue(cutoff);
    }

    /**
     * @return the cutoff value
     */
    public float getCutoffValue() {
        return getCutoff().getValue();
    }
}
