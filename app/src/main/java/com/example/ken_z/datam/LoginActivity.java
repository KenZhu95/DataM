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

import java.util.Calendar;

import de.greenrobot.event.EventBus;

public class LoginActivity extends AppCompatActivity {
    EditText dateEdit;
    EditText timeEdit;
    EditText kmEdit;
    Button buttonStart;
    Button buttonCancel;

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
        String date = dateEdit.getText().toString();
        String time = dateEdit.getText().toString();
        String dist_km = kmEdit.getText().toString();

        final StartEvent startEvent = new StartEvent(date, time, dist_km);
        EventBus.getDefault().post(startEvent);
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("确认对话框");
        builder.setMessage("确认开始测试吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EventBus.getDefault().post(startEvent);
                Toast.makeText(LoginActivity.this, "确认开始测试", Toast.LENGTH_LONG).show();
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

    private void cancelTest() {
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        return;
    }
}
