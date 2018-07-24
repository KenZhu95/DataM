package com.example.ken_z.datam;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.greenrobot.event.EventBus;

import static com.example.ken_z.datam.BeeAndVibrateManager.playBeeAndVibrate;
import static com.example.ken_z.datam.BeeAndVibrateManager.playWarnAndVibrate;

public class ShowActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {
    Button buttonHDM;
    Button buttonLDWL;
    Button buttonLDWR;
    Button buttonBSDL;
    Button buttonBSDR;

    Button buttonIndex;
    TextView showIndex;
    Button level_1;
    Button level_2;
    Button level_3;
    Button level_4;
    Button level_5;
    Button buttonAPL;
    Button buttonBKP;
    private ConstraintLayout constraintLayout;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Drawable drawable = getResources().getDrawable(R.drawable.bkcolor);
        this.getWindow().setBackgroundDrawable(drawable);
        setContentView(R.layout.activity_show);
        MApplication.getInstance().addActivity(this);
        //buttonMap = (Button) findViewById(R.id.button_map);
        buttonHDM = (Button) findViewById(R.id.button_HDM);
        buttonLDWL = (Button) findViewById(R.id.button_LDWL);
        buttonLDWR = (Button) findViewById(R.id.button_LDWR);
        buttonBSDL = (Button) findViewById(R.id.button_BSDL);
        buttonBSDR = (Button) findViewById(R.id.button_BSDR);
        buttonAPL = (Button) findViewById(R.id.button_ACC);
        buttonBKP = (Button) findViewById(R.id.button_BRK);

        buttonIndex = (Button) findViewById(R.id.button_TGT);
        showIndex = (TextView) findViewById(R.id.show_index);
        level_1 = (Button) findViewById(R.id.button_level_1);
        level_2 = (Button) findViewById(R.id.button_level_2);
        level_3 = (Button) findViewById(R.id.button_level_3);
        level_4 = (Button) findViewById(R.id.button_level_4);
        level_5 = (Button) findViewById(R.id.button_level_5);


        constraintLayout = (ConstraintLayout) findViewById(R.id.constraint_show);
        constraintLayout.setOnTouchListener(this);
        constraintLayout.setLongClickable(true);
        gestureDetector = new GestureDetector((GestureDetector.OnGestureListener) this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        //Event Bus register
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Event Bus unregister
        EventBus.getDefault().unregister(this);
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
            Intent intent = new Intent(ShowActivity.this, MapActivity.class);
            startActivity(intent);
            //this.finish();
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
        }

        //right
        if (e1.getX() - e2.getX() < FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
            Intent intent = new Intent(ShowActivity.this, MainActivity.class);
            startActivity(intent);
            //this.finish();
            overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        }

        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ShowEvent event) {
        if (event != null) {
            int[] res_int = event.getMsgInt();
            int[] res_alert = event.getAlertMsg();
            if (res_int == null || res_int.length != 11) return;
            int info_HDM = res_int[0];
            int info_LDWL = res_int[1];
            int info_LDWR = res_int[2];
            int info_BSDL = res_int[3];
            int info_BSDR = res_int[4];
            int info_DIS = res_int[5];
            int info_GAP = res_int[6];
            int info_TGT = res_int[7];
            int info_APL = res_int[8];
            int info_BKP = res_int[9];
            int info_TAG = res_int[10];


            Button[] levels = new Button[]{level_1, level_2, level_3, level_4, level_5};
            for (int i = 0; i < info_GAP; i++) {
                levels[i].setBackground(getResources().getDrawable(R.drawable.button_solid));
            }
            for (int j = info_GAP; j < 5; j++) {
                levels[j].setBackground(getResources().getDrawable(R.drawable.button_line));
            }

            if (info_APL == 1) {
                buttonAPL.setBackground(getResources().getDrawable(R.drawable.button_solid));
            } else {
                buttonAPL.setBackground(getResources().getDrawable(R.drawable.button_line));
            }

            if (info_BKP == 1) {
                buttonBKP.setBackground(getResources().getDrawable(R.drawable.button_solid));
            } else {
                buttonBKP.setBackground(getResources().getDrawable(R.drawable.button_line));
            }

            showIndex.setText(String.valueOf(info_DIS));
            if (info_TAG == 1) {
                showIndex.setTextColor(getResources().getColor(R.color.snowWhite));
            } else {
                showIndex.setTextColor(getResources().getColor(R.color.grey));
            }
            if (info_TGT == 1) {
                buttonIndex.setVisibility(View.VISIBLE);
            } else {
                buttonIndex.setVisibility(View.INVISIBLE);
            }

            if (info_HDM == 0) {
                buttonHDM.setVisibility(View.INVISIBLE);
            } else {
                buttonHDM.setVisibility(View.VISIBLE);
                if (info_HDM == 1) {
                    buttonHDM.setBackgroundColor(getResources().getColor(R.color.colorNormal));
                } else {
                    buttonHDM.setBackgroundColor(getResources().getColor(R.color.colorWarning));
                }
            }

            if (info_LDWL == 0) {
                buttonLDWL.setVisibility(View.INVISIBLE);
            } else {
                buttonLDWL.setVisibility(View.VISIBLE);
                if (info_LDWL == 1) {
                    buttonLDWL.setBackgroundColor(getResources().getColor(R.color.snowWhite));
                } else {
                    buttonLDWL.setBackgroundColor(getResources().getColor(R.color.colorWarning));
                }
            }

            if (info_LDWR == 0) {
                buttonLDWR.setVisibility(View.INVISIBLE);
            } else {
                buttonLDWR.setVisibility(View.VISIBLE);
                if (info_LDWR == 1) {
                    buttonLDWR.setBackgroundColor(getResources().getColor(R.color.snowWhite));
                } else {
                    buttonLDWR.setBackgroundColor(getResources().getColor(R.color.colorWarning));
                }
            }

            if (info_BSDL == 0) {
                buttonBSDL.setVisibility(View.INVISIBLE);
            } else {
                buttonBSDL.setVisibility(View.VISIBLE);
                if (info_BSDL == 1) {
                    buttonBSDL.setBackgroundColor(getResources().getColor(R.color.snowWhite));
                } else {
                    buttonBSDL.setBackgroundColor(getResources().getColor(R.color.colorWarning));
                }
            }

            if (info_BSDR == 0) {
                buttonBSDR.setVisibility(View.INVISIBLE);
            } else {
                buttonBSDR.setVisibility(View.VISIBLE);
                if (info_BSDR == 1) {
                    buttonBSDR.setBackgroundColor(getResources().getColor(R.color.snowWhite));
                } else {
                    buttonBSDR.setBackgroundColor(getResources().getColor(R.color.colorWarning));
                }
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
