package org.theko.sound.event;

public class DataLineAdapter implements DataLineListener {
    @Override public void onSend(DataLineEvent e) { }
    @Override public void onSendTimeout(DataLineEvent e) { }
    @Override public void onReceive(DataLineEvent e) { }
    @Override public void onReceiveTimeout(DataLineEvent e) { }
}