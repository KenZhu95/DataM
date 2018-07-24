package com.example.ken_z.datam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.sunflower.FlowerCollector;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.greenrobot.event.EventBus;

public class SpeechActivity extends Activity implements View.OnClickListener {

    public static final String PREFER_NAME = "com.iflytek.setting";
    private static final String APP_IP = "192.168.8.99";
    private static final int APP_PORT = 9980;
    private static String TAG = SpeechActivity.class.getSimpleName();
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    private EditText mResultText;
    private Switch switchSpeaker;
    private TextView textSpeaker;
    private Toast mToast;
    private SharedPreferences mSharedPreferences;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private boolean mTranslateEnable = false;
    private int currVolume;

    //to send audio data, translation and time to server

    private int[] curDates = new int[3];
    private int[] curTimes = new int[3];
    String uploadText = null;
    private RandomAccessFile accessFile = null;
    private byte[] translationData;
    private int audio_index = -1;
    private boolean listenStatus = true;
    private boolean ifToSendAudio = false;
    private int tries_audio = 0;
    DatagramSocket s_socket_audio;
    private static final int MAXNUM = 10;
    private static final int MAXBYTES = 40000;



    @SuppressLint("ShowToast")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_speech);
        MApplication.getInstance().addActivity(this);

        initLayout();
        // 初始化识别无UI识别对象
        //SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID + "=" + R.string.speech_app_id);
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(SpeechActivity.this, mInitListener);

        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(SpeechActivity.this, mInitListener);

        mSharedPreferences = getSharedPreferences(PREFER_NAME,
                Activity.MODE_PRIVATE);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mResultText = ((EditText) findViewById(R.id.iat_text));
        textSpeaker = (TextView) findViewById(R.id.text_speaker);
        switchSpeaker = (Switch) findViewById(R.id.switch_speaker);
        switchSpeaker.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    textSpeaker.setText("免提");
                    openSpeaker();
                } else {
                    textSpeaker.setText("耳机");
                    closeSpeaker();
                }
            }
        });

        String SEND_FILE_PATH = Environment.getExternalStorageDirectory()+"/msc/iat.wav";
