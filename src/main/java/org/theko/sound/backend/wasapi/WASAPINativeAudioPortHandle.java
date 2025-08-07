package org.theko.sound.backend.wasapi;

/**
 * String identifier representation of a native audio port handle.
 * Used to identify audio ports link type in the WASAPI backend.
 * 
 * @since v2.3.2
 * @author Theko
 */
public class WASAPINativeAudioPortHandle {
    
    private final String handle;

    public WASAPINativeAudioPortHandle(String handle) {
        this.handle = handle;
    }

    public String getHandle() {
        return handle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WASAPINativeAudioPortHandle that = (WASAPINativeAudioPortHandle) o;
        return handle.equals(that.handle);
    }

    @Override
    public String toString() {
        return "WASAPINativeAudioPortHandle{" +
                "handle='" + handle + '\'' +
                '}';
    }
}
