package com.example.ken_z.datam;

public class StopEvent {
    private String msg_date;
    private String msg_time;
    private String msg_km;

    public StopEvent(String date, String time, String km) {
        msg_date = date;
        msg_time = time;
        msg_km = km;
    }

    public String getMsg_date() {
        return msg_date;
    }

    public String getMsg_time() {
        return msg_time;
    }

    public String getMsg_km() {
        return msg_km;
    }
}
