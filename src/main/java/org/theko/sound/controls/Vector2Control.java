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

package org.theko.sound.controls;

import org.theko.events.ListenerPriority;
import org.theko.sound.events.AudioControlEvent;
import org.theko.sound.events.AudioControlEventType;
import org.theko.sound.events.AudioControlListener;

public class Vector2Control extends AudioControl {

    private final FloatControl xControl;
    private final FloatControl yControl;

    private final AudioControlListener valueChangeListener = new AudioControlListener() {
        @Override
        public void onValueChanged(AudioControlEvent event) {
            eventDispatcher.dispatch(AudioControlEventType.VALUE_CHANGED, new AudioControlEvent(Vector2Control.this));
        }
    };

    public Vector2Control(String name, float xMin, float xMax, float yMin, float yMax) {
        super(name);
        xControl = new FloatControl(name + " X", xMin, xMax, 0.0f);
        yControl = new FloatControl(name + " Y", yMin, yMax, 0.0f);
        xControl.getListenersManager().addListener(ListenerPriority.HIGHEST, valueChangeListener);
        yControl.getListenersManager().addListener(ListenerPriority.HIGHEST, valueChangeListener);
    }

    public Vector2Control(String name, float min, float max) {
        this(name, min, max, min, max);
    }

    public Vector2Control(String name) {
        this(name, -1.0f, 1.0f);
    }
    
    
    public FloatControl getXControl() {
        return xControl;
    }
    
    public FloatControl getYControl() {
        return yControl;
    }

    public float getX() {
        return xControl.getValue();
    }
    
    public float getY() {
        return yControl.getValue();
    }

    public void setX(float x) {
        xControl.setValue(x);
    }
    
    public void setY(float y) {
        yControl.setValue(y);
    }

    @Override
    public String toString() {
        return String.format("Vector2Control{Name: %s, X: %s, Y: %s}", name, xControl, yControl);
    }
}
