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

package org.theko.sound.control;

import org.theko.sound.event.AudioControlEvent;
import org.theko.sound.event.AudioControlListener;

public class Vector3Control extends AudioControl {

    private final FloatControl xControl;
    private final FloatControl yControl;
    private final FloatControl zControl;

    private final AudioControlListener valueChangeListener = new AudioControlListener() {
        @Override
        public void onValueChanged(AudioControlEvent event) {
            eventDispatcher.dispatch(AudioControlNotifyType.VALUE_CHANGE, new AudioControlEvent(Vector3Control.this));
        }
    };

    public Vector3Control(String name, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        super(name);
        xControl = new FloatControl(name + " X", xMin, xMax, 0.0f);
        yControl = new FloatControl(name + " Y", yMin, yMax, 0.0f);
        zControl = new FloatControl(name + " Z", zMin, zMax, 0.0f);
        xControl.addListener(valueChangeListener);
        yControl.addListener(valueChangeListener);
        zControl.addListener(valueChangeListener);
    }

    public Vector3Control(String name, float min, float max) {
        this(name, min, max, min, max, min, max);
    }

    public Vector3Control(String name) {
        this(name, -1.0f, 1.0f);
    }
    
    public FloatControl getXControl() {
        return xControl;
    }
    
    public FloatControl getYControl() {
        return yControl;
    }
    
    public FloatControl getZControl() {
        return zControl;
    }
    
    public float getX() {
        return xControl.getValue();
    }
    
    public float getY() {
        return yControl.getValue();
    }
    
    public float getZ() {
        return zControl.getValue();
    }

    public void setX(float x) {
        xControl.setValue(x);
    }

    public void setY(float y) {
        yControl.setValue(y);
    }

    public void setZ(float z) {
        zControl.setValue(z);
    }

    @Override
    public String toString() {
        return String.format("Vector3Control{Name: %s, X: %s, Y: %s, Z: %s}", name, xControl, yControl, zControl);
    }
}
