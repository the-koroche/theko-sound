package org.theko.sound.control;

public class Vector3Control extends AudioControl {

    private final FloatControl xControl;
    private final FloatControl yControl;
    private final FloatControl zControl;

    public Vector3Control (String name, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        super(name);
        xControl = new FloatControl(name + " X", xMin, xMax, 0.0f);
        yControl = new FloatControl(name + " Y", yMin, yMax, 0.0f);
        zControl = new FloatControl(name + " Z", zMin, zMax, 0.0f);
    }

    public Vector3Control (String name, float min, float max) {
        super(name);
        xControl = new FloatControl(name + " X", min, max, 0.0f);
        yControl = new FloatControl(name + " Y", min, max, 0.0f);
        zControl = new FloatControl(name + " Z", min, max, 0.0f);
    }

    public Vector3Control (String name) {
        super(name);
        xControl = new FloatControl(name + " X", -1.0f, 1.0f, 0.0f);
        yControl = new FloatControl(name + " Y", -1.0f, 1.0f, 0.0f);
        zControl = new FloatControl(name + " Z", -1.0f, 1.0f, 0.0f);
    }
    
    public FloatControl getXControl () {
        return xControl;
    }
    
    public FloatControl getYControl () {
        return yControl;
    }
    
    public FloatControl getZControl () {
        return zControl;
    }
    
    public float getX () {
        return xControl.getValue();
    }
    
    public float getY () {
        return yControl.getValue();
    }
    
    public float getZ () {
        return zControl.getValue();
    }

    public void setX (float x) {
        xControl.setValue(x);
    }

    public void setY (float y) {
        yControl.setValue(y);
    }

    public void setZ (float z) {
        zControl.setValue(z);
    }

    @Override
    public String toString () {
        return String.format("Vector3Control {Name: %s, X: %s, Y: %s, Z: %s}", name, xControl, yControl, zControl);
    }
}
