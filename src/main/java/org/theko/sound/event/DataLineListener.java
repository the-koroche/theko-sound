package org.theko.sound.event;

public interface DataLineListener {
    void onSend(DataLineEvent e);
    void onSendTimeout(DataLineEvent e);
    void onReceive(DataLineEvent e);
    void onReceiveTimeout(DataLineEvent e);
}