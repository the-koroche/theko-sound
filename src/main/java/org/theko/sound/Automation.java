package org.theko.sound;

import java.util.ArrayList;
import java.util.List;

import org.theko.sound.control.AudioController;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatController;

public class Automation implements AudioObject, Controllable {
    private List<KeyPoint> keyPoints;
    private FloatController speed;
    private float position;
    private boolean isLooping = false;
    private boolean isPaused = false;
    private boolean isPlaying = false;
    private transient Thread playThread;

    private static int automationInstances;
    private static final int thisAutomationInstance = ++automationInstances;

    private List<FloatController> targetControllers;

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
        this.speed = new FloatController("Speed", -4, 4, 1f);
        this.position = 0;
        this.playThread = new Thread(this::automationPlay, "Automation Play-" + thisAutomationInstance);
        this.playThread.setPriority(Thread.NORM_PRIORITY - 3); // Set to minimum priority
        this.playThread.setName("AutomationPlayThread");
        this.playThread.setDaemon(true); // Set as daemon thread
    }

    public Automation() {
        this(new ArrayList<>());
    }

    public void addTargetController(FloatController controller) {
        if (targetControllers == null) {
            targetControllers = new ArrayList<>();
        }
        targetControllers.add(controller);
    }

    public void removeTargetController(FloatController controller) {
        if (targetControllers != null) {
            targetControllers.remove(controller);
        }
    }

    public void clearTargetControllers() {
        if (targetControllers != null) {
            targetControllers.clear();
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
            t = (float) Math.pow(t, easing * 2);
            return value1 + t * (value2 - value1);
        } else { // ease out
            t = 1 - (float) Math.pow(1 - t, easing * 2);
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

    public void removeKeyPoint(KeyPoint keyPoint) {
        this.keyPoints.remove(keyPoint);
    }

    public void clearKeyPoints() {
        this.keyPoints.clear();
    }

    public FloatController getSpeed() {
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

            if (targetControllers != null) {
                for (FloatController controller : targetControllers) {
                    controller.setValue(currentValue);
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
    public List<AudioController> getAllControllers() {
        return List.of(speed);
    }
}
