package com.example.ken_z.datam;

import android.content.Intent;
import android.net.http.EventHandler;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRouteGuideManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.PriorityQueue;

import de.greenrobot.event.EventBus;

import static com.example.ken_z.datam.BeeAndVibrateManager.playBeeAndVibrate;
import static com.example.ken_z.datam.BeeAndVibrateManager.playWarnAndVibrate;

public class GuideActivity extends AppCompatActivity {

    private static final String TAG = GuideActivity.class.getName();

    private BNRoutePlanNode mBNRoutePlanNode = null;
    private List<BNRoutePlanNode> mBNRoutePlanNodes;

    private IBNRouteGuideManager mRouteGuideManager;

    private Button action_button_1;
    private Button action_button_2;
    private Button action_button_3;

    private static String[] warnings = {"VIDEO","ETH","CAN","GPS","SVC","LIDAR","AFE","AIO","EQM"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        createHandler();

        mRouteGuideManager = BaiduNaviManagerFactory.getRouteGuideManager();
        View view = mRouteGuideManager.onCreate(this, mOnNavigationListener);
        //setContentView(R.layout.activity_guide);
        if (view != null) {
            setContentView(view);
        }
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);


        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                mBNRoutePlanNode = (BNRoutePlanNode) bundle.getSerializable(MapActivity.ROUTE_PLAN_NODE);
                mBNRoutePlanNodes = (List<BNRoutePlanNode>) bundle.getSerializable(MapActivity.ROUTE_PLAN_NODES);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            getActionBarButtons();
        }

        routeGuideEvent();
    }

    private void getActionBarButtons() {
        action_button_1 = (Button) findViewById(R.id.button_navi_1);
        action_button_2 = (Button) findViewById(R.id.button_navi_2);
        action_button_3 = (Button) findViewById(R.id.button_navi_3);
    }

    private void routeGuideEvent() {
        com.example.ken_z.datam.EventHandler.getInstance().getDialog(this);
        com.example.ken_z.datam.EventHandler.getInstance().showDialog();

        BaiduNaviManagerFactory.getRouteGuideManager().setRouteGuideEventListener(
                new IBNRouteGuideManager.IRouteGuideEventListener() {
                    @Override
                    public void onCommonEventCall(int what, int arg1, int arg2, Bundle bundle) {
                        com.example.ken_z.datam.EventHandler.getInstance().handleNaviEvent(what, arg1, arg2, bundle);
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Event Bus register
        EventBus.getDefault().register(this);
        mRouteGuideManager.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Event Bus unregister
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRouteGuideManager.onResume();
        // 自定义图层
        //showOverlay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }



    private static final int MSG_RESET_NODE = 3;

    private Handler hd = null;

    private void createHandler() {
        if (hd == null) {
            hd = new Handler(getMainLooper()) {
                public void handleMessage(Message msg) {
                    if (msg.what == MSG_RESET_NODE) {
                        //
                    }
                }
            };
        }

    }

    private IBNRouteGuideManager.OnNavigationListener mOnNavigationListener =
            new IBNRouteGuideManager.OnNavigationListener() {
                @Override
                public void onNaviGuideEnd() {
                    //exit navigation
                    finish();
                }

                @Override
                public void notifyOtherAction(int actionType, int i1, int i2, Object o) {
                    if (actionType == 0) {
                        //get destination, exit automatically
                        Log.i(TAG, "notifyOtherAction actionType = " + actionType + ",导航到达目的地！");
                        mRouteGuideManager.forceQuitNaviWithoutDialog();
                    }
                }
            };


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GuideEvent event) {
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
}
