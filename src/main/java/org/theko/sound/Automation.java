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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.theko.sound.properties.AudioSystemProperties.AUTOMATIONS_UPDATE_TIME;

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.AudioControlGroup;
import org.theko.sound.util.MathUtilities;

/**
 * Represents an automation of audio controls over time.
 * It stores a list of keypoints (time, value, tension) which are used to interpolate the control values over time.
 * The automation can be played, looped, and scaled.
 * 
 * @see AudioControlGroup
 * @see Keypoint
 * 
 * @since 2.4.0
 * @author Theko
 */
public class Automation {
    private List<Keypoint> keypoints;
    private AudioControlGroup controls;

    protected final int updateTime;
    protected static final int DEFAULT_UPDATE_TIME = AUTOMATIONS_UPDATE_TIME;

    private float playhead = 0.0f;
    private volatile boolean isPlaying;
    private volatile boolean isLooping;
    private float timeScale;

    /**
     * Represents a keypoint in an automation.
     * It stores the time, value, and tension of the keypoint.
     * 
     * @since 2.4.0
     * @author Theko
     */
    public static class Keypoint {
        private float time;
        private float value;
        private float tension;

        public Keypoint(float time, float value, float tension) {
            this.time = Math.max(0, time);
            this.value = value;
            this.tension = MathUtilities.clamp(tension, -1, 1);
        }

        public void setTime(float time) { this.time = Math.max(0, time); }
        public void setValue(float value) { this.value = value; }
        public void setTension(float tension) { this.tension = MathUtilities.clamp(tension, -1, 1); }

        public float getTime() { return time; }
        public float getValue() { return value; }
        public float getTension() { return tension; }

        @Override
        public String toString() {
            return String.format("Keypoint{Time: %.2f, Value: %.2f, Tension: %.2f}", time, value, tension);
        }
    }

    /**
     * Creates an automation with the given keypoints and controls.
     * 
     * @param keypoints The keypoints of the automation.
     * @param controls The controls of the automation.
     * @param updateTime The update time of the automation in milliseconds.
     */
    public Automation(List<Keypoint> keypoints, List<AudioControl> controls, int updateTime) {
        this.keypoints = new ArrayList<>();
        this.keypoints.addAll(keypoints);
        this.controls = new AudioControlGroup(controls);
        this.updateTime = updateTime;

        isPlaying = false;
        isLooping = false;
        timeScale = 1.0f;
    }

    /**
     * Creates an automation with the given keypoints and controls.
     * 
     * @param keypoints The keypoints of the automation.
     * @param controls The controls of the automation.
     */
    public Automation(List<Keypoint> keypoints, List<AudioControl> controls) {
        this(keypoints, controls, DEFAULT_UPDATE_TIME);
    }

    /**
     * Creates an automation with the given controls.
     * 
     * @param controls The controls of the automation.
     */
    public Automation(List<AudioControl> controls) {
        this(List.of(), controls, DEFAULT_UPDATE_TIME);
    }

    /**
     * Creates an empty automation.
     */
    public Automation() {
        this(List.of(), List.of(), DEFAULT_UPDATE_TIME);
    }

    /**
     * Adds a control to the automation.
     * 
     * @param control The control to add.
     */
    public void addControl(AudioControl control) {
        controls.addControl(control);
    }

    /**
     * Removes a control from the automation.
     * 
     * @param control The control to remove.
     */
    public void removeControl(AudioControl control) {
        controls.removeControl(control);
    }

    /**
     * Adds a keypoint to the automation at the given time with the given value and tension.
     * 
     * @param time The time of the keypoint in seconds.
     * @param value The value of the keypoint.
     * @param tension The tension of the keypoint.
     */
    public void addKeypoint(float time, float value, float tension) {
        keypoints.add(new Keypoint(time, value, tension));
    }

    /**
     * Adds a keypoint to the automation.
     * 
     * @param keypoint The keypoint to add.
     */
    public void addKeypoint(Keypoint keypoint) {
        keypoints.add(keypoint);
    }

    /**
     * Removes a keypoint from the automation.
     * 
     * @param keypoint The keypoint to remove.
     */
    public void removeKeypoint(Keypoint keypoint) {
        keypoints.remove(keypoint);
    }

    /**
     * Returns an unmodifiable list of all keypoints in the automation.
     * This list is a copy of the internal list and is not updated when the automation is modified.
     * It is intended for use in rendering the automation or other forms of visualization.
     * 
     * @return An unmodifiable list of keypoints.
     */
    public List<Keypoint> getKeypoints() {
        return Collections.unmodifiableList(keypoints);
    }

