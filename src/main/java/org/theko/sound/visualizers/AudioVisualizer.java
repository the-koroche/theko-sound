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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.theko.sound.effects.AudioEffect;

/**
 * The {@code AudioVisualizer} class serves as an abstract base class for creating
 * custom audio visualizers. 
 * <p>It extends the {@code AudioEffect} class and provides
 * a framework for visualizing audio data in real-time.
 * 
 * @since 0.2.1-beta
 * @author Theko
 */
public abstract class AudioVisualizer extends AudioEffect implements Closeable {
    
    private RenderPanel panel;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Render render;
    private boolean pendingResize = false;
    private long lastResizeTime = 0;
    private final int resizeDelayMs;
    private final float frameRate;

    /** The audio samples buffer */
    private float[][] samplesBuffer;
    private int sampleRate;
    private int length;

    private long lastBufferUpdateTime;

    private class RenderPanel extends JPanel {
        public RenderPanel() {
            super();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            BufferedImage ready = render.getReadyImage();
            if (ready != null) {
                g2d.drawImage(ready, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

    /**
     * The {@code Render} class is a utility class used by {@code AudioVisualizer} to manage rendering.
     * 
     * @since 0.2.4-beta
     * @author Theko
     */
    protected class Render {
        private BufferedImage renderImage;
        private int imageType;
        private volatile BufferedImage readyImage;
        private Graphics2D g, readyG;
        private volatile long prevUpdateTime, lastUpdateTime;

        private int width, height;

        private final Object lock = new Object();

        /**
         * Creates a new Render instance.
         * @param width the width of the render area
         * @param height the height of the render area
         * @param imageType the type of the buffered image
         */
        protected Render(int width, int height, int imageType) {
            this.imageType = imageType;
            this.resize(width, height);
        }

        private void setupGraphics(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }

        /**
         * Returns the current render image.
         * <p>
         * This method is thread-safe and returns a reference to the current render image.
         * The returned image is a snapshot of the render area at the time of the method call
         * and may not reflect the current state of the render area.
         * <p>
         * The render image is updated by calling the {@link #resize(int, int)} method.
         * 
         * @return the current render image
         */
        protected BufferedImage getRenderImage() {
            return renderImage;
        }
        
        /**
         * Returns the graphics context for the render area.
         * <p>
         * This method is thread-safe and returns a reference to the graphics context
         * for the render area. The returned graphics context is a snapshot of the
         * render area at the time of the method call and may not reflect the current
         * state of the render area.
         * 
         * @return the graphics context for the render area
         */
        protected Graphics2D getGraphics() {
            synchronized (lock) {
                return g;
            }
        }
        
        /**
         * Returns the ready image.
         * <p>
         * This method is thread-safe and returns a reference to the ready image.
         * The ready image is a snapshot of the render area at the time of the method call
         * and may not reflect the current state of the render area.
         * <p>
         * The ready image is updated by calling the {@link #updateReadyImage()} method.
         * 
         * @return the ready image
         */
        protected BufferedImage getReadyImage() {
            synchronized (lock) {
                return readyImage;
            }
        }
        
        /**
         * Updates the ready image with the current render image.
         * <p>
         * This method is thread-safe and updates the ready image with the current render
         * image. It does nothing if the ready graphics context or the render image is
         * null.
         */
        protected void updateReadyImage() {
            synchronized (lock) {
                if (readyG != null && renderImage != null) {
                    readyG.drawImage(renderImage, 0, 0, null);
                }
            }
        }

        /**
         * Returns the width of the render area.
         * 
         * @return the width.
         */
        protected int getWidth() {
            return width;
        }

        /**
         * Returns the height of the render area.
         * 
         * @return the height
         */
        protected int getHeight() {
            return height;
        }

        /**
         * Returns the image type of the render area.
         * 
         * @return the image type
         */
        protected int getImageType() {
            return imageType;
        }

        /**
         * Updates the previous and last update times.
         * 
         * This method updates the previous and last update times with the current time.
         * It is thread-safe and should be called before any rendering operations.
         */
        protected void updateTime() {
            prevUpdateTime = lastUpdateTime;
            lastUpdateTime = System.nanoTime();
        }

        /**
         * @return the time difference between the last and previous update in nanoseconds.
         */
        protected long getTimeDelta() {
            return lastUpdateTime - prevUpdateTime;
        }

        /**
         * @return the time delta multiplier for frame-independent animations.
         */
        protected float getTimeDeltaMult() {
            float deltaSec = getTimeDelta() / 1_000_000_000f;
            return deltaSec * frameRate;
        }

        /**
         * Resizes the render area to the specified size.
         * <p>
         * This method resizes the render area to the specified size. It is thread-safe and
         * is designed to be used with the {@link Render} interface.
         * 
         * @param newWidth The new width of the render area.
         * @param newHeight The new height of the render area.
         * @return This object, for chaining purposes.
         */
        protected Render resize(int newWidth, int newHeight) {
            synchronized (lock) {
                this.width = Math.max(newWidth, 1);
                this.height = Math.max(newHeight, 1);

                // Create new images
                BufferedImage newRenderImage = new BufferedImage(width, height, imageType);
                BufferedImage newReadyImage = new BufferedImage(width, height, imageType);
                
                // Copy contents of old images to new ones
                Graphics2D tempG = newRenderImage.createGraphics();
                setupGraphics(tempG);
                if (renderImage != null) {
                    tempG.drawImage(renderImage, 0, 0, width, height, null);
                }
                tempG.dispose();
                
                tempG = newReadyImage.createGraphics();
                setupGraphics(tempG);
                if (readyImage != null) {
                    tempG.drawImage(readyImage, 0, 0, width, height, null);
                }
                tempG.dispose();
                
                // Replace old images and graphics contexts
                BufferedImage oldRender = renderImage;
                BufferedImage oldReady = readyImage;
                Graphics2D oldG = g;
                Graphics2D oldReadyG = readyG;
                
                renderImage = newRenderImage;
                readyImage = newReadyImage;
                g = renderImage.createGraphics();
                readyG = readyImage.createGraphics();
                setupGraphics(g);
                setupGraphics(readyG);
                
                // Dispose old resources
                if (oldG != null) oldG.dispose();
                if (oldReadyG != null) oldReadyG.dispose();
                if (oldRender != null) oldRender.flush();
                if (oldReady != null) oldReady.flush();
            }
            invalidate();
            return this;
        }

        protected void paint(Graphics2D g2d) {
            // Default implementation draws the black background
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setBackground(Color.BLACK);
            g2d.clearRect(0, 0, renderImage.getWidth(), renderImage.getHeight());
        }

        protected void invalidate() {
            synchronized (lock) {
                renderImage.flush();
                readyImage.flush();
            }
        }

        protected void close() {
            g.dispose();
            readyG.dispose();
            renderImage.flush();
            readyImage.flush();
        }
    }

    /**
     * Creates a new audio visualizer with the specified type and frame rate.
     * @param type The type of the audio visualizer (REALTIME or OFFLINE_PROCESSING)
     * @param frameRate The frame rate of the audio visualizer
     * @param resizeDelayMs The delay in milliseconds at which the render area is resized
     */
    public AudioVisualizer(Type type, float frameRate, int resizeDelayMs) {
        super(type);
        this.frameRate = frameRate;
        this.resizeDelayMs = resizeDelayMs;
        this.render = new Render(640, 480, BufferedImage.TYPE_INT_ARGB);
        this.panel = new RenderPanel();
        this.panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                pendingResize = true;
            }
        });
        initialize();
        initializeTimer();
    }

