package org.theko.sound;

import java.util.ArrayList;
import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;
import org.theko.sound.util.ThreadsFactory;
import org.theko.sound.util.ThreadsFactory.ThreadType;

/**
 * The Automation class represents an audio automation system that allows controlling
 * audio parameters over time using key points. It supports playback, looping, pausing,
 * and resuming of automation sequences.
 * 
 * <p>Each automation instance operates on a list of {@link KeyPoint} objects, which define
 * the time, value, and easing for the automation curve. The class also supports controlling
 * multiple {@link FloatControl} targets simultaneously.</p>
 * 
 * <p>Features include:</p>
 * <ul>
 *   <li>Adding, removing, and clearing key points.</li>
 *   <li>Controlling playback with start, stop, pause, and resume methods.</li>
 *   <li>Looping and speed adjustment.</li>
 *   <li>Interpolation between key points with linear, ease-in, and ease-out easing.</li>
 *   <li>Normalizing key point times to fit within a 0-1 range.</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * List<Automation.KeyPoint> keyPoints = List.of(
 *     new Automation.KeyPoint(0f, 0f, 0.5f),
 *     new Automation.KeyPoint(1f, 1f, 0.5f)
 * );
 * Automation automation = new Automation(keyPoints);
 * automation.addTargetControl(new FloatControl("Volume", 0, 1, 0.5f));
 * automation.start();
 * }</pre>
 * 
 * <p>Note: The playback thread is a daemon thread and runs with a lower priority.</p>
 * 
 * <p>Thread Safety: This class is not thread-safe. Ensure proper synchronization if accessed
 * from multiple threads.</p>
 * 
 * @see KeyPoint
 * @see FloatControl
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class Automation implements AudioObject, Controllable {
    private List<KeyPoint> keyPoints;
    private final FloatControl speed;
    private float position;
    private boolean isLooping = false;
    private boolean isPaused = false;
    private boolean isPlaying = false;
    private final transient Thread playThread;

    private float lastEasing = 0.8f;

    private static int automationInstances;
    private static final int thisAutomationInstance = ++automationInstances;

    private List<FloatControl> targetControls;

    public static class KeyPoint implements AudioObject{
        private float time;
        private float value; // 0 - 1
        private float easing; // 0.5 - linear, < or > - easing

        public KeyPoint (float time, float value, float easing) {
            this.time = time;
            this.value = value;
            this.easing = easing;
        }

        public float getTime() {
            return time;
        }

        public float getValue() {
            return value;
        }

        public float getEasing() {
            return easing;
        }

        public void setTime(float time) {
            this.time = time;
        }

        public void setValue(float value) {
            this.value = value;
        }

        public void setEasing(float easing) {
            this.easing = easing;
        }
    }

    public Automation(List<KeyPoint> keyPoints) {
        this.keyPoints = keyPoints;
        this.speed = new FloatControl("Speed", -4, 4, 1f);
        this.position = 0;
        this.playThread = ThreadsFactory.createThread(ThreadType.AUTOMATION, this::automationPlay, "Automation Play-" + thisAutomationInstance);
        this.playThread.setPriority(Thread.MIN_PRIORITY); // Set to minimum priority
    }

    public Automation() {
        this(new ArrayList<>());
    }

    public void addTargetControl(FloatControl control) {
        if (targetControls == null) {
            targetControls = new ArrayList<>();
        }
        targetControls.add(control);
    }

    public void removeTargetControl(FloatControl control) {
        if (targetControls != null) {
            targetControls.remove(control);
        }
    }

    public void clearTargetControls() {
        if (targetControls != null) {
            targetControls.clear();
        }
    }

    public void start() {
        if (!isPlaying) {
            isPlaying = true;
            playThread.start();
        }
    }

    public void stop() {
        isPlaying = false;
        if (playThread != null) {
            playThread.interrupt();
        }
    }

    public void pause() {
        isPaused = true;
        isPlaying = false;
    }

    public void resume() {
        isPaused = false;
        isPlaying = true;
        playThread.start();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isLooping() {
        return isLooping;
    }

    public void setLooping(boolean looping) {
        isLooping = looping;
    }

    public float getValue(float time) {
        if (keyPoints.isEmpty()) {
            return 0;
        }

        if (time <= keyPoints.get(0).getTime()) {
            return keyPoints.get(0).getValue();
        } else if (time >= keyPoints.get(keyPoints.size() - 1).getTime()) {
            return keyPoints.get(keyPoints.size() - 1).getValue();
        }

        for (int i = 0; i < keyPoints.size() - 1; i++) {
            KeyPoint kp1 = keyPoints.get(i);
            KeyPoint kp2 = keyPoints.get(i + 1);

            if (time >= kp1.getTime() && time <= kp2.getTime()) {
                float t = (time - kp1.getTime()) / (kp2.getTime() - kp1.getTime());
                float easing = kp1.getEasing();
                float value = interpolate(kp1, kp2, t, easing);
                return value;
            }
        }

        return 0;
    }

    private float interpolate(KeyPoint kp1, KeyPoint kp2, float t, float easing) {
        float value1 = kp1.getValue();
        float value2 = kp2.getValue();

        if (easing == 0.5f) { // linear
            return value1 + t * (value2 - value1);
        } else if (easing < 0.5f) { // ease in
            t = (float) Math.pow(t, 1 / (easing * 2));
            return value1 + t * (value2 - value1);
        } else { // ease out
            t = (float) Math.pow(t, 2 * (1 - easing));
            return value1 + t * (value2 - value1);
        }
    }

    public List<KeyPoint> getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(List<KeyPoint> keyPoints) {
        this.keyPoints = keyPoints;
    }

    public void addKeyPoint(KeyPoint keyPoint) {
        this.keyPoints.add(keyPoint);
    }

    public void addKeyPoint(float time, float value, float easing) {
        this.keyPoints.add(new KeyPoint(time, value, easing));
        lastEasing = easing;
    }

    public void addKeyPoint(float time, float value) {
        this.keyPoints.add(new KeyPoint(time, value, lastEasing));
    }

    public void removeKeyPoint(KeyPoint keyPoint) {
        this.keyPoints.remove(keyPoint);
    }

    public void clearKeyPoints() {
        this.keyPoints.clear();
    }

    public FloatControl getSpeed() {
        return speed;
    }

    public float getPosition() {
        return position;
    }

    public void setPosition(float position) {
        KeyPoint lastKeyPoint = keyPoints.get(keyPoints.size() - 1);
        if (position < 0) {
            position = 0;
        } else if (position > lastKeyPoint.getTime()) {
            position = lastKeyPoint.getTime();
        }
        this.position = position;
    }

    public Automation clone() {
        return new Automation(this.keyPoints);
    }

    public void normalizeTime() {
        float minTime = Float.MAX_VALUE;
        for (KeyPoint keyPoint : keyPoints) {
            if (keyPoint.getTime() < minTime) {
                minTime = keyPoint.getTime();
            }
        }

        float maxTime = Float.MIN_VALUE;
        for (KeyPoint keyPoint : keyPoints) {
            if (keyPoint.getTime() > maxTime) {
                maxTime = keyPoint.getTime();
            }
        }

        float timeRange = maxTime - minTime;

        for (KeyPoint keyPoint : keyPoints) {
            keyPoint.setTime((keyPoint.getTime() - minTime) / timeRange);
        }
    }

    private void automationPlay() {
        isPlaying = true;
        long lastTime = System.nanoTime();

        while (isPlaying) {
            if (isPaused) {
                try {
                    Thread.sleep(10);
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }

            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastTime) / 1_000_000_000f; // в секундах
            lastTime = currentTime;

            float newPosition = position + deltaTime * speed.getValue();
            float maxTime = keyPoints.get(keyPoints.size() - 1).getTime();

            if (newPosition >= maxTime) {
                if (isLooping) {
                    newPosition = 0;
                } else {
                    newPosition = maxTime;
                    isPlaying = false;
                }
            } else if (newPosition < 0) {
                if (isLooping) {
                    newPosition = maxTime;
                } else {
                    newPosition = 0;
                    isPlaying = false;
                }
            }

            position = newPosition;
            float currentValue = getValue(position);

            if (targetControls != null) {
                for (FloatControl control : targetControls) {
                    control.setValue(currentValue);
                }
            }

            try {
                Thread.sleep(10); // ~100 FPS
            } catch (InterruptedException e) {
                break;
            }
        }

        isPlaying = false;
    }

    @Override
    public List<AudioControl> getAllControls() {
        return List.of(speed);
    }
}
