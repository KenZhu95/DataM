package com.example.ken_z.datam;

public class StartAckEvent {
    private boolean msg_ack;
    public StartAckEvent(boolean ack) {
        this.msg_ack = ack;
    }

    public boolean isMsg_ack() {
        return msg_ack;
    }
}
