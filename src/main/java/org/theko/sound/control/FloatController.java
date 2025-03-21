package org.theko.sound.control;

public class FloatController extends AudioController {
    protected float value;
    protected final float min, max;

    public FloatController(String name, float min, float max, float value) {
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

    @Override
    public String toString() {
        return String.format("FloatController {%s, %f.2}", name, value);
    }
}