    /**
     * Creates a new audio visualizer with the specified type and frame rate, using a default resize delay of 500ms.
     * @param type The type of the audio visualizer (REALTIME or OFFLINE_PROCESSING)
     * @param frameRate The frame rate of the audio visualizer
     */
    public AudioVisualizer(Type type, float frameRate) {
        this(type, frameRate, 500);
    }

    /**
     * Creates a new audio visualizer with the specified type and default frame rate of 60.
     * @param type The type of the audio visualizer (REALTIME or OFFLINE_PROCESSING)
     */
    public AudioVisualizer(Type type) {
        this(type, 60);
    }

    private void initializeTimer() {
        executor.scheduleAtFixedRate(() -> {
            try {
                if (pendingResize && System.currentTimeMillis() - lastResizeTime > resizeDelayMs) {
                    render.resize(panel.getWidth(), panel.getHeight());
                    pendingResize = false;
                    lastResizeTime = System.currentTimeMillis();
                }
                Graphics2D g2d = render.getGraphics();
                if (g2d != null) {
                    render.updateTime();
                    g2d.setBackground(panel.getBackground());
                    g2d.clearRect(0, 0, render.getWidth(), render.getHeight());
                    render.paint(g2d);
                    render.updateReadyImage();
                }
                
                SwingUtilities.invokeLater(() -> panel.repaint());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, (long)(1000 / frameRate), TimeUnit.MILLISECONDS);
    }

    /**
     * Clones the audio samples buffer and updates the sample rate.
     * @param samples The audio samples
     * @param sampleRate The sample rate of the audio samples
     */
    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        int length = samples[0].length;
        samplesBuffer = new float[samples.length][length];
        for (int ch = 0; ch < samples.length; ch++) {
            System.arraycopy(samples[ch], 0, samplesBuffer[ch], 0, length);
        }
        this.sampleRate = sampleRate;
        this.length = samples[0].length;

        lastBufferUpdateTime = System.nanoTime();
        onBufferUpdate();
    }

    protected int getSamplesOffset() {
        long now = System.nanoTime();
        long delta = now - lastBufferUpdateTime;
        if (length > 0) {
            int elapsedSamples = (int) (delta * sampleRate / 1000000000f);
            return elapsedSamples % length;
        }
        return 0;
    }

    /**
     * Initializes the audio visualizer before the repaint timer starts.
     */
    protected abstract void initialize();

    /**
     * Repaint task. Executes by the 'repaintTimer'.
     * By default, it just repaints the panel returned by {@link #getPanel()}.
     */
    protected void repaint() {
        getPanel().repaint();
    }
    
    /**
     * Automatically called when the audio samples buffer is updated.
     */
    protected void onBufferUpdate() {}

    /**
     * Automatically called when the audio visualizer is closed.
     */
    protected void onEnd() {}

    /**
     * Returns the panel of the audio visualizer.
     * @return The panel of the audio visualizer
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Returns the frame rate of the audio visualizer.
     * @return The frame rate of the audio visualizer
     */
    public float getFrameRate() {
        return frameRate;
    }

    public BufferedImage getImage() {
        return render.getRenderImage();
    }

    public int getImageWidth() {
        return render.getWidth();
    }

    public int getImageHeight() {
        return render.getHeight();
    }

    /**
     * Closes the audio visualizer.
     */
    @Override
    public void close() {
        executor.shutdownNow();
        render.close();
        onEnd();
        panel.setVisible(false);
    }

    protected void setRender(Render render) {
        if (render == null)
            throw new IllegalArgumentException("Render cannot be null");
        this.render.close();
        this.render = render;
    }

    /**
     * Returns the audio samples buffer.
     * @return The audio samples buffer
     */
    protected float[][] getSamplesBuffer() {
        return samplesBuffer;
    }

    /**
     * Returns the sample rate of the audio samples.
     * @return The sample rate of the audio samples
     */
    protected int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the length of the audio samples.
     * @return The length of the audio samples
     */
    protected int getLength() {
        return length;
    }

    protected long getLastBufferUpdateTime() {
        return lastBufferUpdateTime;
    }
}
