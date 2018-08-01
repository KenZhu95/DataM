package com.example.ken_z.datam;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.navi.BaiduMapNavigation;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRoutePlanManager;
import com.baidu.navisdk.adapter.IBNTTSManager;
import com.baidu.navisdk.adapter.IBaiduNaviManager;
import com.baidu.navisdk.adapter.impl.BaiduNaviManager;
import com.baidu.navisdk.adapter.impl.base.BNaviAuthManager;
import com.baidu.navisdk.comapi.setting.BNSettingManager;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.CharArrayWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

import static com.example.ken_z.datam.BeeAndVibrateManager.playBeeAndVibrate;
import static com.example.ken_z.datam.BeeAndVibrateManager.playWarnAndVibrate;

public class MapActivity extends AppCompatActivity {

    static final String ROUTE_PLAN_NODE = "routePlanNode";
    static final String ROUTE_PLAN_NODES = "routePlanNodes";
    static final String NAV_ABORT = "navigationAbort";
    private static final String APP_FOLDER_NAME = "datam";
    private static final String GUIDE_APP_NAME = "com.example.ken_z.datag";

    private MapView myMapView = null;
    private BaiduMap mBaiduMap;
    private CheckedTextView checkbox_traffic;
    private Button button_navi;
    private boolean if_start_navi = false;

    public MyOrientationListener myOrientationListener;
    boolean isFirst = true; //whether to set location for the first time
    public BitmapDescriptor mCurrentMaker;
    private MyLocationConfiguration.LocationMode mCurrentMode;
    private float mCurrentX;

    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;

    //location module
    private LocationClient locationClient;
    public MyLocationListener myLocationListener = new MyLocationListener();

    //navigation module
    RoutePlanSearch mSearch = null;
    String authinfo;
    private boolean if_init_success = false;
    private BNRoutePlanNode mStartNode = null;

    // for count down alert dialog
    private TextView mOffTextView;
    private Handler mOffHandler;
    private Timer mOffTimer;
    private Dialog mDialog;



    private Button action_button_1;
    private Button action_button_2;
    private Button action_button_3;

    private static String[] warnings = {"VIDEO","ETH","CAN","GPS","SVC","LIDAR","AFE","AIO","EQM"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_map);
        MApplication.getInstance().addActivity(this);
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        myMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = myMapView.getMap();
        checkbox_traffic = (CheckedTextView)findViewById(R.id.checkbox_traffic);
        button_navi = (Button) findViewById(R.id.button_navi);
        //checkbox_traffic.setChecked(true);
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setTrafficEnabled(checkbox_traffic.isChecked());

