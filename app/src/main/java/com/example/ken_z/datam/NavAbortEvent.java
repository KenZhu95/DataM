package com.example.ken_z.datam;

public class NavAbortEvent {
    private boolean msg_abort;
    public NavAbortEvent(boolean abort) {
        this.msg_abort = abort;
    }

    public boolean isMsg_abort() {
        return msg_abort;
    }
}