    /**
     * Starts the playback of the automation.
     * If the automation is already playing, this method does nothing.
     * 
     * @see #stop()
     * @see #loop()
     * @see #unloop()
     */
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            AutomationsThreadPool.submit(this::process);
        }
    }

    /**
     * Stops the playback of the automation.
     * If the automation is not playing, this method does nothing.
     * 
     * @see #play()
     * @see #loop()
     * @see #unloop()
     */
    public void stop() {
        isPlaying = false;
    }

    /**
     * Returns whether the automation is currently playing.
     * 
     * @return true if the automation is playing, false otherwise.
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Sets the automation to loop indefinitely when it reaches the end.
     * This method doesn't start the playback of the automation.
     * 
     * @see #play()
     * @see #stop()
     * @see #unloop()
     */
    public void loop() {
        isLooping = true;
    }

    /**
     * Disables the looping of the automation.
     * When the automation is no longer looping, it will stop when it reaches the end.
     * This method doesn't start the playback of the automation.
     * 
     * @see #play()
     * @see #stop()
     * @see #loop()
     */
    public void unloop() {
        isLooping = false;
    }

    /**
     * Returns the duration of the automation in seconds.
     * This method will return the maximum time value of all keypoints in the automation.
     * If the automation has no keypoints, it will return 0.0f.
     * 
     * @return The duration of the automation in seconds.
     */
    public float getDuration() {
        return keypoints.stream()
            .map(Keypoint::getTime)
            .max(Float::compare)
            .orElse(0.0f);
    }

    /**
     * Returns the maximum value of all keypoints in the automation.
     * This method will return the maximum value of all keypoints in the automation.
     * If the automation has no keypoints, it will return 0.0f.
     * 
     * @return The maximum value of all keypoints in the automation.
     */
    public float getMaxValue() {
        return keypoints.stream()
            .map(Keypoint::getValue)
            .max(Float::compare)
            .orElse(0.0f);
    }

    /**
     * Returns the minimum value of all keypoints in the automation.
     * This method will return the minimum value of all keypoints in the automation.
     * If the automation has no keypoints, it will return 0.0f.
     * 
     * @return The minimum value of all keypoints in the automation.
     */
    public float getMinValue() {
        return keypoints.stream()
            .map(Keypoint::getValue)
            .min(Float::compare)
            .orElse(0.0f);
    }

    /**
     * Sets the current time of the automation in seconds.
     * This method allows for jumping to a specific time in the automation.
     * The automation will start playing from the specified time when the next {@link #play()} call is made.
     * If the automation is already playing, this method will stop the automation and reset it to the specified time.
     * 
     * @param time The time to set the automation to in seconds.
     */
    public void setTime(float time) {
        playhead = time;
    }

    /**
     * Returns the current time of the automation in seconds.
     * This method is used to get the current time of the automation.
     * The current time is the time that the automation is currently playing at.
     * This method is useful for getting the current time of the automation
     * when it is already playing.
     *
     * @return The current time of the automation in seconds.
     */
    public float getTime() {
        return playhead;
    }

    /**
     * Sets the time scale of the automation.
     * A time scale of 1.0f will play the automation at its normal speed.
     * A time scale of 2.0f will play the automation at twice its normal speed.
     * A time scale of 0.5f will play the automation at half its normal speed.
     * 
     * @param scale The time scale of the automation.
     */
    public void setTimeScale(float scale) {
        timeScale = scale;
    }

    /**
     * Returns the time scale of the automation.
     * 
     * @return The time scale of the automation.
     */
    public float getTimeScale() {
        return timeScale;
    }

    /**
     * This method is used to process the automation in the background.
     * It updates the playhead and controls the effects based on the automation's time scale and current time.
     * It also handles looping and stopping the automation when the end is reached.
     * This method is called by the automation's thread and should not be called directly by the user.
     * 
     * @see #play()
     * @see #stop()
     * @see #isPlaying()
     * @see #getTimeScale()
     * @see #getTime()
     */
    protected void process() {
        long lastTime = System.nanoTime();

        while (isPlaying && !Thread.currentThread().isInterrupted()) {
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000f * timeScale;
            lastTime = now;

            playhead += deltaTime;

            if (keypoints.size() > 0) {
                float maxTime = keypoints.get(keypoints.size() - 1).getTime();
                if (playhead > maxTime) {
                    if (isLooping) {
                        playhead %= maxTime;
                    } else {
                        stop();
                        break;
                    }
                }
            }

            float value = getValue(playhead);

            controls.apply(value);

            try {
                Thread.sleep(updateTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Retrieves the value of the automation at the given time.
     * 
     * @param time The time to retrieve the value for.
     * @return The value of the automation at the given time.
     * @see #getValue(float)
     */
    public float getValue(float time) {
        if (keypoints.isEmpty()) return 0.0f;
        if (keypoints.size() == 1) return keypoints.get(0).getValue();

        // Find the two keypoints that the playhead is between
        Keypoint k0 = null, k1 = null;
        for (int i = 0; i < keypoints.size() - 1; i++) {
            if (time >= keypoints.get(i).getTime() && time <= keypoints.get(i + 1).getTime()) {
                k0 = keypoints.get(i);
                k1 = keypoints.get(i + 1);
                break;
            }
        }

        if (k0 == null || k1 == null) {
            if (time < keypoints.get(0).getTime()) return keypoints.get(0).getValue();
            else return keypoints.get(keypoints.size() - 1).getValue();
        }

        float t = (time - k0.getTime()) / (k1.getTime() - k0.getTime());
        float tension = k0.getTension() * 8.0f;

        float modifiedT;
        float alpha = 1.0f + Math.abs(tension);
        if (tension >= 0) {
            modifiedT = 1.0f - (float)Math.pow(1.0 - t, alpha);
        } else {
            modifiedT = (float)Math.pow(t, alpha);
        }

        return k0.getValue() + (k1.getValue() - k0.getValue()) * modifiedT;
    }
}
