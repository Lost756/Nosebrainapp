package com.example.nosebrainapp.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public class TimerHelper {
    private Handler handler = new Handler(Looper.getMainLooper());
    private long startTime = 0;
    private boolean isRunning = false;
    private double elapsedSeconds = 0;
    private double timeLimit = 0;
    private OnTickListener listener;

    public interface OnTickListener {
        void onTick(double seconds);
        void onTimeLimitReached();
    }

    public TimerHelper(OnTickListener listener) {
        this.listener = listener;
    }

    public void start(double limit) {
        if (isRunning) return;
        this.timeLimit = limit;
        startTime = SystemClock.elapsedRealtime() - (long)(elapsedSeconds * 1000);
        isRunning = true;
        handler.post(updateRunnable);
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        handler.removeCallbacks(updateRunnable);
        elapsedSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0;
    }

    public void reset() {
        stop();
        elapsedSeconds = 0;
        startTime = 0;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            elapsedSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0;
            listener.onTick(elapsedSeconds);

            if (elapsedSeconds >= timeLimit) {
                stop();
                listener.onTimeLimitReached();
                return;
            }
            handler.postDelayed(this, 50);
        }
    };
}