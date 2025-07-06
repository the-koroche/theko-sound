package org.theko.sound.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.theko.sound.AudioNode;
import org.theko.sound.control.AudioControl;
import org.theko.sound.control.BooleanControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;

public abstract class AudioEffect implements AudioNode, Controllable {

    protected final Type type;
    protected final FloatControl mixLevel = new FloatControl("Mix Level", 0.0f, 1.0f, 1.0f);
    protected final BooleanControl enable = new BooleanControl("Enable", true);

    protected final List<AudioControl> mixingControls = List.of(mixLevel, enable);
    protected final List<AudioControl> allControls = new ArrayList<>(mixingControls);

    public enum Type {
        REALTIME, OFFLINE_PROCESSING
    }

    public AudioEffect (Type type) {
        this.type = Objects.requireNonNull(type);
    }

    public Type getType () {
        return type;
    }

    public FloatControl getMixLevelControl () {
        return mixLevel;
    }

    public BooleanControl getEnableControl () {
        return enable;
    }

    @Override
    public List<AudioControl> getAllControls () {
        return allControls;
    }
}
