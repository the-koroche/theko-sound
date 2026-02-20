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

package org.theko.sound.visualizers;

import java.util.Arrays;

/**
 * Utility class for spectrum visualization with interpolation methods,
 * and position mapping.
 * 
 * @since 2.4.1
 * @author Theko
 */
public final class SpectrumVisualizationUtilities {

    /**
     * Calculates the positions for a given array, given a width and scale factor.
     * The positions are calculated by taking the logarithm of the index plus one, 
     * normalizing it to the range [0, 1], and then scaling it to the range [0, width - 1].
     * The result is stored in the given outPositions array.
     * 
     * @param outPositions the array to store the calculated positions
     * @param width the width of the range to scale to
     * @param scale the scale factor to apply to the normalized positions
     * 
     * @throws IllegalArgumentException if the positions array is null or empty,
     *                               or if the width is less than or equal to 0,
     *                               or if the scale is less than or equal to 0
     */
    public static void getScaledPositions(float[] outPositions, int width, float scale) {
        if (outPositions == null || outPositions.length == 0) {
            throw new IllegalArgumentException("Positions array cannot be null or empty.");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be greater than 0.");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be greater than 0.");
        }

        for (int i = 0; i < outPositions.length; i++) {
            double normalized = Math.log10(1 + i) / Math.log10(1 + outPositions.length);
            double scaled = Math.pow(normalized, scale);
            outPositions[i] = (float) (scaled * (width - 1));
        }
    }

    /**
     * Maps the input spectrum to the output spectrum using nearest neighbor resampling.
     * The mapping is done by iterating over the positions array and setting the value of the output
     * spectrum at the corresponding position to the value of the input spectrum at the same index.
     * If the output spectrum value at the position is already greater than the input spectrum value,
     * it is left unchanged.
     *
     * @param inputSpectrum the input spectrum to map
     * @param positions the positions array to use for mapping
     * @param interpolatedSpectrumOut the output spectrum to map to
     * 
     * @throws IllegalArgumentException if the input spectrum, positions array, or output spectrum is null or empty
     */
    public static void mapSpectrumNearest(float[] inputSpectrum, float[] positions, float[] interpolatedSpectrumOut) {
        if (inputSpectrum == null || inputSpectrum.length == 0) {
            throw new IllegalArgumentException("Input spectrum cannot be null or empty.");
        }
        if (positions == null || positions.length == 0) {
            throw new IllegalArgumentException("Positions array cannot be null or empty.");
        }
        if (interpolatedSpectrumOut == null || interpolatedSpectrumOut.length == 0) {
            throw new IllegalArgumentException("Output spectrum cannot be null or empty.");
        }

        Arrays.fill(interpolatedSpectrumOut, 0f);

        for (int i = 0; i < positions.length - 1; i++) {
            int startX = (int) positions[i];
            int endX = (int) positions[i + 1];

            if (startX >= interpolatedSpectrumOut.length || endX >= interpolatedSpectrumOut.length) {
                continue;
            }

            float startValue = inputSpectrum[i];

            for (int x = startX; x <= endX; x++) {
                if (startValue > interpolatedSpectrumOut[x]) {
                    interpolatedSpectrumOut[x] = startValue;
                }
            }
        }
    }

    /**
     * Maps the input spectrum to the output spectrum using linear interpolation.
     * The mapping is done by iterating over the positions array and setting the value of the output
     * spectrum at the corresponding position to the interpolated value of the input spectrum at the same index.
     * If the output spectrum value at the position is already greater than the input spectrum value,
     * it is left unchanged.
     *
     * @param inputSpectrum the input spectrum to map
     * @param positions the positions array to use for mapping
     * @param interpolatedSpectrumOut the output spectrum to map to
     * 
     * @throws IllegalArgumentException if the input spectrum, positions array, or output spectrum is null or empty
     */
    public static void mapSpectrumLinear(float[] inputSpectrum, float[] positions, float[] interpolatedSpectrumOut) {
        if (inputSpectrum == null || inputSpectrum.length == 0) {
            throw new IllegalArgumentException("Input spectrum cannot be null or empty.");
        }
        if (positions == null || positions.length == 0) {
            throw new IllegalArgumentException("Positions array cannot be null or empty.");
        }
        if (interpolatedSpectrumOut == null || interpolatedSpectrumOut.length == 0) {
            throw new IllegalArgumentException("Output spectrum cannot be null or empty.");
        }

        Arrays.fill(interpolatedSpectrumOut, 0f);

        for (int i = 0; i < positions.length - 1; i++) {
            int startX = (int) positions[i];
            int endX = (int) positions[i + 1];

            if (startX >= interpolatedSpectrumOut.length || endX >= interpolatedSpectrumOut.length) {
                continue;
            }

            float startValue = inputSpectrum[i];
            float endValue = inputSpectrum[i + 1];

            for (int x = startX; x <= endX; x++) {
                float t = (x - positions[i]) / (positions[i + 1] - positions[i] + 1e-6f); // Add epsilon to avoid division by zero
                t = Math.max(0, Math.min(1, t)); // clamp

                // Lerp
                float value = startValue * (1 - t) + endValue * t;
                if (value > interpolatedSpectrumOut[x]) {
                    interpolatedSpectrumOut[x] = value;
                }
            }
        }
    }

    /**
     * Maps the input spectrum to the output spectrum using smooth catmull-Rom interpolation.
     * The mapping is done by iterating over the positions array and setting the value of the output
     * spectrum at the corresponding position to the interpolated value of the input spectrum at the same index.
     * If the output spectrum value at the position is already greater than the input spectrum value,
     * it is left unchanged.
     *
     * @param inputSpectrum the input spectrum to map
     * @param positions the positions array to use for mapping
     * @param out the output spectrum to map to
     * 
     * @throws IllegalArgumentException if the input spectrum, positions array, or output spectrum is null or empty
     */
    public static void mapSpectrumCubic(
        float[] inputSpectrum,
        float[] positions,
        float[] out)
    {
        if (inputSpectrum == null || inputSpectrum.length == 0)
            throw new IllegalArgumentException("Input spectrum cannot be null or empty.");
        if (positions == null || positions.length == 0)
            throw new IllegalArgumentException("Positions array cannot be null or empty.");
        if (out == null || out.length == 0)
            throw new IllegalArgumentException("Output spectrum cannot be null or empty.");

        Arrays.fill(out, 0f);

        for (int i = 0; i < positions.length - 1; i++) {

            int startX = (int) positions[i];
            int endX   = (int) positions[i + 1];

            if (startX >= out.length || endX >= out.length)
                continue;
            if (endX < startX)
                continue;

            float p0 = inputSpectrum[Math.max(i - 1, 0)];
            float p1 = inputSpectrum[i];
            float p2 = inputSpectrum[i + 1];
            float p3 = inputSpectrum[Math.min(i + 2, inputSpectrum.length - 1)];

            float dx = (positions[i + 1] - positions[i]);
            if (dx <= 0) dx = 1;

            for (int x = startX; x <= endX; x++) {
                float t = (x - positions[i]) / dx;
                if (t < 0) t = 0;
                if (t > 1) t = 1;

                float value = catmullRom(p0, p1, p2, p3, t);

                if (value > out[x])
                    out[x] = value;
            }
        }
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        return 0.5f * (
                2f * p1 +
                (-p0 + p2) * t +
                (2f*p0 - 5f*p1 + 4f*p2 - p3) * t2 +
                (-p0 + 3f*p1 - 3f*p2 + p3) * t3
        );
    }

    /**
     * Maps the input spectrum to the output spectrum by dividing the window into fixed width bars
     * and filling each bar with the maximum value found in the bar.
     * The mapping is done by iterating over the positions array and setting the value of the output
     * spectrum at the corresponding position to the interpolated value of the input spectrum at the same index.
     * If the output spectrum value at the position is already greater than the input spectrum value,
     * it is left unchanged.
     *
     * @param inputSpectrum the input spectrum to map
     * @param positions the positions array to use for mapping
     * @param windowWidth the width of the window to divide into fixed width bars
     * @param fixedWidthBarCount the number of fixed width bars to divide the window into
     * @param fixedWidthBarWidth the width of each fixed width bar as a fraction of the window width
     * @param interpolatedSpectrumOut the output spectrum to map to
     * 
     * @throws IllegalArgumentException if the input spectrum, positions array, or output spectrum is null or empty,
     *                               or if the window width is less than or equal to 0,
     *                               or if the fixed width bar count is less than or equal to 0
     */
    public static void mapSpectrumFixedWidth(
        float[] inputSpectrum, 
        float[] positions,
        int windowWidth, 
        int fixedWidthBarCount, 
        float fixedWidthBarWidth, 
        float[] interpolatedSpectrumOut
    ) {
        if (inputSpectrum == null || inputSpectrum.length == 0) {
            throw new IllegalArgumentException("Input spectrum cannot be null or empty.");
        }
        if (positions == null || positions.length == 0) {
            throw new IllegalArgumentException("Positions array cannot be null or empty.");
        }
        if (interpolatedSpectrumOut == null || interpolatedSpectrumOut.length == 0) {
            throw new IllegalArgumentException("Output spectrum cannot be null or empty.");
        }
        if (windowWidth <= 0) {
            throw new IllegalArgumentException("Window width must be greater than 0.");
        }
        if (fixedWidthBarCount <= 0) {
            throw new IllegalArgumentException("Fixed width bar count must be greater than 0.");
        }
        mapSpectrumLinear(inputSpectrum, positions, interpolatedSpectrumOut);

        int barCount = Math.min(fixedWidthBarCount, windowWidth);
        float step = Math.max(1, windowWidth / (float) barCount);
        fixedWidthBarWidth = Math.min(1, Math.max(0, fixedWidthBarWidth));
        float barPixelWidth = Math.max(1, step * fixedWidthBarWidth);

        for (int bar = 0; bar < barCount; bar++) {
            int startX = (int) (bar * step);
            int endX = Math.min(windowWidth - 1, (int) (startX + barPixelWidth));
            int nextStartX = Math.min(windowWidth - 1, (int) ((bar + 1) * step));

            // Find the maximum value in this bar
            float maxValue = 0.0f;
            for (int x = startX; x <= endX; x++) {
                maxValue = Math.max(maxValue, interpolatedSpectrumOut[x]);
            }

            // Fill the bar with the maximum value
            for (int x = startX; x <= endX; x++) {
                interpolatedSpectrumOut[x] = maxValue;
            }

            // Clear the rest of the bar
            for (int x = endX; x <= nextStartX; x++) {
                interpolatedSpectrumOut[x] = 0.0f;
            }
        }
    }
}
