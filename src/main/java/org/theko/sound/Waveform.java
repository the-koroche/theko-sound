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

package org.theko.sound;

/**
 * Represents a waveform shape with predefined generators
 * such as sine, square, triangle, and sawtooth.
 *
 * @since 0.3.0-beta
 * @author Theko
 */
public abstract class Waveform {

    public static final Waveform SINE = new Sine();
    public static final Waveform SQUARE = new Square(0.5f);
    public static final Waveform TRIANGLE = new Triangle();
    public static final Waveform SAWTOOTH = new Sawtooth();
    
    public static class Sine extends Waveform {
        @Override
        public float generate(float t) {
            return (float)Math.sin(t * Math.PI * 2);
        }
    }

    public static class Square extends Waveform {
        private final float duty;

        public Square(float duty) {
            this.duty = duty;
        }

        @Override
        public float generate(float t) {
            return t % 1f < duty ? 1f : -1f;
        }
    }

    public static class Triangle extends Waveform {
        @Override
        public float generate(float t) {
            return 1f - 4f * Math.abs(t % 1f - 0.5f);
        }
    }

    public static class Sawtooth extends Waveform {
        @Override
        public float generate(float t) {
            return 2f * (t % 1f) - 1f;
        }
    }

    public abstract float generate(float t);
}
