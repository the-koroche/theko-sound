package org.theko.sound.event;

public interface AudioInputLineListener extends AudioLineListener {
    void onRead(AudioLineEvent e);
}