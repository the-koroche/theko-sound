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

package org.theko.sound.utility;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Locale;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.theko.sound.control.AudioControl;

/**
 * A utility class for visualizing the history of an {@link AudioControl} over time.
 * <p>
 * It can be used to visualize the history of any control, such as a volume control or a filter cutoff.
 * 
 * @see AudioControl
 * 
 * @author Theko
 * @since 2.4.1
 */
public class ControlVisualizer extends JPanel {

    private final AudioControl control;
    private float historyMaxTime = 2.0f; // in seconds
    protected int historyMaxSamples;
    protected float[] historySamples;
    private int historyIndex = 0;
    private int historyCount = 0;
    private final int frameRate;
    private final Timer updateTimer;

    private float historyMinValue = Float.POSITIVE_INFINITY;
    private float historyMaxValue = Float.NEGATIVE_INFINITY;

    private final BasicStroke basicStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private boolean drawValueLine = true;
    private float valueLineWidth = 1.0f;
    private BasicStroke valueLineStroke = new BasicStroke(valueLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private Color valueLineColor = Color.RED;

    private boolean drawGrid = true;
    private int gridLines = 10;
    private Color gridColor = Color.GRAY;

    private boolean drawValueText = true;
    private Color valueTextBackgroundColor = new Color(0, 0, 0, 128);
    private Color valueTextColor = Color.WHITE;
    private int valueTextPadding = 5;
    private int valueTextSize = 24;
    private Font valueTextFont = new Font("Arial", Font.BOLD, valueTextSize);

    private Color historyColor = Color.WHITE;
    private int historyPathWidth = 1;
    private BasicStroke historyPathStroke = new BasicStroke(historyPathWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    /**
     * Creates a new ControlVisualizer.
     * @param control The control to visualize.
     * @param frameRate The frame rate in frames per second.
     */
    public ControlVisualizer(AudioControl control, int frameRate) {
        Objects.requireNonNull(control);
        if (frameRate <= 0) throw new IllegalArgumentException("frameRate must be greater than 0");
        this.control = control;
        this.frameRate = frameRate;
        
        this.historyMaxSamples = Math.max(1, (int) (historyMaxTime * frameRate));
        historySamples = new float[historyMaxSamples];

        updateTimer = new Timer(Math.max(1, 1000 / frameRate), e -> {
            updateHistory();
            repaint();
        });
        updateTimer.start();

        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));
    }

    /**
     * Creates a new ControlVisualizer with a default frame rate of 60 frames per second.
     * @param control The control to visualize.
     */
    public ControlVisualizer(AudioControl control) {
        this(control, 60);
    }

    /**
     * Updates control value history.
     *
     * <p>Called automatically by the timer at the frame rate set in the constructor.
     * <p>Retrieves the current {@link AudioControl} value, updates min/max,
     * stores it in the history array, and refreshes range every 4 samples.
     */
    protected void updateHistory() {
        float value = AudioControlUtilities.getValueAsFloat(control);
        
        if (value < historyMinValue) historyMinValue = value;
        if (value > historyMaxValue) historyMaxValue = value;
        
        historySamples[historyIndex] = value;
        historyIndex = (historyIndex + 1) % historyMaxSamples;
        if (historyCount < historyMaxSamples) historyCount++;
        
        if (historyIndex % 4 == 0) {
            getRange();
        }
    }

    /**
     * Retrieves the minimum and maximum values from the history array and updates
     * {@link #historyMinValue} and {@link #historyMaxValue} accordingly.
     * 
     * <p>Called automatically by the timer at the frame rate set in the constructor.
     * <p>If the range is very small (< 0.001f), it is artificially expanded to
     * be centered around the midpoint of the range, with a range of 1.0f.
     */
    protected void getRange() {
        if (historyCount == 0) {
            historyMinValue = 0;
            historyMaxValue = 0;
            return;
        }
        
        historyMinValue = Float.POSITIVE_INFINITY;
        historyMaxValue = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < historyCount; i++) {
            int idx = (historyIndex - 1 - i + historyMaxSamples) % historyMaxSamples;
            float v = historySamples[idx];
            if (v < historyMinValue) historyMinValue = v;
            if (v > historyMaxValue) historyMaxValue = v;
        }
        
