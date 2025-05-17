package org.theko.sound.event;

public interface SoundSourceEventListener {
    void onOpened(SoundSourceEvent e);
    void onClosed(SoundSourceEvent e);
    void onPlaybackStarted(SoundSourceEvent e);
    void onPlaybackStoped(SoundSourceEvent e);
    void onLoop(SoundSourceEvent e);
    void onSpeedChanged(SoundSourceEvent e);
    void onPositionChanged(SoundSourceEvent e);
    void onVolumeChanged(SoundSourceEvent e);
    void onPanChanged(SoundSourceEvent e);
}
