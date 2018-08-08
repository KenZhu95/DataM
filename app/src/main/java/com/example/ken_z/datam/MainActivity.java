package com.example.ken_z.datam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
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
import android.widget.EditText;
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

import de.greenrobot.event.EventBus;

import static com.example.ken_z.datam.BeeAndVibrateManager.playBeeAndVibrate;
import static com.example.ken_z.datam.BeeAndVibrateManager.playWarnAndVibrate;
import static com.example.ken_z.datam.MapActivity.ROUTE_PLAN_NODE;
import static com.example.ken_z.datam.MapActivity.ROUTE_PLAN_NODES;
import static com.example.ken_z.datam.MapActivity.NAV_ABORT;
import static com.example.ken_z.datam.ShowActivity.breakWarningShow;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {
    private static final String TAG = MainActivity.class.getName();

    //public static final String APP_IP = "10.111.10.84";
    //public static final String APP_IP = "192.168.1.111";
    public static String APP_IP = "192.168.8.99";
    private static final int APP_PORT = 9990;
    private static final int SERVER_RECEIVE_PORT = 9992;

//    private static final int MAXNUM = 10;
//    private static final int REVNUM = 12;
    //receive timeout length for data receive sockets (10s for connect, 15s for break)
    private static final int TIMEOUT = 10000;
    private static final int TIMEOUT_BREAK = 15000;
    //Guide App package name
    private static final String GUIDE_APP_NAME = "com.example.ken_z.datag";

    //buttons to show states
    private Button button1, button2, button3, button4, button5, button6, button7, button8, button9;
    //buttons for different functions
    private Button buttonSpeech, buttonPermission, buttonFinish;
//    private Button buttonResetIP, buttonChangeIP, buttonStartAll;
//    private EditText text_IP;
    private RelativeLayout relativeLayout;
    private GestureDetector gestureDetector;
    public static TextView breakWarningMain;

    //whether to keep contact with server. always true, only to be false in onDestroy()
    private boolean listenStatus = true;

    private DatagramSocket r_socket;
    private DatagramSocket s_socket_connect;
    private DatagramSocket s_socket_heart;
    private DatagramSocket s_socket_navigation;
    private DatagramSocket s_socket_break;

    boolean ifToSendNavStartAck = false;
    boolean ifToSendNavAbortAck = false;
    boolean ifToSendNavFinishAck = false;
    boolean ifToSendBreak = false;
    boolean ifToSendConnect = true;
    boolean ifToReceive = false;
    static boolean ifCurrentNav = false;

    public static void beginNav() {
        ifCurrentNav = true;
    }

    public static void endNav() {
        ifCurrentNav = false;
    }


    private ReceiveHandler receiveHandler = new ReceiveHandler();

    //guide start node and list of all nodes
    private List<BNRoutePlanNode> mBNRoutePlanNodes = new ArrayList<>();
    private BNRoutePlanNode mStartNode = null;

    // for count down alert dialog
    private TextView mOffTextView;
    private Handler mOffHandler;
    private Timer mOffTimer;
    private Dialog mDialog;
    private Dialog mIPDialog;

    private boolean isRunning = false;

    private double[] latitudes;
    private double[] longitudes;


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
        buttonSpeech = findViewById(R.id.button_speech);
        buttonFinish = findViewById(R.id.button_finish);
        //buttonChangeIP = findViewById(R.id.button_change_IP);
        //buttonResetIP = findViewById(R.id.button_reset_IP);
        //buttonStartAll = findViewById(R.id.button_start_all);
        //text_IP = findViewById(R.id.edit_IP);
        breakWarningMain = findViewById(R.id.text_break_main);
        breakWarningMain.setVisibility(View.VISIBLE);

        relativeLayout.setOnTouchListener(this);
        relativeLayout.setLongClickable(true);
        gestureDetector = new GestureDetector((GestureDetector.OnGestureListener)this);



        buttonPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocation();
            }
        });

        buttonSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeech();
            }
        });

        //only for debugging, should be cancelled in release version
//        button1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                createAbortDialog();
//            }
//        });

        buttonFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFinish();
            }
        });
        startAll();

