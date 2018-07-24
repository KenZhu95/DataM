package com.example.ken_z.datam;

public class AlertEvent {
    private int[] msg;
    public AlertEvent(int[] msg) {
        msg = msg;
    }
    public int[] getAlertMsg() {
        return this.msg;
    }
}
