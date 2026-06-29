package com.zaros.app.backend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import com.zaros.app.R;

/**
 * ZarOS AlarmReceiver (Java backend)
 * Fires when a ZarOS alarm goes off.
 * Plays the bundled ZarOS ringtone and vibrates.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_ALARM = "com.zaros.app.ALARM";
    public static final String EXTRA_LABEL  = "alarm_label";

    @Override
    public void onReceive(Context context, Intent intent) {
        String label = intent.getStringExtra(EXTRA_LABEL);
        if (label == null) label = "ZarOS Alarm";

        Toast.makeText(context, "⏰ " + label, Toast.LENGTH_LONG).show();

        // Play the bundled ZarOS ringtone for the alarm
        try {
            final MediaPlayer mp = MediaPlayer.create(context, R.raw.ringtone_default);
            if (mp != null) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
                mp.setLooping(true);
                mp.start();
                // Stop and release after 30 seconds — MediaPlayer has no
                // postDelayed of its own, so use a Handler on the main looper.
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (mp.isPlaying()) mp.stop();
                    mp.release();
                }, 30_000);
            } else {
                SoundManager.playNotif(context);
            }
        } catch (Exception e) {
            SoundManager.playNotif(context);
        }

        // Vibrate pattern
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            long[] pattern = {0, 500, 300, 500, 300, 500};
            v.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
    }
}
