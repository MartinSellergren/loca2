package com.localore.localore;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Session;
import com.localore.localore.model.SessionDao;
import com.localore.localore.modelManipulation.SessionControl;

/**
 * A splash screen, displayed for some seconds, then starts relevant activity
 * (based on session in db).
 */
public class SplashActivity extends AppCompatActivity {
    private static int DISPLAY_TIME = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                Session session = SessionControl.load(AppDatabase.getInstance(SplashActivity.this));

                if (session.getUserId() == -1) {
                    //log-in / sign-up
                }
                else if (session.getLoadingExerciseStatus() != LoadingNewExerciseActivity.NOT_STARTED) {
                    startLoadingNewExerciseActivity();
                }
                else if (session.getExerciseId() == -1) {
                    startSelectExerciseActivity();
                }
                else {
                    //startExerciseActivity();
                    startSelectExerciseActivity();
                }
            }
        }, DISPLAY_TIME);
    }

    private void startSelectExerciseActivity() {
        Intent intent = new Intent(this, SelectExerciseActivity.class);
        startActivity(intent);
    }

    private void startExerciseActivity() {
        Intent intent = new Intent(this, ExerciseActivity.class);
        startActivity(intent);
    }

    private void startLoadingNewExerciseActivity() {
        Intent intent = new Intent(this, LoadingNewExerciseActivity.class);
        startActivity(intent);
    }
}
