package com.example.ken_z.datam;

import java.util.List;

public class SendEvent {
    private int[] msg;
    private int[] alert_msg;
    public SendEvent(int[] msg_, int[] alert_msg_) {
        msg = msg_;
        alert_msg = alert_msg_;
    }
    public int[] getMsg() {
        return msg;
    }
    public int[] getAlertMsg() {return alert_msg; }
}
