package com.example.ken_z.datam;

public class SpeechEvent {
    private String msg_trans;
    private String msg_file;
    private int[] msg_dates;
    private int[] msg_times;

    public SpeechEvent(String msg_trans, String msg_file, int[] msg_dates, int[] msg_times) {
        this.msg_file = msg_file;
        this.msg_trans = msg_trans;
        this.msg_dates = msg_dates;
        this.msg_times = msg_times;
    }

    public String getMsg_file() {
        return msg_file;
    }

    public String getMsg_trans() {
        return msg_trans;
    }

    public int[] getMsg_dates() {
        return msg_dates;
    }

    public int[] getMsg_times() {
        return msg_times;
    }
}
