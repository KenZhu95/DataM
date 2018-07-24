package com.example.ken_z.datam;

import android.app.Activity;
import android.app.Application;

import com.baidu.mapapi.SDKInitializer;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

import java.util.LinkedList;
import java.util.List;

public class MApplication extends Application {
    private static MApplication instance;
    private List<Activity> activityList = new LinkedList();
    private String vehicle_number;
    private int audio_index = 0;
    @Override
    public void onCreate() {

        instance = this;
        SDKInitializer.initialize(getApplicationContext());
        SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID + "=" + getString(R.string.speech_app_id));
        super.onCreate();
    }

    public static MApplication getInstance() {
        return instance;
    }

    public void exit() {
        for (Activity act : activityList) {
            act.finish();
        }
        System.exit(0);
    }

    public void addActivity(Activity act) {
        activityList.add(act);
    }
    public void setVIN(String vin) {
        this.vehicle_number = vin;
    }
    public String getVIN() {
        return this.vehicle_number;
    }
    public int getAIN() {
        return this.audio_index;
    }
    public void newAudio() {
        this.audio_index++;
    }
}