        if (Math.abs(historyMaxValue - historyMinValue) < 0.001f) {
            float center = (historyMaxValue + historyMinValue) / 2;
            historyMinValue = center - 0.5f;
            historyMaxValue = center + 0.5f;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        setupG2D(g2d);

        int w = getWidth();
        int h = getHeight();

        float actualMin = historyMinValue;
        float actualMax = historyMaxValue;
        float range = Math.max(0.001f, actualMax - actualMin);

        float scaleX = (float) w / Math.max(1, historyCount - 1);
        float scaleY = (float) h / range;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, w, h);

        if (drawGrid) {
            drawGrid(g2d, w, h, actualMin, actualMax, range);
        }

        if (historyCount > 0) {
            drawHistoryPath(g2d, w, h, actualMin, scaleX, scaleY);
        }

        if (drawValueLine && historyCount > 0) {
            drawCurrentValueLine(g2d, w, h, actualMin, range);
        }

        if (drawValueText) {
            drawCurrentValueText(g2d, w, h);
        }
    }

    /**
     * Draws a grid on the given graphics context.
     * 
     * @param g2d the graphics context to draw on.
     * @param w the width of the component.
     * @param h the height of the component.
     * @param actualMin the minimum y value of the component.
     * @param actualMax the maximum y value of the component.
     * @param range the range of y values (actualMax - actualMin).
     */
    protected void drawGrid(Graphics2D g2d, int w, int h, float actualMin, float actualMax, float range) {
        g2d.setColor(gridColor);
        
        for (int i = 0; i <= gridLines; i++) {
            float value = actualMin + i * (range) / gridLines;
            int y = (int) (h - (value - actualMin) * h / range);
            g2d.drawLine(0, y, w, y);

            String label = String.format(Locale.US, "%.2f", value);
            g2d.drawString(label, 2, y - 2);
        }
    }

    /**
     * Draws a path representing the history of values on the given graphics context.
     * 
     * @param g2d the graphics context to draw on.
     * @param w the width of the component.
     * @param h the height of the component.
     * @param actualMin the minimum y value of the component.
     * @param actualMax the maximum y value of the component.
     * @param range the range of y values (actualMax - actualMin).
     * @param scaleX the scale factor for the x axis.
     * @param scaleY the scale factor for the y axis.
     */
    protected void drawHistoryPath(Graphics2D g2d, int w, int h, float actualMin, float scaleX, float scaleY) {
        g2d.setColor(historyColor);

        if (drawValueLine && historyCount > 1) {
            Path2D.Float path = new Path2D.Float();
            int start = (historyIndex - historyCount + historyMaxSamples) % historyMaxSamples;

            boolean firstPoint = true;
            for (int j = 0; j < historyCount; j++) {
                int idx = (start + j) % historyMaxSamples;
                float v = historySamples[idx];

                float y = h - (v - actualMin) * scaleY;
                float x = j * scaleX;

                if (firstPoint) {
                    path.moveTo(x, y);
                    firstPoint = false;
                } else {
                    path.lineTo(x, y);
                }
            }

            g2d.setStroke(historyPathStroke);
            g2d.draw(path);
            g2d.setStroke(basicStroke);
        }
    }

    /**
     * Draws a line representing the current value on the given graphics context.
     * <p>The line is drawn at the y position corresponding to the current value.
     * 
     * @param g2d the graphics context to draw on.
     * @param w the width of the component.
     * @param h the height of the component.
     * @param actualMin the minimum y value of the component.
     * @param range the range of y values (actualMax - actualMin).
     */
    protected void drawCurrentValueLine(Graphics2D g2d, int w, int h, float actualMin, float range) {
        float value = historySamples[(historyIndex - 1 + historyMaxSamples) % historyMaxSamples];
        int y = (int) (h - (value - actualMin) * h / range);

        g2d.setColor(valueLineColor);
        g2d.setStroke(valueLineStroke);
        g2d.drawLine(0, y, w, y);
    }

    /**
     * Draws the current value of the associated control as a text on the given graphics context.
     * <p>The text is drawn at the bottom right corner of the component.
     * 
     * @param g2d the graphics context to draw on.
     * @param w the width of the component.
     * @param h the height of the component.
     */
    protected void drawCurrentValueText(Graphics2D g2d, int w, int h) {
        String valueText = String.format(Locale.US, "%.3f", AudioControlUtilities.getValueAsFloat(control));

        g2d.setFont(valueTextFont);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(valueText);
        int textHeight = fm.getAscent();

        int x = w - textWidth - valueTextPadding;
        int y = h - valueTextPadding;

        g2d.setColor(valueTextBackgroundColor);
        g2d.fillRect(x - 2, y - textHeight, textWidth + 4, textHeight + 2);
        
        g2d.setColor(valueTextColor);
        g2d.drawString(valueText, x, y);
    }

    private void setupG2D(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * Sets the maximum time period in seconds for which the control value history is kept.
     * <p>A minimum value of 0.2f is enforced to prevent very short history periods.
     * 
     * @param historyTime the maximum time period in seconds for which the control value history is kept.
     */
    public void setHistoryMaxTime(float historyTime) {
        if (historyTime < 0.2f) historyTime = 0.2f;
        this.historyMaxTime = historyTime;
        
        this.historyMaxSamples = Math.max(1, (int) (historyTime * frameRate));
        float[] newHistory = new float[historyMaxSamples];
        
        // Copy old history samples to new array
        int copyCount = Math.min(historyCount, historyMaxSamples);
        for (int i = 0; i < copyCount; i++) {
            int oldIndex = (historyIndex - copyCount + i + historySamples.length) % historySamples.length;
            newHistory[i] = historySamples[oldIndex];
        }
        
        this.historySamples = newHistory;
        this.historyIndex = copyCount % historyMaxSamples;
        this.historyCount = copyCount;
        getRange();
    }

    /**
     * Retrieves the maximum time period in seconds for which the control value history is kept.
     * 
     * @return The maximum history time period in seconds.
     */
    public float getHistoryMaxTime() {
        return historyMaxTime;
    }

    /**
     * Returns the associated AudioControl object that is being visualized.
     * @return The AudioControl object.
     */
    public AudioControl getControl() {
        return control;
    }

    /**
     * Stops the update timer and disposes of any allocated resources.
     */
    public void dispose() {
        updateTimer.stop();
    }

    // Getters and Setters for Properties
    public void setValueLineVisible(boolean drawValueLine) {
        this.drawValueLine = drawValueLine;
    }

    public boolean isValueLineVisible() {
        return drawValueLine;
    }
    
    public void setValueLineWidth(float width) {
        this.valueLineWidth = Math.max(0.5f, width);
        this.valueLineStroke = new BasicStroke(valueLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public float getValueLineWidth() {
        return valueLineWidth;
    }
    
    public void setValueLineColor(Color color) {
        Objects.requireNonNull(color);
        this.valueLineColor = color;
    }

    public Color getValueLineColor() {
        return valueLineColor;
    }
    
    public void setGridVisible(boolean drawGrid) {
        this.drawGrid = drawGrid;
    }

    public boolean isGridVisible() {
        return drawGrid;
    }

    public void setGridLines(int gridLines) {
        if (gridLines < 1) gridLines = 1;
        this.gridLines = gridLines;
    }

    public int getGridLines() {
        return gridLines;
    }

    public void setGridColor(Color color) {
        Objects.requireNonNull(color);
        this.gridColor = color;
    }

    public Color getGridColor() {
        return gridColor;
    }
    
    public void setValueTextVisible(boolean drawValueText) {
        this.drawValueText = drawValueText;
    }

    public boolean isValueTextVisible() {
        return drawValueText;
    }

    public void setValueTextBackgroundColor(Color color) {
        Objects.requireNonNull(color);
        this.valueTextBackgroundColor = color;
    }

    public Color getValueTextBackgroundColor() {
        return valueTextBackgroundColor;
    }

    public void setValueTextColor(Color color) {
        Objects.requireNonNull(color);
        this.valueTextColor = color;
    }

    public Color getValueTextColor() {
        return valueTextColor;
    }

    public void setValueTextPadding(int padding) {
        this.valueTextPadding = padding;
    }

    public int getValueTextPadding() {
        return valueTextPadding;
    }

    public void setValueTextFont(Font font) {
        Objects.requireNonNull(font);
        this.valueTextFont = font;
    }

    public Font getValueTextFont() {
        return valueTextFont;
    }
}