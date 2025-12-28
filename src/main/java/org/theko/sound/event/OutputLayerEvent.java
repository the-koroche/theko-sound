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

package org.theko.sound.event;

import org.theko.events.Event;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioOutputLayer;

/**
 * Event object for {@link AudioOutputLayer} events.
 * It contains the audio formats and the buffer sizes of the output layer.
 * 
 * @since 2.4.0
 * @author Theko
 */
public class OutputLayerEvent extends Event {
    
    private final AudioFormat sourceFormat, targetFormat;
    private final int renderBufferSize, outBufferSize;
    private final int buffersCount;
    
    /**
     * Constructor for the OutputLayerEvent class.
     * @param sourceFormat The audio format of the source.
     * @param targetFormat The audio format of the target.
     * @param renderBufferSize The size of the render buffer.
     * @param outBufferSize The size of the output buffer.
     * @param buffersCount The number of audio ring buffers.
     */
    public OutputLayerEvent(AudioFormat sourceFormat, AudioFormat targetFormat, int renderBufferSize, int outBufferSize, int buffersCount) {
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.renderBufferSize = renderBufferSize;
        this.outBufferSize = outBufferSize;
        this.buffersCount = buffersCount;
    }

    /**
     * Returns the audio format of the source.
     * @return the source format
     */
    public AudioFormat getSourceFormat() { return sourceFormat; }

    /**
     * Returns the audio format of the opened line.
     * @return the opened format
     */
    public AudioFormat getTargetFormat() { return targetFormat; }

    /**
     * Returns the size of the render buffer.
     * @return the render buffer size
     */
    public int getRenderBufferSize() { return renderBufferSize; }

    /**
     * Returns the size of the output line buffer.
     * @return the output buffer size
     */
    public int getOutBufferSize() { return outBufferSize; }

    /**
     * Returns the number of audio ring buffers.
     * @return the number of buffers
     */
    public int getBuffersCount() { return buffersCount; }
}
