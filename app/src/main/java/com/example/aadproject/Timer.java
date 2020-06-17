package com.example.aadproject;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

public class Timer {
    Handler timerHandler = new Handler();
    Runnable timerRunnable;
    long startTime;
    long currentTime;
    TextView timerTextView;
    long durationBeforePause=0;
    long pauseTime;

    Timer(long startTime, TextView timerTextView){
        this.startTime = startTime;
        this.timerTextView = timerTextView;
    }

    public void startTimer(){
        timerRunnable = new Runnable() {

            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime + durationBeforePause;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                timerTextView.setText(String.format("%d:%02d", minutes, seconds));

                timerHandler.postDelayed(this, 500);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void stopTimer(){
        timerHandler.removeCallbacks(timerRunnable);
    }
    public void pauseTimer(){
        stopTimer();
        pauseTime=System.currentTimeMillis();
        durationBeforePause+=pauseTime-startTime;
        Log.d("pauseTimer",checkTime(durationBeforePause));
    }

    public void resumeTimer(){
        startTime = System.currentTimeMillis();
        startTimer();
        Log.d("resumeTimer",checkTime(startTime));
    }

    public String checkTime(long millis){
        millis = System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String time = (String.format("%d:%02d", minutes, seconds));
        return time;

    }

}
