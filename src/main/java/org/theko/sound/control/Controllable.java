package org.theko.sound.control;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface Controllable {
    default List<AudioController> getAllControllers() {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            List<AudioController> controllers = new ArrayList<>();
            for (Field field : fields) {
                field.setAccessible(true);
                if (AudioController.class.isAssignableFrom(field.getType())) {
                    AudioController controller = (AudioController) field.get(this);
                    if (controller != null) {
                        controllers.add(controller);
                    }
                }
            }
            return Collections.unmodifiableList(controllers);
        } catch (IllegalAccessException ex) {
            return Collections.unmodifiableList(new ArrayList<>(0));
        }
    }

    default AudioController getController(String name) {
        for (AudioController controller : getAllControllers()) {
            if (controller.getName().equals(name)) {
                return controller;
            }
        }
        return null;
    }

    default FloatController getFloatController(String name) {
        for (AudioController controller : getAllControllers()) {
            if (controller.getName().equals(name) && controller instanceof FloatController) {
                return (FloatController) controller;
            }
        }
        return null;
    }

    default BooleanController getBooleanController(String name) {
        for (AudioController controller : getAllControllers()) {
            if (controller.getName().equals(name) && controller instanceof BooleanController) {
                return (BooleanController) controller;
            }
        }
        return null;
    }
}
