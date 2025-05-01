package org.theko.sound.event;

public interface AudioLineListener {
    void onOpen(AudioLineEvent e);
    void onClose(AudioLineEvent e);
    void onFlush(AudioLineEvent e);
    void onDrain(AudioLineEvent e);
    void onStart(AudioLineEvent e);
    void onStop(AudioLineEvent e);
}