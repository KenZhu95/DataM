package com.example.ken_z.datam;

import android.app.Service;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.provider.MediaStore;

import java.io.IOException;

public class BeeAndVibrateManager {
    private static boolean shouldPlayBeep = true;
    private static int count = 0;
    /**
     * @param context Context instance
     * @param milliseconds length of vibration, in milliseconds
     */

    public static void vibrate(Context context, long milliseconds) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        vibrator.vibrate(milliseconds);
    }

    public static void vibrate(Context context, long[] pattern, boolean isRepeat) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, isRepeat ? 1 : -1);
    }

    public static void playBee(final Context context, final int counts) {
        count = 0;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            shouldPlayBeep = false; // check if is silent mode
        }

        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                if (count < counts - 1) {
                    count++;
                    player.start();
                    //mediaPlayer.start();
                } else {
                    player.seekTo(0);
                    player.stop();
                }
            }
        });

        AssetFileDescriptor file = context.getResources().openRawResourceFd(R.raw.beep);
        try {
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            //mediaPlayer.setDataSource(file.getFileDescriptor(), 1000, 2000);
            file.close();
            mediaPlayer.setVolume(0, 1);
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            mediaPlayer = null;
        }

        if (shouldPlayBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }

    }

    public static void playWarn(final Context context, final int counts) {
        count = 0;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            shouldPlayBeep = false; // check if is silent mode
        }

        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                if (count < counts - 1) {
                    count++;
                    player.start();
                } else {
                    player.seekTo(0);
                    player.stop();
                }
            }
        });

        AssetFileDescriptor file = context.getResources().openRawResourceFd(R.raw.danger);
        try {
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            file.close();
            mediaPlayer.setVolume(0, 1);
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            mediaPlayer = null;
        }

        if (shouldPlayBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }

    }


    public static void playBeeAndVibrate(final Context context, long milliseconds, int counts) {
        //vibration
        vibrate(context, milliseconds);
        //bee voice warning
        playBee(context, counts);
    }

    public static void playWarnAndVibrate(final Context context, long milliseconds, int counts) {
        //vibration
        vibrate(context, milliseconds);
        //warning voice
        playWarn(context, counts);
    }
}
