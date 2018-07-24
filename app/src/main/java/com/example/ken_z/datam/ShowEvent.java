package com.example.ken_z.datam;

public class ShowEvent {
    private int[] msg_int;
    private int[] alert_msg;

    public ShowEvent(int[] msg_int_, int[] alert_msg_) {
        msg_int = msg_int_;
        alert_msg = alert_msg_;

    }
    public int[] getMsgInt() {
        return msg_int;
    }
    public int[] getAlertMsg() { return alert_msg; }

}
