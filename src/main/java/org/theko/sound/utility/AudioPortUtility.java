package org.theko.sound.utility;

import java.util.HashMap;

import javax.sound.sampled.Mixer;

import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.javasound.JavaSoundBackend;
import org.theko.sound.backend.wasapi.WASAPINativeAudioPortHandle;
import org.theko.sound.backend.wasapi.WASAPISharedBackend;

/**
 * Utility class to map audio port link types to audio backends.
 * 
 * @since v2.3.2
 * @author Theko
 */
public class AudioPortUtility {

    private static final HashMap<Class<?>, Class<? extends AudioBackend>> backends = new HashMap<>();

    static {
        backends.put(Mixer.Info.class, JavaSoundBackend.class);
        backends.put(WASAPINativeAudioPortHandle.class, WASAPISharedBackend.class);
    }
    
    private AudioPortUtility() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static Class<? extends AudioBackend> getBackendByLinkType(Class<?> link) {
        if (link == null) {
            return null;
        }
        for (Class<?> key : backends.keySet()) {
            if (key.isAssignableFrom(link)) {
                return backends.get(key);
            }
        }
        return null;
    }
}