        //locationClient.requestLocation();
        checkbox_traffic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean if_traff = checkbox_traffic.isChecked();
                checkbox_traffic.setChecked(!if_traff);
                mBaiduMap.setTrafficEnabled(checkbox_traffic.isChecked());
            }
        });

        button_navi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startNavigation();
                createDialog();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            getActionBarButtons();
        }

        initSlidr();
        initLocation();
        initNavigation();

    }


    private void getActionBarButtons() {
        action_button_1 = (Button) findViewById(R.id.button_navi_1);
        action_button_2 = (Button) findViewById(R.id.button_navi_2);
        action_button_3 = (Button) findViewById(R.id.button_navi_3);
    }

    private void initSlidr() {
        SlidrConfig config = new SlidrConfig.Builder()
                .position(SlidrPosition.LEFT)
                .edge(true)
                .edgeSize(0.15f)
                .distanceThreshold(0.35f)
                .velocityThreshold(5f)
                .build();
        Slidr.attach(this, config);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mBaiduMap.setMyLocationEnabled(true);
        if (!locationClient.isStarted()) {
            locationClient.start();
            myOrientationListener.start();
        }
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        mBaiduMap.setMyLocationEnabled(false);
            locationClient.stop();
            myOrientationListener.stop();
    }

    private void initLocation() {
        mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
        mCurrentMaker = null;
        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                mCurrentMode, true, mCurrentMaker, accuracyCircleFillColor, accuracyCircleStrokeColor
        ));
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(myLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setNeedDeviceDirect(true);
        option.setCoorType("bd09ll");
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        option.disableCache(true);
        locationClient.setLocOption(option);
        myOrientationListener = new MyOrientationListener(this);
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });

    }

    private void initNavigation() {
        mSearch = RoutePlanSearch.newInstance();
        OnGetRoutePlanResultListener nvListener = new OnGetRoutePlanResultListener() {
            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        };
        mSearch.setOnGetRoutePlanResultListener(nvListener);

        BaiduNaviManagerFactory.getBaiduNaviManager().init(this, Environment.getExternalStorageDirectory().toString(),
                APP_FOLDER_NAME, new IBaiduNaviManager.INaviInitListener() {
                    @Override
                    public void onAuthResult(int i, String s) {
                        if (0 == i) {
                            authinfo = "key check success";
                        } else {
                            authinfo = "key check fail" + s;
                        }
                        MapActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MapActivity.this, authinfo, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void initStart() {
                        Toast.makeText(MapActivity.this, "navigation initialization begin", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void initSuccess() {
                        Toast.makeText(MapActivity.this, "navigation initialization success", Toast.LENGTH_SHORT).show();
                        if_init_success = true;
                        initSetting();
                        initTTS();
                    }

                    @Override
                    public void initFailed() {
                        Toast.makeText(MapActivity.this, "navigation initialization fail", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initSetting() {
    }

    private String getSDcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    private IBNTTSManager.IBNOuterTTSPlayerCallback mTTSCallback = new IBNTTSManager.IBNOuterTTSPlayerCallback() {

        @Override
        public int getTTSState() {
//            /** 播放器空闲 */
//            int PLAYER_STATE_IDLE = 1;
//            /** 播放器正在播报 */
//            int PLAYER_STATE_PLAYING = 2;
            return PLAYER_STATE_IDLE;
        }

        @Override
        public int playTTSText(String text, String s1, int i, String s2) {
            Log.e("BNSDKDemo", "playTTSText:" + text);
            return 0;
        }

        @Override
        public void stopTTS() {
            Log.e("BNSDKDemo", "stopTTS");
        }
    };

    private void initTTS() {
        //apply internal TTS
        BaiduNaviManagerFactory.getTTSManager().initTTS(getApplicationContext(),
                getSDcardDir(), APP_FOLDER_NAME, NormalUtils.getTTSAppID());

        //同步内置TTS状态callback
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedListener(new IBNTTSManager.IOnTTSPlayStateChangedListener() {
            @Override
            public void onPlayStart() {
                Log.e("datam", "ttsCallback.onPlayStart");
            }

            @Override
            public void onPlayEnd(String s) {
                Log.e("datam", "ttsCallback.onPlayEnd");
            }

            @Override
            public void onPlayError(int i, String s) {
                Log.e("datam", "ttsCallback.onPlayError");
            }
        });

        //注册异步状态消息
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedHandler(
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        Log.e("datam", "ttsHandler.msg.what=" + msg.what);
                    }
                }
        );
    }

    private void startNavigation() {

        if (!if_init_success) {
            Toast.makeText(MapActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }
        MyLocationData currentLoc = mBaiduMap.getLocationData();

        BNRoutePlanNode sNode = new BNRoutePlanNode(currentLoc.longitude, currentLoc.latitude, "Start", "Start", BNRoutePlanNode.CoordinateType.BD09LL);
        BNRoutePlanNode node_1 = new BNRoutePlanNode(currentLoc.longitude + 0.05, currentLoc.latitude, "node 1", "node 1", BNRoutePlanNode.CoordinateType.BD09LL);
        BNRoutePlanNode node_2 = new BNRoutePlanNode(currentLoc.longitude + 0.1, currentLoc.latitude + 0.05, "node 2", "node 2", BNRoutePlanNode.CoordinateType.BD09LL);
        BNRoutePlanNode eNode = new BNRoutePlanNode(currentLoc.longitude - 0.2, currentLoc.latitude - 0.2, "End", "End", BNRoutePlanNode.CoordinateType.BD09LL);

        mStartNode = sNode;

        final List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
        list.add(sNode);
        list.add(node_1);
        list.add(node_2);
        list.add(eNode);

        PackageManager packageManager = getPackageManager();
        if (isApkInstalled(getApplicationContext(),GUIDE_APP_NAME)) {
            Intent intent = packageManager.getLaunchIntentForPackage(GUIDE_APP_NAME);
            Bundle bundle = new Bundle();
            bundle.putInt(NAV_ABORT, 0);
            bundle.putSerializable(ROUTE_PLAN_NODE, mStartNode);
            bundle.putSerializable(ROUTE_PLAN_NODES, (Serializable) list);
            intent.putExtras(bundle);
            //intent.putExtra("title", "From DataM");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(MapActivity.this, "Not Installed" + GUIDE_APP_NAME, Toast.LENGTH_LONG).show();
        }

    }


    public class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //do not process new data after MapView is destroyed
            if (location == null || myMapView == null) {
                return;
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(mCurrentX)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            mBaiduMap.setMyLocationData(locData); //set location data
            int code = location.getLocType();
            System.out.print(code);
            if (isFirst) {
                isFirst = false;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //define map state
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(latLng)
                        .zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {

        }
    }

    @Override
    protected void onDestroy() {
        //locationClient.stop();
        //mBaiduMap.setMyLocationEnabled(false);
        myMapView.onDestroy();
        myMapView = null;
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        myMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        myMapView.onPause();
        super.onPause();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread( GuideEvent event) {
        if (event != null) {
            int[] res = event.getMsg();
            int[] res_alert = event.getAlertMsg();
            Log.i("EVentBus", String.valueOf(res[0]));
            int count = 0;
            int index = 0;
            Button[] buttons = new Button[]{action_button_1, action_button_2, action_button_3};
            for (Button bt : buttons) {
                bt.setBackgroundColor(getResources().getColor(R.color.colorNormal));
                bt.setText("");
            }
            while (count < 3 && index< 6 ) {
                if (res[index] == 2) {
                    buttons[count].setBackgroundColor(getResources().getColor(R.color.colorWarning));
                    buttons[count].setText(warnings[index]);
                    count++;
                }
                index++;
            }
            voiceWarning(res_alert[0]);
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
        mDialog = new AlertDialog.Builder(this)
                .setTitle("Launch Guide App")
                .setCancelable(false)
                .setView(mOffTextView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mOffTimer.cancel();
                        MainActivity.beginNav();
                        startNavigation();
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
                      MainActivity.beginNav();
                      startNavigation();
                  }
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
}
