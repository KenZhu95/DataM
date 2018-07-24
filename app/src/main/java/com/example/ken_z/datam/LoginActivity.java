package com.example.ken_z.datam;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

public class LoginActivity extends AppCompatActivity {
    private static final String APP_IP = "192.168.8.99";
    private static final int SERVER_RECEIVE_PORT = 9992;
    private static final int MAXNUM = 10;

    EditText dateEdit;
    EditText timeEdit;
    EditText kmEdit;
    Button buttonStart;
    Button buttonCancel;

    DatagramSocket s_socket_start;
    int tries_start = 0;
    int[] startDates = new int[3];
    int[] startTimes = new int[3];
    boolean listenStatus = true;
    boolean ifToSendStart = false;
    boolean ifAckStart = false;
    int distanceKM = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        MApplication.getInstance().addActivity(this);
        dateEdit = (EditText) findViewById(R.id.edit_date);
        timeEdit = (EditText) findViewById(R.id.edit_time);
        kmEdit = (EditText) findViewById(R.id.edit_km);
        buttonStart = (Button) findViewById(R.id.button_start_test);
        buttonCancel = (Button) findViewById(R.id.button_cancel_test);
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

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelTest();
            }
        });

        new UdpStartThread().start();
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

    private void startTest() {
        final String date = dateEdit.getText().toString();
        final String time = dateEdit.getText().toString();
        final String dist_km = kmEdit.getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("确认对话框");
        builder.setMessage("确认开始测试吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                postStart(date, time, dist_km);
                //Toast.makeText(LoginActivity.this, "确认开始测试", Toast.LENGTH_LONG).show();
                //LoginActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();

    }

    private void postStart(String date, String time, String dist) {
        String[] sDates = date.split("-");
        String[] sTimes = time.split(":");
        try {
            int year = Integer.parseInt(sDates[0]);
            int month = Integer.parseInt(sDates[1]);
            int day = Integer.parseInt(sDates[2]);

            int hour = Integer.parseInt(sTimes[0]);
            int minute = Integer.parseInt(sTimes[1]);

            startDates = new int[]{year, month, day};
            startTimes = new int[]{hour, minute, 0};
            distanceKM = Integer.parseInt(dist);
            ifToSendStart = true;
            tries_start = 0;

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


    public class UdpStartThread extends Thread {
        @Override
        public void run() {
            try {
                String ipValidation = Validation.validateIP(APP_IP);
                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_start = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK","pandora");
                send_object.put("LEN","60000");
                send_object.put("ETT",41);
                JSONObject send_time = new JSONObject();


                while (listenStatus) {
                    while (ifToSendStart && tries_start < MAXNUM) {
                        send_time.put("YEAR", startDates[0]);
                        send_time.put("MON", startDates[1]);
                        send_time.put("DAY", startDates[2]);
                        send_time.put("HOUR", startTimes[0]);
                        send_time.put("MIN", startTimes[1]);
                        send_time.put("SEC", startTimes[2]);
                        send_object.put("TIME", send_time);
                        send_object.put("VIN", MApplication.getInstance().getVIN());
                        send_object.put("STM", distanceKM);

                        int len = send_object.toString().getBytes().length;
                        String lenString = String.format("%05d", len);
                        send_object.put("LEN", lenString);

                        String send_content = send_object.toString();
                        DatagramPacket dp_send_start = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);

                        s_socket_start.send(dp_send_start);
                        tries_start++;
                        //send an start info package every 3 seconds
                        try {
                            Thread.sleep(3 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (tries_start >= MAXNUM) {
                        Toast.makeText(LoginActivity.this, "开始测试信息发送失败", Toast.LENGTH_LONG).show();
                    }
                    if (ifAckStart) {
                        LoginActivity.super.onBackPressed();
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
    public void onEventMainThread(StartAckEvent event) {
        if (event !=null) {
            boolean ack =event.isMsg_ack();
            if (ack) {
                ifToSendStart = false;
                tries_start = 0;
                Toast.makeText(LoginActivity.this, "成功开始测试", Toast.LENGTH_LONG).show();
                ifAckStart = true;

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
