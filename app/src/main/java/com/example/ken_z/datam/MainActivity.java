package com.example.ken_z.datam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.widget.Button;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRouteGuideManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

import static com.example.ken_z.datam.BeeAndVibrateManager.playBeeAndVibrate;
import static com.example.ken_z.datam.BeeAndVibrateManager.playWarnAndVibrate;
import static com.example.ken_z.datam.MapActivity.ROUTE_PLAN_NODE;
import static com.example.ken_z.datam.MapActivity.ROUTE_PLAN_NODES;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {
    private static final String TAG = MainActivity.class.getName();

    //private static final String APP_IP = "10.110.110.188";
    private static final String APP_IP = "192.168.8.99";
    private static final int APP_PORT = 9980;

    private static final int MAXNUM = 10;
    private static final int REVNUM = 12;
    private static final int TIMEOUT = 10000;
    private static final String GUIDE_APP_NAME = "com.example.ken_z.datag";


    private Button button1, button2, button3, button4, button5, button6, button7, button8, button9;
    private Button[] buttons;
    private Button buttonPermission;
    private Button buttonLogin, buttonSpeech;
    private RelativeLayout relativeLayout;
    private GestureDetector gestureDetector;

    private boolean listenStatus = true;
    private boolean ifToSendAudio = false;
    private boolean ifToSendStart = false;
    private boolean ifToSendStop = false;
    private DatagramSocket s_socket;
    private DatagramSocket r_socket;
    private DatagramSocket s_socket_heart;
    private DatagramSocket s_socket_start;
    private DatagramSocket s_socket_stop;
    public int tries_start = 0;
    public int tries_stop = 0;

    private int[] startDate = new int[3];
    private int[] startTime = new int[3];
    private int[] stopDate = new int[3];
    private int[] stopTime = new int[3];

    private ReceiveHandler receiveHandler = new ReceiveHandler();

    //guide
    private BNRoutePlanNode mBNRoutePlanNode = null;
    private List<BNRoutePlanNode> mBNRoutePlanNodes = new ArrayList<>();
    private BNRoutePlanNode mStartNode = null;

    //private IBNRouteGuideManager mRouteGuideManager;

    //to launch another guide app

    // for count down alert dialog
    private TextView mOffTextView;
    private Handler mOffHandler;
    private Timer mOffTimer;
    private Dialog mDialog;

    private double[] latitudes;
    private double[] longitudes;

    private RandomAccessFile accessFile = null;
    private byte[] translationData;

    class ReceiveHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("AndroidUDP", "Received");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SDKInitializer.initialize(getApplicationContext());

        setContentView(R.layout.activity_main);
        MApplication.getInstance().addActivity(this);
        Log.d("AndroidUDP", "Start.");

        relativeLayout = findViewById(R.id.relative_main);

        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);
        button5 = findViewById(R.id.button5);
        button6 = findViewById(R.id.button6);
        button7 = findViewById(R.id.button7);
        button8 = findViewById(R.id.button8);
        button9 = findViewById(R.id.button9);

        buttonPermission = findViewById(R.id.button_permission);
        buttonLogin =  findViewById(R.id.button_login);
        buttonSpeech = findViewById(R.id.button_speech);

        relativeLayout.setOnTouchListener(this);
        relativeLayout.setLongClickable(true);
        gestureDetector = new GestureDetector((GestureDetector.OnGestureListener)this);

        //UDP threads: first for data receive, others for data send

        new UdpReceiveThread().start();
        new UdpHeartBeatThread().start();
        new UdpStartThread().start();
        new UdpStopThread().start();

        //gary  #D4D4D4
        //green #7FFF00
        //red   #FF4500


        buttonPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocation();
            }
        });
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLogin();
            }
        });
        buttonSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeech();
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createDialog();
            }
        });

    }


    private void startLocation() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[]
                            {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},0x0010);
                }

                int permission1 = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
                int permission2 = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

                if(permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startSpeech() {
        Intent intent = new Intent(MainActivity.this, SpeechActivity.class);
        startActivity(intent);
    }

    private void startLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TTT", "Access GPS");
                } else {
                    Log.d("TTTT", "Access Denied");
                } break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        listenStatus = false;
        //close all UDP sockets
        r_socket.close();
        s_socket.close();
        s_socket_heart.close();
        s_socket_start.close();
        s_socket_stop.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        //Event Bus unregister
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Event Bus register
        EventBus.getDefault().register(this);

    }


    public class UdpReceiveThread extends Thread {
        @Override
        public void run() {
            byte[] buf = new byte[1024];

            try {
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);

                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket = new DatagramSocket();
                r_socket = new DatagramSocket(APP_PORT);

                JSONObject send_object = new JSONObject();
                send_object.put("CHK", "pandora");
                send_object.put("ETT", 10);
                send_object.put("LEN", "999999");
//                JSONObject com_object = new JSONObject();
//                com_object.put("REG", 1);
//                com_object.put("BRK", 0);
//                send_object.put("COM", com_object);
                String send_content = send_object.toString();

                DatagramPacket dp_send = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, 9992);
                DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
                r_socket.setSoTimeout(TIMEOUT);
                int tries = 0;
                boolean receivedResponse = false;
                while (!receivedResponse && tries < MAXNUM) {
                    Log.d("AndroidUDP", "Wait send.");
                    try {
                        s_socket.send(dp_send);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("AndroidUDP", "" + e.getMessage());
                    }
//                    s_socket.close();
                    Log.d("AndroidUDP", "Send Check.");

                    try {
                        r_socket.receive(dp_receive);
                        if (!dp_receive.getAddress().equals(APP_ADD)) {
                            throw new IOException("Received packet from an unknown source");
                        }
                        receivedResponse = true;
                        Log.d("AndroidUDP", "Received.");
                    } catch (Exception e) {
                        tries += 1;
                        Log.d("AndroidUDP", "Time out," + (MAXNUM - tries) + " more tries...");
                    }

                    if (receivedResponse) {
                        while (listenStatus) {
                            r_socket.receive(dp_receive);
                            dp_receive.getData();

                            String rev_log_ = new String(dp_receive.getData(), 0, dp_receive.getLength());

                            JSONObject jsonObject_;

                            jsonObject_ = new JSONObject(rev_log_);
                            try {
                                String chk = jsonObject_.getString("CHK");
                                int ETT = jsonObject_.getInt(" ETT");
                                String LEN = jsonObject_.getString("LEN");
                                try {
                                    int len = Integer.parseInt(LEN);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (chk.equals("pandora")) {
                                    switch (ETT) {
                                        case 20 :
                                            try {

                                                JSONObject jsonObjectTIME = jsonObject_.getJSONObject("TIME"); //time info
                                                JSONObject jsonObjectHMI = jsonObject_.getJSONObject("HMI"); //BMi info
                                                JSONObject jsonObjectSTS = jsonObject_.getJSONObject("STS"); //states info
                                                JSONObject jsonObjectPOS = jsonObject_.getJSONObject("POS"); //position (GPS) info
                                                JSONObject jsonObjectALT = jsonObject_.getJSONObject("ALT"); // alert info

                                                //time info
                                                int year = jsonObjectTIME.getInt("YEAR");
                                                int month = jsonObjectTIME.getInt("MON");
                                                int day = jsonObjectTIME.getInt("DAY");
                                                int hour = jsonObjectTIME.getInt("HOUR");
                                                int minute = jsonObjectTIME.getInt("MIN");
                                                int second = jsonObjectTIME.getInt("SEC");

                                                //HMI info
                                                JSONObject jsonObjectHLB = jsonObjectHMI.getJSONObject("HLB");
                                                JSONObject jsonObjectACC = jsonObjectHMI.getJSONObject("ACC");
                                                //HLB info
                                                int HDM = jsonObjectHLB.getInt("HDM");
                                                int LDWL = jsonObjectHLB.getInt("LDWL");
                                                int LDWR = jsonObjectHLB.getInt("LDWR");
                                                int BSDL = jsonObjectHLB.getInt("BSDL");
                                                int BSDR = jsonObjectHLB.getInt("BSDR");
                                                //ACC info
                                                int DIS = jsonObjectACC.getInt("DIS");
                                                int GAP = jsonObjectACC.getInt("GAP");
                                                int TGT = jsonObjectACC.getInt("TGT");
                                                int APL = jsonObjectACC.getInt("APL");
                                                int BKP = jsonObjectACC.getInt("BKP");
                                                int TAG = jsonObjectACC.getInt("ATV"); //validation of DIS index number (1: valid-white 0:invalid-grey)
                                                //int TAG = 1;
                                                //STS info
                                                int VIDEO = jsonObjectSTS.getInt("VIDEO");
                                                int ETH = jsonObjectSTS.getInt("ETH");
                                                int CAN = jsonObjectSTS.getInt("CAN");
                                                int GPS = jsonObjectSTS.getInt("GPS");
                                                int SVC = jsonObjectSTS.getInt("SVC");
                                                int LIDAR = jsonObjectSTS.getInt("LIDAR");
                                                int AFE = jsonObjectSTS.getInt("AFE");
                                                int AIO = jsonObjectSTS.getInt("AIO");
                                                int EQM = jsonObjectSTS.getInt("EQM");

                                                //Position info
                                                double LON = jsonObjectPOS.getDouble("LON");
                                                double LAT = jsonObjectPOS.getDouble("LAT");

                                                //alert info
                                                int LVL = jsonObjectALT.getInt("LVL");
                                                int CNT = jsonObjectALT.getInt("CNT");

                                                int[] post_show = new int[]{HDM, LDWL, LDWR, BSDL, BSDR, DIS, GAP, TGT, APL, BKP, TAG};
                                                int[] post_alert = new int[]{LVL, CNT};
                                                double[] post_gps = new double[]{LON, LAT};
                                                int[] post_time = new int[]{year, month, day, hour, minute, second};

                                                EventBus.getDefault().post(new SendEvent(new int[]{VIDEO, ETH, CAN, GPS, SVC, LIDAR, AFE, AIO, EQM}, post_alert));
                                                EventBus.getDefault().post(new GuideEvent(new int[]{VIDEO, ETH, CAN, GPS, SVC, LIDAR, AFE, AIO, EQM}, post_alert, post_gps));
                                                EventBus.getDefault().post(new ShowEvent(post_show, post_alert));
                                                //EventBus.getDefault().post(new AlertEvent(post_alert));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case 21:
                                            try {
                                                JSONObject jsonObjectTIME = jsonObject_.getJSONObject("TIME"); //time info
                                                JSONArray jsonArrayPATH = jsonObject_.getJSONArray("PATH"); //path info (points GPS)

                                                //time info
                                                int year = jsonObjectTIME.getInt("YEAR");
                                                int month = jsonObjectTIME.getInt("MON");
                                                int day = jsonObjectTIME.getInt("DAY");
                                                int hour = jsonObjectTIME.getInt("HOUR");
                                                int minute = jsonObjectTIME.getInt("MIN");
                                                int second = jsonObjectTIME.getInt("SEC");

                                                //path (GPS) info
                                                int size = jsonArrayPATH.length();
                                                double[] longis = new double[size];
                                                double[] latis = new double[size];
                                                for (int i = 0; i < size; i++) {
                                                    JSONObject path = jsonArrayPATH.getJSONObject(i);
                                                    double longi = path.getDouble("LON");
                                                    double lati = path.getDouble("LAT");
                                                    longis[i] = longi;
                                                    latis[i] = lati;
                                                }
                                                GPSEvent gpsEvent = new GPSEvent(latis, longis);

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case 23:
                                            //notify cancellation of navigation
                                            break;
                                        case 31:
                                            //acknowledge for audio tag
                                            EventBus.getDefault().post(new SpeechAckEvent(true));
                                            break;
                                        case 40:
                                            try {
                                                String VIN = jsonObject_.getString("VIN");
                                                //give this VIN to MApplication instance
                                                MApplication.getInstance().setVIN(VIN);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case 42:
                                            //acknowledge of start information
                                            ifToSendStart = false;
                                            tries_start = 0;
                                            break;
                                        case 44:
                                            //acknowledge of end information
                                            ifToSendStop = false;
                                            tries_stop = 0;
                                            break;
                                        default:
                                            break;
                                    }

                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            Log.d("AndroidUDP", "Data Length:" + dp_receive.getLength());

                            dp_receive.setLength(1024);

                            receiveHandler.sendEmptyMessage(1);

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
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

    public class UdpHeartBeatThread extends Thread {
        @Override
        public void run() {

            try {
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);

                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_heart = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK", "pandora");
                send_object.put("LEN", "999999");
                send_object.put("ETT", 12);
                String send_content = send_object.toString();

                DatagramPacket dp_send_heart = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, 9992);
                while (listenStatus) {
                    s_socket_heart.send(dp_send_heart);
                    //send a heart beat package every 20 seconds
                    try {
                        Thread.sleep(20 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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

    public class UdpStartThread extends Thread {
        @Override
        public void run() {
            try {
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);

                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_start = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK","pandora");
                send_object.put("LEN","999999");
                send_object.put("ETT",41);
                JSONObject send_time = new JSONObject();
                if (startTime.length == 3 && startDate.length == 3) {
                    send_time.put("YEAR", startDate[0]);
                    send_time.put("MON", startDate[1]);
                    send_time.put("DAY", startDate[2]);
                    send_time.put("HOUR", startTime[0]);
                    send_time.put("MIN", startTime[1]);
                    send_time.put("SEC", startTime[2]);
                }
                send_object.put("TIME", send_time);
                send_object.put("VIN", MApplication.getInstance().getVIN());

                String send_content = send_object.toString();
                int len = send_content.getBytes().length;

                DatagramPacket dp_send_start = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, 9992);
                while (listenStatus) {
                    while (ifToSendStart && tries_start < MAXNUM) {
                        s_socket_start.send(dp_send_start);
                        tries_start++;
                        //send an start info package every 5 seconds
                        try {
                            Thread.sleep(5 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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

    public class UdpStopThread extends Thread {
        @Override
        public void run() {
            try {
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);

                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_stop = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK","pandora");
                send_object.put("LEN","999999");
                send_object.put("ETT",43);

                JSONObject send_time = new JSONObject();
                if (stopTime.length == 3 && stopDate.length == 3) {
                    send_time.put("YEAR", stopDate[0]);
                    send_time.put("MON", stopDate[1]);
                    send_time.put("DAY", stopDate[2]);
                    send_time.put("HOUR", stopTime[0]);
                    send_time.put("MIN", stopTime[1]);
                    send_time.put("SEC", stopTime[2]);
                }
                send_object.put("TIME", send_time);
                send_object.put("VIN", MApplication.getInstance().getVIN());
                String send_content = send_object.toString();

                DatagramPacket dp_send_stop = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, 9992);
                while (listenStatus) {
                    while (ifToSendStop && tries_stop < MAXNUM) {
                        s_socket_stop.send(dp_send_stop);
                        tries_stop++;
                        //send an stop info package every 5 seconds
                        try {
                            Thread.sleep(5 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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

    private void buttonSetColor(Button bt, int cl) {
        if (cl == 0) {
            bt.setBackgroundColor(getResources().getColor(R.color.silver));
        } else if (cl == 1) {
            bt.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        } else {
            bt.setBackgroundColor(getResources().getColor(R.color.colorWarning));
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        final int FLING_MIN_DISTANCE = 100;
        final int FLING_MIN_VELOCITY = 200;

        //left

        if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
            Intent intent = new Intent(MainActivity.this, ShowActivity.class);
            startActivity(intent);
            //this.finish();
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
        }

        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendEvent event) {
        if (event != null) {
            int[] res = event.getMsg();
            int[] alert_res = event.getAlertMsg();
            Log.i("EVentBus", String.valueOf(res[0]));
            if (res.length != 9) return;
            int count = 0;
            int index = 0;
            Button[] buttons = new Button[]{button1, button2, button3, button4, button5, button6, button7, button8, button9};
            for (int i = 0; i < 9; i++) {
                buttonSetColor(buttons[i], res[i]);
            }

            voiceWarning(alert_res[0]);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGPSEventMainThread(GPSEvent event) {
        if (event != null) {
            longitudes = event.getMsglong();
            latitudes = event.getMsgLati();
            for (int i = 0; i < latitudes.length; i++) {
                String descrip = "node" + (i+1);
                BNRoutePlanNode node = new BNRoutePlanNode(longitudes[i], latitudes[i], descrip, descrip, BNRoutePlanNode.CoordinateType.BD09LL);
                mBNRoutePlanNodes.add(node);
            }
            mStartNode = mBNRoutePlanNodes.get(0);
            createDialog();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartEventMainThread(StartEvent event) {
        if (event != null) {
            String sDate = event.getMsg_date();
            String sTime = event.getMsg_time();
            String sKM = event.getMsg_km();

            String[] sDates = sDate.split("-");
            String[] sTimes = sTime.split(":");
            try {
                int year = Integer.parseInt(sDates[0]);
                int month = Integer.parseInt(sDates[1]);
                int day = Integer.parseInt(sDates[2]);

                int hour = Integer.parseInt(sTimes[0]);
                int minute = Integer.parseInt(sTimes[1]);

                startDate = new int[]{year, month, day};
                startTime = new int[]{hour, minute, 0};
                ifToSendStart = true;
                tries_start = 0;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStopEventMainThread(StartEvent event) {
        if (event != null) {
            String sDate = event.getMsg_date();
            String sTime = event.getMsg_time();
            String sKM = event.getMsg_km();

            String[] sDates = sDate.split("-");
            String[] sTimes = sTime.split(":");
            try {
                int year = Integer.parseInt(sDates[0]);
                int month = Integer.parseInt(sDates[1]);
                int day = Integer.parseInt(sDates[2]);

                int hour = Integer.parseInt(sTimes[0]);
                int minute = Integer.parseInt(sTimes[1]);

                stopDate = new int[]{year, month, day};
                stopTime = new int[]{hour, minute, 0};
                ifToSendStop = true;
                tries_stop = 0;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void voiceWarning(int level) {
        if (level == 0) {
            return;
        } else if (level == 1) {
            playBeeAndVibrate(getApplicationContext(), 2000, 1);
        } else if (level == 2) {
            playWarnAndVibrate(getApplicationContext(), 3000, 1);
        }

    }


    public static boolean isApkInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void createDialog() {
        mOffTextView = new TextView(this);
        mDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Launch Guide App")
                .setCancelable(false)
                .setView(mOffTextView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mOffTimer.cancel();
                        launchGuide();
                        //off();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        mOffTimer.cancel();
                    }
                })
                .create();
        mDialog.show();
        mDialog.setCanceledOnTouchOutside(false);

        mOffHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what > 0) {
                    mOffTextView.setText( msg.what+ "秒后自动跳转");
                    mOffTextView.setTextSize(24);
                    mOffTextView.setTextColor(getResources().getColor(R.color.colorAccent));
                    mOffTextView.setGravity(Gravity.CENTER);
                } else {
                    if (mDialog != null) {
                        mDialog.dismiss();
                        launchGuide();
                    }
                    //off();
                    mOffTimer.cancel();
                }
                super.handleMessage(msg);
            }
        };

        //count down timer
        mOffTimer = new Timer(true);
        TimerTask tt = new TimerTask() {
            int countTime = 10;
            @Override
            public void run() {
                if (countTime > 0) {
                    countTime--;
                }
                Message msg = new Message();
                msg.what = countTime;
                mOffHandler.sendMessage(msg);
            }
        };
        mOffTimer.schedule(tt, 1000, 1000);
    }

    public void launchGuide() {
        PackageManager packageManager = getPackageManager();
        if (isApkInstalled(getApplicationContext(),GUIDE_APP_NAME)) {
            Intent intent = packageManager.getLaunchIntentForPackage(GUIDE_APP_NAME);
            //intent.putExtra("title", "From DataM");
            //intent.putExtra("longs", longitudes);
            //intent.putExtra("latis", latitudes);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, mStartNode);
            bundle.putSerializable(ROUTE_PLAN_NODES, (Serializable) mBNRoutePlanNodes);
            intent.putExtras(bundle);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, "Not Installed" + GUIDE_APP_NAME, Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void onBackPressed() {
        Intent intent = new Intent(MainActivity.this, LogoffActivity.class);
        startActivity(intent);
    }


}