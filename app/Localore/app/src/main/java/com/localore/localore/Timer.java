package com.localore.localore;

import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class Timer {

    private ImageView timerImage;
    private TextView timerText;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private int timeLeft;
    private Runnable onStop;

    public Timer(ImageView timerImage, TextView timerText) {
        this.timerImage = timerImage;
        this.timerText = timerText;

        timerHandler = new Handler();
        timerRunnable = () -> {
            timeLeft -= 1;
            timerText.setText(timeLeft + "");

            if (timeLeft > 0) timerHandler.postDelayed(timerRunnable, 1000);
            else {
                try {
                    onStop.run();
                }
                catch (Exception e) {}
            }
        };
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
        timerText.setText(timeLeft + "");
    }
    public int getTimeLeft() {
        return this.timeLeft;
    }

    /**
     * Show the timer.
     */
    public void show() {
        timerImage.setVisibility(View.VISIBLE);
        timerText.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the timer and stops counting down (no onStop-action).
     */
    public void hide() {
        timerImage.setVisibility(View.INVISIBLE);
        timerText.setVisibility(View.INVISIBLE);
        timerHandler.removeCallbacks(timerRunnable);
    }

    /**
     * @param time Time in seconds before timer runs out.
     * @param onStop What to do when runs out.
     */
    public void start(int time, Runnable onStop) {
        if (timerText.getVisibility() == View.INVISIBLE) return;

        this.timeLeft = time + 1;
        this.onStop = onStop;
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void stop() {
        timerHandler.removeCallbacks(timerRunnable);
    }
}
