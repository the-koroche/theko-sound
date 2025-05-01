package org.theko.sound.event;

public interface AudioOutputLineListener extends AudioLineListener {
    void onWrite(AudioLineEvent e);
}