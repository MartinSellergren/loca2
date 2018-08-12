package com.localore.localore;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Session;
import com.localore.localore.model.SessionDao;
import com.localore.localore.modelManipulation.RunningQuizControl;
import com.localore.localore.modelManipulation.SessionControl;

/**
 * A splash screen, displayed for some seconds, then starts relevant activity
 * (based on session in db).
 */
public class SplashActivity extends AppCompatActivity {
    private static int DISPLAY_TIME = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                AppDatabase db = AppDatabase.getInstance(SplashActivity.this);
                Session session = SessionControl.load(db);

                if (session.getUserId() == -1) {
                    //log-in / sign-up
                }
                else if (session.getLoadingExerciseStatus() != LoadingNewExerciseActivity.NOT_STARTED) {
                    LoadingNewExerciseActivity.resumedStart(SplashActivity.this);
                }
                else if (session.getExerciseId() == -1) {
                    SelectExerciseActivity.start(SplashActivity.this);
                }
                else if (RunningQuizControl.isCurrentlyRunning(SplashActivity.this)) {
                    QuizActivity.resumedStart(SplashActivity.this);
                }
                else {
                    ExerciseActivity.start(session.getExerciseId(), SplashActivity.this);
                }

                SplashActivity.this.finish();
            }

        }, DISPLAY_TIME);
    }
}
