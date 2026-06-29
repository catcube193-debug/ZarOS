package com.zaros.app.backend;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Locale;

/**
 * ZarOS ClockService — background service (no foreground required)
 * Keeps timer/stopwatch running when ClockActivity is closed.
 */
public class ClockService extends Service {

    private final IBinder binder = new ClockBinder();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean swRunning    = false;
    private long    swElapsed    = 0;
    private long    swStartTime  = 0;
    private boolean timerRunning = false;
    private long    timerTotal   = 0;
    private long    timerLeft    = 0;
    private long    timerStart   = 0;
    private Callback callback;

    public interface Callback {
        void onSwTick(long elapsed);
        void onTimerTick(long left);
        void onTimerDone();
    }

    public class ClockBinder extends Binder {
        public ClockService getService() { return ClockService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler.post(tickRunnable);
    }

    private final Runnable tickRunnable = new Runnable() {
        @Override public void run() {
            if (swRunning) {
                swElapsed = System.currentTimeMillis() - swStartTime;
                if (callback != null) callback.onSwTick(swElapsed);
            }
            if (timerRunning) {
                timerLeft = timerTotal - (System.currentTimeMillis() - timerStart);
                if (timerLeft <= 0) {
                    timerLeft = 0; timerRunning = false;
                    SoundManager.playNotif(ClockService.this);
                    if (callback != null) callback.onTimerDone();
                } else {
                    if (callback != null) callback.onTimerTick(timerLeft);
                }
            }
            handler.postDelayed(this, 100);
        }
    };

    public void startStopwatch()   { swStartTime = System.currentTimeMillis() - swElapsed; swRunning = true; }
    public void stopStopwatch()    { swRunning = false; }
    public void resetStopwatch()   { swRunning = false; swElapsed = 0; }
    public long getSwElapsed()     { return swElapsed; }
    public boolean isSwRunning()   { return swRunning; }

    public void startTimer(long ms){ timerTotal = ms; timerLeft = ms; timerStart = System.currentTimeMillis(); timerRunning = true; }
    public void stopTimer()        { timerRunning = false; }
    public void resetTimer()       { timerRunning = false; timerLeft = 0; timerTotal = 0; }
    public long getTimerLeft()     { return timerLeft; }
    public boolean isTimerRunning(){ return timerRunning; }
    public void setCallback(Callback cb) { this.callback = cb; }

    @Override public IBinder onBind(Intent intent) { return binder; }
    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public void onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null); }
}
