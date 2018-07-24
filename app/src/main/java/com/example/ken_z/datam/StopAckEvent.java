package com.example.ken_z.datam;

public class StopAckEvent {
    private boolean msg_ack;
    public StopAckEvent(boolean ack) {
        this.msg_ack = ack;
    }

    public boolean isMsg_ack() {
        return msg_ack;
    }
}
