package com.example.ken_z.datam;

public class GuideEvent {
    private int[] msg;
    private int[] alert_msg;
    private double[] gps_msg;
    public GuideEvent(int[] msg_, int[] alert_msg_, double[] gps_msg_) {
        msg = msg_;
        alert_msg = alert_msg_;
        gps_msg = gps_msg_;
    }
    public int[] getMsg() {
        return msg;
    }
    public int[] getAlertMsg() {return alert_msg; }
    public double[] getGpsMsg() { return gps_msg; }
}
