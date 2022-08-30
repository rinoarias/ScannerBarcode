package com.rinoarias.scannerbarcode.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

import com.rinoarias.scannerbarcode.R;

/**
 * Manages beeps and vibrations.
 */
public final class BeepManager {

    private static final String TAG = BeepManager.class.getSimpleName();

    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;

    private final Context context;

    private boolean beepEnabled = true;
    private boolean vibrateEnabled = false;

    public BeepManager(Activity activity) {
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // We do not keep a reference to the Activity itself, to prevent leaks
        this.context = activity.getApplicationContext();
    }

    public boolean isBeepEnabled() {
        return beepEnabled;
    }

    /**
     * Call updatePrefs() after setting this.
     *
     * If the device is in silent mode, it will not beep.
     *
     * @param beepEnabled true to enable beep
     */
    public void setBeepEnabled(boolean beepEnabled) {
        this.beepEnabled = beepEnabled;
    }

    public boolean isVibrateEnabled() {
        return vibrateEnabled;
    }

    /**
     * Call updatePrefs() after setting this.
     *
     * @param vibrateEnabled true to enable vibrate
     */
    public void setVibrateEnabled(boolean vibrateEnabled) {
        this.vibrateEnabled = vibrateEnabled;
    }

    @SuppressLint("MissingPermission")
    public synchronized void playBeepSoundAndVibrate() {
        if (beepEnabled) {
            playBeepSound();
        }
        if (vibrateEnabled) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(VIBRATE_DURATION);
            }
        }
    }


    public MediaPlayer playBeepSound() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= 21) {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC).build());
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        mediaPlayer.setOnCompletionListener(mp -> {
            mp.stop();
            mp.reset();
            mp.release();
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.w(TAG, "Failed to beep " + what + ", " + extra);
            // possibly media player error, so release and recreate
            mp.stop();
            mp.reset();
            mp.release();
            return true;
        });
        try {
            AssetFileDescriptor file = context.getResources().openRawResourceFd(R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return mediaPlayer;
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer.reset();
            mediaPlayer.release();
            return null;
        }
    }
}