//        buttonResetIP.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                resetIP();
//            }
//        });
//
//        buttonChangeIP.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                changeIP();
//            }
//        });
//
//        buttonStartAll.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startAll();
//            }
//        });
    }

    private void startAll() {
        isRunning = true;
        //UDP threads: one for data receive, one for heart break, others for data sending issues. Only to receive data in MainActivity

        new UdpReceiveThread().start();
        new UdpConnectThread().start();
        new UdpHeartBeatThread().start();
        new UdpNavigationThread().start();
        new UdpBreakThread().start();
    }

//    private void changeIP() {
//        final String newIP = text_IP.getText().toString();
//        if (newIP == null || newIP.equals("")) {
//            Toast.makeText(MainActivity.this, "IP地址为空", Toast.LENGTH_LONG).show();
//        } else {
//            mOffTextView = new TextView(this);
//            mIPDialog = new android.app.AlertDialog.Builder(this)
//                    .setTitle("确认更改IP地址")
//                    .setCancelable(false)
//                    .setView(mOffTextView)
//                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            APP_IP = newIP;
//                            Toast.makeText(MainActivity.this, "修改IP地址为" + APP_IP, Toast.LENGTH_LONG).show();
//                        }
//                    })
//                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            mIPDialog.dismiss();
//                        }
//                    })
//                    .create();
//            mIPDialog.show();
//            mIPDialog.setCanceledOnTouchOutside(false);
//        }
//    }

//    private void resetIP() {
//        mOffTextView = new TextView(this);
//        mIPDialog = new android.app.AlertDialog.Builder(this)
//                .setTitle("确认重置IP地址")
//                .setCancelable(false)
//                .setView(mOffTextView)
//                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        APP_IP = "192.168.8.99";
//                        Toast.makeText(MainActivity.this, "重置IP地址为" + APP_IP, Toast.LENGTH_LONG).show();
//                    }
//                })
//                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mIPDialog.dismiss();
//                    }
//                })
//                .create();
//        mIPDialog.show();
//        mIPDialog.setCanceledOnTouchOutside(false);
//    }


    //check permissions (GPS info, audio, storage)
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

    //goto speech tag activity
    private void startSpeech() {
        Intent intent = new Intent(MainActivity.this, SpeechActivity.class);
        startActivity(intent);
    }

    //show dialog wo confirm a finish of a navigation action
    private void startFinish() {
        mOffTextView = new TextView(this);
        mDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("确认导航完成")
                .setCancelable(false)
                .setView(mOffTextView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ifToSendNavFinishAck = true; //to send a finish acknowledge message to server
                        endNav(); //to notify a finish of navigation
                        Toast.makeText(MainActivity.this, "导航完成", Toast.LENGTH_LONG).show();
                        }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialog.dismiss();
                    }
                })
                .create();
        mDialog.show();
        mDialog.setCanceledOnTouchOutside(false);
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
        if (isRunning) {
            r_socket.close();
            s_socket_connect.close();
            s_socket_heart.close();
            s_socket_navigation.close();
            s_socket_break.close();
        }

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
        listenStatus = true;
    }

