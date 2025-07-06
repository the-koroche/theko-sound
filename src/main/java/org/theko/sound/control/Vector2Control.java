package org.theko.sound.control;

public class Vector2Control extends AudioControl {

    private final FloatControl xControl;
    private final FloatControl yControl;

    public Vector2Control (String name, float xMin, float xMax, float yMin, float yMax) {
        super(name);
        xControl = new FloatControl(name + " X", xMin, xMax, 0.0f);
        yControl = new FloatControl(name + " Y", yMin, yMax, 0.0f);
    }

    public Vector2Control (String name, float min, float max) {
        super(name);
        xControl = new FloatControl(name + " X", min, max, 0.0f);
        yControl = new FloatControl(name + " Y", min, max, 0.0f);
    }

    public Vector2Control (String name) {
        super(name);
        xControl = new FloatControl(name + " X", -1.0f, 1.0f, 0.0f);
        yControl = new FloatControl(name + " Y", -1.0f, 1.0f, 0.0f);
    }
    
    
    public FloatControl getXControl () {
        return xControl;
    }
    
    public FloatControl getYControl () {
        return yControl;
    }

    public float getX () {
        return xControl.getValue();
    }
    
    public float getY () {
        return yControl.getValue();
    }

    public void setX (float x) {
        xControl.setValue(x);
    }
    
    public void setY (float y) {
        yControl.setValue(y);
    }

    @Override
    public String toString () {
        return String.format("Vector2Control {Name: %s, X: %s, Y: %s}", name, xControl, yControl);
    }
}
