package com.example.ken_z.datam;

public class SpeechAckEvent {
    private boolean msg_ack;
    public SpeechAckEvent(boolean ack) {
        this.msg_ack = ack;
    }

    public boolean isMsg_ack() {
        return msg_ack;
    }
}