//    thread to receive data from server
    public class UdpReceiveThread extends Thread {
        @Override
        public void run() {
            try {
                byte[] buf = new byte[1024];
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);
                InetAddress APP_ADD = InetAddress.getByName(APP_IP);

                DatagramPacket dp_receive = new DatagramPacket(buf, 1024);

                while (listenStatus) {
                    //only to receive data when connecting with server
                    while (ifToReceive) {
                        //r_socket = new DatagramSocket(APP_PORT);
                        r_socket.setSoTimeout(TIMEOUT_BREAK);
                        try {
                            r_socket.receive(dp_receive);
                            if (!dp_receive.getAddress().equals(APP_ADD)) {
                                throw new IOException("Received packet from an unknown source");
                            }

                            String rev_log_ = new String(dp_receive.getData(), 0, dp_receive.getLength());
                            JSONObject jsonObject_;
                            jsonObject_ = new JSONObject(rev_log_);

                            try {
                                String chk = jsonObject_.getString("CHK");
                                int ETT = jsonObject_.getInt("ETT");
                                String LEN = jsonObject_.getString("LEN");

                                if (chk.equals("pandora")) {
                                    switch (ETT) {
                                        // STATUS + HMI + Alert
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
                                        case 21: //start a navigation
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
                                                //pass latitude and longitude info to main thread for starting navigation
                                                GPSEvent gpsEvent = new GPSEvent(latis, longis);
                                                EventBus.getDefault().post(gpsEvent);

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case 23:
                                            //notify cancellation of navigation
                                            EventBus.getDefault().postSticky(new NavAbortEvent(true));
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
                                        default:
                                            break;
                                    }
                                }
                            } catch (Exception e) {
                                //if no data arriving more than 10s, throw exception
                                e.printStackTrace();
                            }

                            dp_receive.setLength(1024);

                            Log.d("AndroidUDP", "Received.");
                        } catch (Exception e) {
                            ifToReceive = false;
                            ifToSendConnect = true;
                            //resetUI();
                            ResetEvent resetEvent = new ResetEvent(true);
                            EventBus.getDefault().postSticky(resetEvent);
                            Log.d("AndroidUDP", "Time out,");
                        }
                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.d("AndroidUDP", e.getMessage());
            }
        }
    }

    //thread to try to connect with server
    public class UdpConnectThread extends Thread {
        @Override
        public void run() {
            try {
                byte[] buf = new byte[1024];
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);
                InetAddress APP_ADD = InetAddress.getByName(APP_IP);

                s_socket_connect = new DatagramSocket();
                r_socket = new DatagramSocket(APP_PORT);

                JSONObject send_object = new JSONObject();
                send_object.put("CHK", "pandora");
                send_object.put("ETT", 10);
                send_object.put("LEN", "60000");
                int len = send_object.toString().getBytes().length;
                String lenString = String.format("%05d", len);
                send_object.put("LEN", lenString);

                /*send a json object, three keys: "CHK","ETT","LEN"
                ETT = 10 for connection request*/

                String send_content = send_object.toString();
                DatagramPacket dp_send = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);
                DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
                //only to receive a piece of data from server, if fail to receive for 10s, resend connection request to server
                r_socket.setSoTimeout(TIMEOUT);
                while (listenStatus) {
                    while (ifToSendConnect) {
                        //s_socket_connect.send(dp_send);
                        try {
                            s_socket_connect.send(dp_send);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("AndroidUDP", "" + e.getMessage());
                        }

                        try {
                            r_socket.receive(dp_receive);
                            if (!dp_receive.getAddress().equals(APP_ADD)) {
                                throw new IOException("Received packet from an unknown source");
                            }
                            //if succeed to receive data from server, then receive thread can work, stop to send connection request
                            ifToReceive = true;
                            ifToSendConnect = false;
                            ResetEvent resetEvent = new ResetEvent(false);
                            EventBus.getDefault().postSticky(resetEvent);

                            Log.d("AndroidUDP", "Received.");
                        } catch (Exception e) {
                            Log.d("AndroidUDP", "Time out,");
                        }
                    }
                }

            }catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.d("AndroidUDP", e.getMessage());
            }
        }
    }

    //thread to send heart break packages to keep connection with server
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
                send_object.put("LEN", "99999");
                send_object.put("ETT", 12);
                int len = send_object.toString().getBytes().length;
                String lenString = String.format("%05d", len);
                send_object.put("LEN", lenString);
                //ETT = 12 for heart break

                String send_content = send_object.toString();
                DatagramPacket dp_send_heart = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);
                while (listenStatus) {
                    s_socket_heart.send(dp_send_heart);
                    //send a heart beat package every 10 seconds
                    try {
                        Thread.sleep(10 * 1000);
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

    //thread to send messages about navigation
    public class UdpNavigationThread extends Thread {
        @Override
        public void run() {

            try {
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);

                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_navigation = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK", "pandora");
                send_object.put("LEN", "60000");
                while (listenStatus) {
                    //to send navigation start acknowledge to server ETT=22
                    if (ifToSendNavStartAck) {
                        send_object.put("ETT", 22);

                        int len = send_object.toString().getBytes().length;
                        String lenString = String.format("%05d", len);
                        send_object.put("LEN", lenString);

                        String send_content = send_object.toString();
                        DatagramPacket dp_send_start = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);

                        s_socket_navigation.send(dp_send_start);
                        ifToSendNavStartAck = false;
                    }

                    //to send navigation abort acknowledge to server ETT=24
                    if (ifToSendNavAbortAck) {
                        send_object.put("ETT", 24);
                        int len = send_object.toString().getBytes().length;
                        String lenString = String.format("%05d", len);
                        send_object.put("LEN", lenString);

                        String send_content = send_object.toString();
                        DatagramPacket dp_send_start = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);

                        s_socket_navigation.send(dp_send_start);
                        endNav();

                        PackageManager packageManager = getPackageManager();
                        if (isApkInstalled(getApplicationContext(),GUIDE_APP_NAME)) {
                            //Intent intent = packageManager.getLaunchIntentForPackage(GUIDE_APP_NAME);
                            Intent intent = new Intent();
                            ComponentName cn = new ComponentName(GUIDE_APP_NAME, GUIDE_APP_NAME + ".GuideActivity");
                            try {
                                //open DataG App, GuideActivity page, to abort navigation and exit App
                                intent.setComponent(cn);
                                Bundle bundle = new Bundle();
                                bundle.putInt(NAV_ABORT, 1);
                                intent.putExtras(bundle);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } catch (Exception e) { }

                        } else {
                            Toast.makeText(MainActivity.this, "Not Installed" + GUIDE_APP_NAME, Toast.LENGTH_LONG).show();
                        }

                        ifToSendNavAbortAck = false;
                    }

                    //to send navigation finish notification message to server ETT=25
                    if (ifToSendNavFinishAck) {
                        send_object.put("ETT", 25);
                        int len = send_object.toString().getBytes().length;
                        String lenString = String.format("%05d", len);
                        send_object.put("LEN", lenString);

                        String send_content = send_object.toString();
                        DatagramPacket dp_send_start = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);

                        s_socket_navigation.send(dp_send_start);

                        ifToSendNavFinishAck = false;
                    }

                    try {
                        Thread.sleep(1000);
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

    //thread to send break notification to server, only when exiting ths App ETT=11
    public class UdpBreakThread extends Thread {
        @Override
        public void run() {
            try {
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);

                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_break = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK", "pandora");
                send_object.put("LEN", "99999");
                send_object.put("ETT", 11);
                int len = send_object.toString().getBytes().length;
                String lenString = String.format("%05d", len);
                send_object.put("LEN", lenString);

                String send_content = send_object.toString();
                DatagramPacket dp_send_break = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, SERVER_RECEIVE_PORT);
                while (listenStatus) {
                    if (ifToSendBreak) {
                        s_socket_break.send(dp_send_break);
                        try {
                            //exit 0.5s after sending break notification out
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        MApplication.getInstance().exit();
                    }
                    try {
                        Thread.sleep(500);
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


    //to show the right colors on each STATUS button
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

    //to jump to next activity with scrolling left
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


    //receive STATUS data from receive thread, to update button colors (UI) within main thread and voice alert
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendEvent event) {
        if (event != null) {
            int[] res = event.getMsg();
            int[] alert_res = event.getAlertMsg();
            Log.i("EVentBus", String.valueOf(res[0]));
            if (res.length != 9) return;
            Button[] buttons = new Button[]{button1, button2, button3, button4, button5, button6, button7, button8, button9};
            for (int i = 0; i < 9; i++) {
                buttonSetColor(buttons[i], res[i]);
            }

            voiceWarning(alert_res[0]);
        }
    }

    //receive GPS data (longitude, latitude pairs) from receive thread, and to start DataG for navia=gation
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GPSEvent event) {
        if (event != null) {
            longitudes = event.getMsglong();
            latitudes = event.getMsgLati();
            mBNRoutePlanNodes.clear();
            for (int i = 0; i < latitudes.length; i++) {
                String descrip = "node" + (i+1);
                BNRoutePlanNode node = new BNRoutePlanNode(longitudes[i], latitudes[i], descrip, descrip, BNRoutePlanNode.CoordinateType.BD09LL);
                //generate node from each (longitude, latitude) pair and add to nodes list
                mBNRoutePlanNodes.add(node);
            }
            //set the first node in list as start node
            mStartNode = mBNRoutePlanNodes.get(0);
            createDialog();
        }
    }

    //receive navigation abort notifications, and create a dialog to open DataG App for navigation
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEventMainThread(NavAbortEvent event) {
        if (event != null) {
            boolean abort = event.isMsg_abort();
            if (abort) {
                createAbortDialog();
            }
        }
    }

    //to reset warning text UI when breaking and connection
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEventMainThread(ResetEvent event) {
        if (event != null) {
            boolean reset = event.getMsg();
            if (reset) {
                breakWarningMain.setVisibility(View.VISIBLE);
                breakWarningShow.setVisibility(View.VISIBLE);
            } else {
                breakWarningMain.setVisibility(View.INVISIBLE);
                breakWarningShow.setVisibility(View.INVISIBLE);
            }
        }
    }

    //to make alert voice according to STATUS
    private void voiceWarning(int level) {
        if (level == 0) {
            return;
        } else if (level == 1) {
            playBeeAndVibrate(getApplicationContext(), 2000, 1);
        } else if (level == 2) {
            playWarnAndVibrate(getApplicationContext(), 3000, 1);
        }

    }

    //check whether an App is installed (to check DataG App)
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

    //create a dialpg with only "confirm" button to start navigation
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
                        //change status to "on navigation", send an acknowledge to server and launch DataG App
                        ifToSendNavStartAck = true;
                        beginNav();
                        launchGuide();
                    }
                })
//                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                        mOffTimer.cancel();
//                    }
//                })
                .create();
        mDialog.show();
        mDialog.setCanceledOnTouchOutside(false);

        //dialog text contains a count down of 5s. If exceeds 5s, automatically launch DataG and start navigation

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
                        ifToSendNavStartAck = true;
                        beginNav();
                        launchGuide();
                    }
                    mOffTimer.cancel();
                }
                super.handleMessage(msg);
            }
        };

        //count down timer
        mOffTimer = new Timer(true);
        TimerTask tt = new TimerTask() {
            int countTime = 5;
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

    //create a dialog to abort navigation, only "confirm" button, count down 5s
    public void createAbortDialog() {
        if (!ifCurrentNav) {
            return;
        }
        mOffTextView = new TextView(this);
        mDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Abort Navigation")
                .setCancelable(false)
                .setView(mOffTextView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ifToSendNavAbortAck = true;
                        mOffTimer.cancel();
                        Toast.makeText(MainActivity.this, "退出导航任务", Toast.LENGTH_LONG).show();

                        //off();
                    }
                })
                .create();
        mDialog.show();
        mDialog.setCanceledOnTouchOutside(false);

        mOffHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what > 0) {
                    mOffTextView.setText( msg.what+ "秒后自动结束导航任务");
                    mOffTextView.setTextSize(24);
                    mOffTextView.setTextColor(getResources().getColor(R.color.colorAccent));
                    mOffTextView.setGravity(Gravity.CENTER);
                } else {
                    if (mDialog != null) {
                        mDialog.dismiss();
                        ifToSendNavAbortAck = true;
                        Toast.makeText(MainActivity.this, "退出导航任务", Toast.LENGTH_LONG).show();
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
            int countTime = 5;
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

    //launch DataG APP for navigation, pass guide nodes
    public void launchGuide() {
        PackageManager packageManager = getPackageManager();
        if (isApkInstalled(getApplicationContext(),GUIDE_APP_NAME)) {
            Intent intent = packageManager.getLaunchIntentForPackage(GUIDE_APP_NAME);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, mStartNode);
            bundle.putSerializable(ROUTE_PLAN_NODES, (Serializable) mBNRoutePlanNodes);
            bundle.putInt(NAV_ABORT, 0);
            intent.putExtras(bundle);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, "Not Installed" + GUIDE_APP_NAME, Toast.LENGTH_LONG).show();
        }
    }

    //to exit APP, show a dialog for confirmation, and send a break notification before exiting
    @Override
    public void onBackPressed() {
        mOffTextView = new TextView(this);
        mDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("退出程序")
                .setCancelable(false)
                .setView(mOffTextView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ifToSendBreak = true;
                        Toast.makeText(MainActivity.this, "退出程序", Toast.LENGTH_LONG).show();
                        //off();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialog.dismiss();
                    }
                })
                .create();
        mDialog.show();
        mDialog.setCanceledOnTouchOutside(false);

    }


}
