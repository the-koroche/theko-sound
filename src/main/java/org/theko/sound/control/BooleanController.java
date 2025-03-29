package org.theko.sound.control;

public class BooleanController extends AudioController {
    protected boolean value;

    public BooleanController(String name, boolean value) {
        super(name);
        this.value = value;
    }
    
    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("AudioController {Name: %s, Value: %b}", name, value);
    }
}
