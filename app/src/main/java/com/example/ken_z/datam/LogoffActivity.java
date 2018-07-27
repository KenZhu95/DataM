package com.example.ken_z.datam;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;

import de.greenrobot.event.EventBus;

public class LogoffActivity extends AppCompatActivity {
    private static final String APP_IP = "192.168.8.99";
    private static final int MAXNUM = 10;
    private static final int SERVER_RECEIVE_PORT = 9992;

    EditText dateEdit;
    EditText timeEdit;
    EditText kmEdit;
    Button buttonEnd;
    Button buttonCancel;
    DatagramSocket s_socket_stop;
    int tries_stop = 0;
    int[] stopDates = new int[3];
    int[] stopTimes = new int[3];
    boolean listenStatus = true;
    boolean ifToSendStop = false;
    boolean ifAckStop = false;
    int distanceKM = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logoff);
        MApplication.getInstance().addActivity(this);
        dateEdit = findViewById(R.id.edit_date_off);
        timeEdit = findViewById(R.id.edit_time_off);
        kmEdit = findViewById(R.id.edit_km_off);
        buttonEnd = findViewById(R.id.button_end_test);
        buttonCancel = findViewById(R.id.button_cancel_test_off);
        dateEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        timeEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        buttonEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endTest();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelTest();
            }
        });

        new UdpStopThread().start();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int curYear = calendar.get(Calendar.YEAR);
        int curMonth = calendar.get(Calendar.MONTH);
        int curDay = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Log.i("test", "当前日期: " + year + "-" + month + "-" + dayOfMonth);
                dateEdit.setText(year + "-" + month + "-" + dayOfMonth);
            }
        }, curYear, curMonth, curDay);
        //datePickerDialog.set
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int curHour = calendar.get(Calendar.HOUR_OF_DAY);
        int curMinute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                timeEdit.setText(hourOfDay + ":" + minute);
            }
        }, curHour, curMinute, false);
        timePickerDialog.show();
    }

    private void endTest() {
        final String date = dateEdit.getText().toString();
        final String time = dateEdit.getText().toString();
        final String dist_km = kmEdit.getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(LogoffActivity.this);
        builder.setTitle("确认对话框");
        builder.setMessage("确认结束测试吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(LogoffActivity.this, "确认结束测试", Toast.LENGTH_LONG).show();
                postStop(date, time, dist_km);
                //MApplication.getInstance().exit();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();

    }


    private void postStop(String date, String time, String dist) {
        String[] sDates = date.split("-");
        String[] sTimes = time.split(":");
        try {
            int year = Integer.parseInt(sDates[0]);
            int month = Integer.parseInt(sDates[1]);
            int day = Integer.parseInt(sDates[2]);

            int hour = Integer.parseInt(sTimes[0]);
            int minute = Integer.parseInt(sTimes[1]);

            stopDates = new int[]{year, month, day};
            stopTimes = new int[]{hour, minute, 0};
            distanceKM = Integer.parseInt(dist);
            ifToSendStop = true;
            tries_stop = 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelTest() {
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        return;
    }


    public class UdpStopThread extends Thread {
        @Override
        public void run() {
            try {
                String ipValidation = Validation.validateIP(APP_IP);
                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_stop = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK","pandora");
                send_object.put("LEN","60000");
                send_object.put("ETT",43);
                JSONObject send_time = new JSONObject();


                while (listenStatus) {
                    while (ifToSendStop && tries_stop < MAXNUM) {
                        send_time.put("YEAR", stopDates[0]);
                        send_time.put("MON", stopDates[1]);
                        send_time.put("DAY", stopDates[2]);
                        send_time.put("HOUR", stopTimes[0]);
                        send_time.put("MIN", stopTimes[1]);
                        send_time.put("SEC", stopTimes[2]);
                        send_object.put("TIME", send_time);
                        send_object.put("VIN", MApplication.getInstance().getVIN());
                        send_object.put("STM", distanceKM);

                        int len = send_object.toString().getBytes().length;
                        String lenString = String.format("%05d", len);
                        send_object.put("LEN", lenString);

                        String send_content = send_object.toString();
                        DatagramPacket dp_send_start = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);

                        s_socket_stop.send(dp_send_start);
                        tries_stop++;
                        //send an start info package every 3 seconds
                        try {
                            Thread.sleep(3 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (tries_stop >= MAXNUM) {
                        Toast.makeText(LogoffActivity.this, "结束测试信息发送失败", Toast.LENGTH_LONG).show();
                    }
                    if (ifAckStop) {
                        try {
                            Thread.sleep(3 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(LogoffActivity.this, "测试结束信息发送成功", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.d("AndroidUDP", e.getMessage());
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StopAckEvent event) {
        if (event !=null) {
            boolean ack =event.isMsg_ack();
            if (ack) {
                ifToSendStop = false;
                tries_stop = 0;
                Toast.makeText(LogoffActivity.this, "成功结束测试", Toast.LENGTH_LONG).show();
                ifAckStop = true;

            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}
