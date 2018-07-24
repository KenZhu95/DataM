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

import java.util.Calendar;

public class LogoffActivity extends AppCompatActivity {

    EditText dateEdit;
    EditText timeEdit;
    EditText kmEdit;
    Button buttonEnd;
    Button buttonCancel;

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
        String date = dateEdit.getText().toString();
        String time = dateEdit.getText().toString();
        String dist_km = kmEdit.getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(LogoffActivity.this);
        builder.setTitle("确认对话框");
        builder.setMessage("确认结束测试吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(LogoffActivity.this, "确认结束测试", Toast.LENGTH_LONG).show();
                //LogoffActivity.super.onBackPressed();
                //ActivityManager am = (ActivityManager)getSystemService (Context.ACTIVITY_SERVICE);
                //am.restartPackage(getPackageName());
                //android.os.Process.killProcess(android.os.Process.myPid());
                //System.exit(0);
                MApplication.getInstance().exit();
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
