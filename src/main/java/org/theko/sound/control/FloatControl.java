package org.theko.sound.control;

public class FloatControl extends AudioControl {
    protected float value;
    protected final float min, max;

    public FloatControl(String name, float min, float max, float value) {
        super(name);
        this.min = min;
        this.max = max;
        this.value = value;
    }

    public void setValue(float value) {
        this.value = Math.clamp(value, min, max);
    }
    
    public float getValue() {
        return value;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getNormalized() {
        return map(value, min, max, 0f, 1f);
    }

    private float map(float x, float inMin, float inMax, float outMin, float outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    @Override
    public String toString() {
        return String.format("FloatController {Name: %s, Value: %.2f, Min: %.2f, Max: %.2f}", name, value, min, max);
    }
}
