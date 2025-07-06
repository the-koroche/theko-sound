package org.theko.sound.event;

public class SoundSourceEventAdapter implements SoundSourceEventListener {

    @Override public void onOpened (SoundSourceEvent e) { }
    @Override public void onClosed (SoundSourceEvent e) { }
    @Override public void onPlaybackStarted (SoundSourceEvent e) { }
    @Override public void onPlaybackStoped (SoundSourceEvent e) { }
    @Override public void onLoop (SoundSourceEvent e) { }
    @Override public void onSpeedChanged (SoundSourceEvent e) { }
    @Override public void onPositionChanged (SoundSourceEvent e) { }
    @Override public void onVolumeChanged (SoundSourceEvent e) { }
    @Override public void onPanChanged (SoundSourceEvent e) { }
}