//        try {
//            accessFile = new RandomAccessFile(SEND_FILE_PATH, "r");
//            long ac_length = accessFile.length();
//            translationData = new byte[(int)ac_length];
//            accessFile.read(translationData);
//            audio_index = MApplication.getInstance().getAIN();
//            MApplication.getInstance().newAudio();
//            //ifToSendAudio = true;
//            Toast.makeText(SpeechActivity.this, "length is :" + translationData.length, Toast.LENGTH_LONG).show();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        new UdpAudioThread().start();
    }

    //扬声器开启和关闭
    public void openSpeaker() {

        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.ROUTE_SPEAKER);
        currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }
        //audioManager.setMode(AudioManager.MODE_IN_CALL);
        Toast.makeText(SpeechActivity.this, "开启免提", Toast.LENGTH_LONG).show();
        audioManager.setSpeakerphoneOn(true);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), AudioManager.STREAM_VOICE_CALL);
    }

    public void closeSpeaker() {

        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.ROUTE_SPEAKER);
        currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }
        //audioManager.setMode(AudioManager.MODE_IN_CALL);
        Toast.makeText(SpeechActivity.this, "关闭免提", Toast.LENGTH_LONG).show();
        audioManager.setSpeakerphoneOn(false);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currVolume, AudioManager.STREAM_VOICE_CALL);

    }

    /**
     * 初始化Layout。
     */
    private void initLayout() {
        findViewById(R.id.iat_recognize).setOnClickListener(SpeechActivity.this);
        findViewById(R.id.iat_recognize_stream).setOnClickListener(SpeechActivity.this);
        findViewById(R.id.iat_upload_wav).setOnClickListener(SpeechActivity.this);
        //findViewById(R.id.iat_upload_userwords).setOnClickListener(SpeechActivity.this);
        findViewById(R.id.iat_stop).setOnClickListener(SpeechActivity.this);
        findViewById(R.id.iat_cancel).setOnClickListener(SpeechActivity.this);
    }

    int ret = 0; // 函数调用返回值

    @Override
    public void onClick(View view) {
        if( null == mIat ){
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip( "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化" );
            return;
        }

        switch (view.getId()) {
            // 进入参数设置页面
            case R.id.iat_recognize:
                // 移动数据分析，收集开始听写事件
                FlowerCollector.onEvent(SpeechActivity.this, "iat_recognize");

                mResultText.setText(null);// 清空显示内容
                mIatResults.clear();
                //记录当前系统时间，以备发送给后台
                Calendar calendar = Calendar.getInstance();
                int curYear = calendar.get(Calendar.YEAR);
                int curMonth = calendar.get(Calendar.MONTH) + 1;
                int curDay = calendar.get(Calendar.DAY_OF_MONTH);

                int curHour = calendar.get(Calendar.HOUR_OF_DAY);
                int curMinute = calendar.get(Calendar.MINUTE);
                int curSecond = calendar.get(Calendar.SECOND);

                curDates = new int[]{curYear, curMonth, curDay};
                curTimes = new int[]{curHour, curMinute, curSecond};

                // 设置参数
                setParam();
                boolean isShowDialog = mSharedPreferences.getBoolean(
                        getString(R.string.pref_key_iat_show), true);
                if (isShowDialog) {
                    // 显示听写对话框
                    mIatDialog.setListener(mRecognizerDialogListener);
                    mIatDialog.show();
                    showTip(getString(R.string.text_begin));
                } else {
                    // 不显示听写对话框
                    ret = mIat.startListening(mRecognizerListener);
                    if (ret != ErrorCode.SUCCESS) {
                        showTip("听写失败,错误码：" + ret);
                    } else {
                        showTip(getString(R.string.text_begin));
                    }
                }
                break;
            // 音频流识别
            case R.id.iat_recognize_stream:
                mResultText.setText(null);// 清空显示内容
                mIatResults.clear();
                // 设置参数
                setParam();
                // 设置音频来源为外部文件
                mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
                // 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
                // mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
                // mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
                ret = mIat.startListening(mRecognizerListener);
                if (ret != ErrorCode.SUCCESS) {
                    showTip("识别失败,错误码：" + ret);
                } else {
                    byte[] audioData = FucUtil.readAudioFile(SpeechActivity.this, "iattest.wav");

                    if (null != audioData) {
                        showTip(getString(R.string.text_begin_recognizer));
                        // 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），
                        // 位长16bit，单声道的wav或者pcm
                        // 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
                        // 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别。
                        // 音频切分方法：FucUtil.splitBuffer(byte[] buffer,int length,int spsize);
                        mIat.writeAudio(audioData, 0, audioData.length);
                        mIat.stopListening();
                    } else {
                        mIat.cancel();
                        showTip("读取音频流失败");
                    }
                }
                break;
            // 停止听写
            case R.id.iat_stop:
                mIat.stopListening();
                showTip("停止听写");
                break;
            // 取消听写
            case R.id.iat_cancel:
                mIat.cancel();
                showTip("取消听写");
                break;
            case R.id.iat_upload_wav:
                upLoadWav();
                break;
            default:
                break;
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    /**
     * 上传联系人/词表监听器。
     */
    private LexiconListener mLexiconListener = new LexiconListener() {

        @Override
        public void onLexiconUpdated(String lexiconId, SpeechError error) {
            if (error != null) {
                showTip(error.toString());
            } else {
                showTip(getString(R.string.text_upload_success));
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if(mTranslateEnable && error.getErrorCode() == 14002) {
                showTip( error.getPlainDescription(true)+"\n请确认是否已开通翻译功能" );
            } else {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if( mTranslateEnable ){
                printTransResult( results );
            }else{
                printResult(results);
            }

            if (isLast) {
                // TODO 最后的结果
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        mResultText.setText(resultBuffer.toString());
        mResultText.setSelection(mResultText.length());
    }

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            if( mTranslateEnable ){
                printTransResult( results );
            }else{
                printResult(results);
            }

        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            if(mTranslateEnable && error.getErrorCode() == 14002) {
                showTip( error.getPlainDescription(true)+"\n请确认是否已开通翻译功能" );
            } else {
                showTip(error.getPlainDescription(true));
            }
        }

    };


    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        this.mTranslateEnable = mSharedPreferences.getBoolean( this.getString(R.string.pref_key_translate), false );
        if( mTranslateEnable ){
            Log.i( TAG, "translate enable" );
            mIat.setParameter( SpeechConstant.ASR_SCH, "1" );
            mIat.setParameter( SpeechConstant.ADD_CAP, "translate" );
            mIat.setParameter( SpeechConstant.TRS_SRC, "its" );
        }

        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            mIat.setParameter(SpeechConstant.ACCENT, null);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "en" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "cn" );
            }
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "cn" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "en" );
            }
        }
        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

    private void printTransResult (RecognizerResult results) {
        String trans  = JsonParser.parseTransResult(results.getResultString(),"dst");
        String oris = JsonParser.parseTransResult(results.getResultString(),"src");

        if( TextUtils.isEmpty(trans)||TextUtils.isEmpty(oris) ){
            showTip( "解析结果失败，请确认是否已开通翻译功能。" );
        }else{
            mResultText.setText( "原始语言:\n"+oris+"\n目标语言:\n"+trans );
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != mIat ){
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(SpeechActivity.this);
        FlowerCollector.onPageStart(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(SpeechActivity.this);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private void upLoadWav() {
        uploadText = mResultText.getText().toString();
        String SEND_FILE_PATH = Environment.getExternalStorageDirectory()+"/msc/iat.wav";
        try {
            accessFile = new RandomAccessFile(SEND_FILE_PATH, "r");
            long ac_length = accessFile.length();
            translationData = new byte[(int)ac_length];
            accessFile.read(translationData);
            audio_index = MApplication.getInstance().getAIN();
            MApplication.getInstance().newAudio();
            ifToSendAudio = true;
            Toast.makeText(SpeechActivity.this, "length is :" + translationData.length, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public class UdpAudioThread extends Thread {
        @Override
        public void run() {

            try {
                String ipValidation = Validation.validateIP(APP_IP);
                Log.d("AndroidUDP", "IP:" + ipValidation);

                InetAddress APP_ADD = InetAddress.getByName(APP_IP);
                s_socket_audio = new DatagramSocket();
                JSONObject send_object = new JSONObject();
                send_object.put("CHK", "pandora");
                send_object.put("LEN", "60000");
                send_object.put("ETT", 30);
                JSONObject send_time = new JSONObject();

                while (listenStatus) {
                    while (ifToSendAudio && tries_audio < MAXNUM) {
                        if (curDates.length == 3 && curTimes.length == 3) {
                            send_time.put("YEAR", curDates[0]);
                            send_time.put("MON", curDates[1]);
                            send_time.put("DAY", curDates[2]);
                            send_time.put("HOUR", curTimes[0]);
                            send_time.put("MIN", curTimes[1]);
                            send_time.put("SEC", curTimes[2]);
                        }
                        send_object.put("TIME", send_time);
                        send_object.put("AIN", MApplication.getInstance().getAIN());
                        send_object.put("TRAN", uploadText);

                        //each package with audio data length MAXBYTES
                        int audioLength = translationData.length;
                        int quo = audioLength / MAXBYTES;
                        int rem = audioLength % MAXBYTES;
                        int lastLen = rem == 0 ? MAXBYTES : rem;
                        int totalNumbers = quo + (rem == 0 ? 0 : 1);
                        send_object.put("TOT", totalNumbers);
                        for (int index = 1; index < totalNumbers; ++index ) {
                            send_object.put("INDEX", index);
                            byte[] bs = new byte[MAXBYTES];
                            System.arraycopy(translationData, MAXBYTES * (index-1), bs, 0, MAXBYTES);
                            String bS = Base64.encodeToString(bs, Base64.DEFAULT);
                            send_object.put("AUDIO", bS);
                            int len = send_object.toString().getBytes().length;
                            String lenString = String.format("%05d", len);
                            send_object.put("LEN", lenString);
                            String send_content = send_object.toString();
                            DatagramPacket dp_send_audio = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, 9992);
                            s_socket_audio.send(dp_send_audio);
                        }
                        send_object.put("INDEX", totalNumbers);
                        byte[] bs = new byte[lastLen];
                        System.arraycopy(translationData, MAXBYTES * (totalNumbers - 1), bs, 0, lastLen);
                        String bS = Base64.encodeToString(bs, Base64.DEFAULT);
                        send_object.put("AUDIO", bS);
                        int len = send_object.toString().getBytes().length;
                        String lenString = String.format("%05d", len);
                        send_object.put("LEN", lenString);

                        String send_content = send_object.toString();
                        DatagramPacket dp_send_audio = new DatagramPacket(send_content.getBytes(), send_content.getBytes().length, APP_ADD, 9992);
                        s_socket_audio.send(dp_send_audio);

                        tries_audio++;
                        //send a series of audio packages every 5 seconds
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SpeechAckEvent event) {
        if (event !=null) {
            boolean ack =event.isMsg_ack();
            if (ack) {
                ifToSendAudio = false;
                tries_audio = 0;
                Toast.makeText(SpeechActivity.this, "成功发送音频", Toast.LENGTH_LONG).show();
            }
        }
    }
}