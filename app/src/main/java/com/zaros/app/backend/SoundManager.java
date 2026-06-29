package com.zaros.app.backend;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.zaros.app.R;

/**
 * ZarOS SoundManager (Java backend)
 *
 * Uses SoundPool with USAGE_MEDIA so it plays through the MUSIC stream,
 * completely separate from system UI sounds. This prevents double-playing
 * with Android's own touch/unlock sounds.
 *
 * IMPORTANT: The app theme uses Theme.ZarOS which must have
 * android:soundEffectsEnabled="false" to silence system touch sounds.
 */
public class SoundManager {

    public enum VibrationPattern { TAP, UNLOCK, NOTIFICATION, ERROR }

    private static SoundPool soundPool;
    private static int idUnlock    = -1;
    private static int idTap       = -1;
    private static int idAppOpen   = -1;
    private static int idNotif     = -1;
    private static int idBoot      = -1;
    private static int idBackspace = -1;
    private static int idMessage   = -1;
    private static int idType      = -1;
    private static boolean ready   = false;

    public static void init(Context ctx) {
        if (ready) return;

        // Use USAGE_MEDIA + CONTENT_TYPE_MUSIC → plays on music stream, NOT system UI stream
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        try {
            idUnlock    = soundPool.load(ctx, R.raw.sound_unlock,    1);
            idTap       = soundPool.load(ctx, R.raw.sound_tap,       1);
            idAppOpen   = soundPool.load(ctx, R.raw.sound_appopen,   1);
            idNotif     = soundPool.load(ctx, R.raw.sound_notif,     1);
            idBoot      = soundPool.load(ctx, R.raw.sound_boot,      1);
            idBackspace = soundPool.load(ctx, R.raw.sound_backspace,  1);
            idMessage   = soundPool.load(ctx, R.raw.sound_message,   1);
            idType      = soundPool.load(ctx, R.raw.sound_type,      1);
        } catch (Exception e) {
            Log.e("SoundManager", "Failed to load sounds: " + e.getMessage());
        }

        ready = true;
    }

    public static void playUnlock(Context ctx)    { play(ctx, idUnlock,    0.75f); vibrate(ctx, VibrationPattern.UNLOCK); }
    public static void playTap(Context ctx)       { play(ctx, idTap,       0.5f);  vibrate(ctx, VibrationPattern.TAP); }
    public static void playAppOpen(Context ctx)   { play(ctx, idAppOpen,   0.6f); }
    public static void playNotif(Context ctx)     { play(ctx, idNotif,     0.7f);  vibrate(ctx, VibrationPattern.NOTIFICATION); }
    public static void playBoot(Context ctx)      { play(ctx, idBoot,      0.85f); }
    public static void playBackspace(Context ctx) { play(ctx, idBackspace, 0.5f); }
    public static void playMessage(Context ctx)   { play(ctx, idMessage,   0.7f);  vibrate(ctx, VibrationPattern.NOTIFICATION); }
    public static void playType(Context ctx)      { play(ctx, idType,      0.35f); }

    private static void play(Context ctx, int soundId, float vol) {
        if (soundPool == null || soundId <= 0) return;
        // Respect user volume setting
        float userVol = ZarOSPrefs.getVolume(ctx) / 100f;
        float finalVol = vol * userVol;
        soundPool.play(soundId, finalVol, finalVol, 1, 0, 1.0f);
    }

    public static void vibrate(Context ctx, VibrationPattern pattern) {
        if (!ZarOSPrefs.isVibration(ctx)) return;
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        VibrationEffect effect;
        switch (pattern) {
            case UNLOCK:
                effect = VibrationEffect.createWaveform(new long[]{0, 30, 60, 30}, -1);
                break;
            case NOTIFICATION:
                effect = VibrationEffect.createWaveform(new long[]{0, 50, 100, 50}, -1);
                break;
            case ERROR:
                effect = VibrationEffect.createWaveform(new long[]{0, 80, 40, 80}, -1);
                break;
            default:
                effect = VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE);
                break;
        }
        v.vibrate(effect);
    }

    public static void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            ready = false;
        }
    }
}
