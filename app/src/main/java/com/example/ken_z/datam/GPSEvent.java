package com.example.ken_z.datam;

public class GPSEvent {
    private double[] msg_long_;
    private double[] msg_lati_;
    public GPSEvent(double[] msg_lati, double[] msg_long) {
        msg_lati_ = msg_lati;
        msg_long_ = msg_long;
    }
    public double[] getMsgLati() {
        return this.msg_lati_;
    }
    public double[] getMsglong() {
        return this.msg_long_;
    }
}
