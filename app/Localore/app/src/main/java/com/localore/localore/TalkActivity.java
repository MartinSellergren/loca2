package com.localore.localore;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class TalkActivity extends AppCompatActivity {

    private static final String TEXT_PARAM_KEY = "com.localore.localore.TalkActivity.TEXT_PARAM_KEY";
    private static final String NEXT_ACTIVITY_INTENT_PARAM_KEY = "com.localore.localore.TalkActivity.NEXT_ACTIVITY_INTENT_PARAM_KEY";
    private static final String TALK_TIME_PARAM_KEY = "com.localore.localore.TalkActivity.TALK_TIME_PARAM_KEY";

    /**
     * Default talk time.
     */
    private static final int DEFAULT_TALK_TIME = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        Intent intent = getIntent();
        String text = intent.getStringExtra(TEXT_PARAM_KEY);
        Intent nextActivityIntent = intent.getParcelableExtra(NEXT_ACTIVITY_INTENT_PARAM_KEY);
        int talkTime = intent.getIntExtra(TALK_TIME_PARAM_KEY, -1);

        TextView textView = findViewById(R.id.textView_talk);
        textView.setText(text);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LocaUtils.fadeInActivity(nextActivityIntent, TalkActivity.this);
            }
        }, talkTime);
    }

    /**
     * Start the activity with provided speech.
     * @param text
     * @param oldActivity
     */
    public static void start(String text, Intent nextActivityIntent, Activity oldActivity, int talkTime) {
        Intent intent = new Intent(oldActivity, TalkActivity.class);
        intent.putExtra(TEXT_PARAM_KEY, text);
        intent.putExtra(NEXT_ACTIVITY_INTENT_PARAM_KEY, nextActivityIntent);
        intent.putExtra(TALK_TIME_PARAM_KEY, talkTime);
        LocaUtils.fadeInActivity(intent, oldActivity);
    }
    public static void start(String text, Class<?> nextActivityClass, Activity oldActivity, int talkTime) {
        Intent intent = new Intent(oldActivity, nextActivityClass);
        start(text, intent, oldActivity, talkTime);
    }
    public static void start(String text, Intent nextActivityIntent, Activity oldActivity) {
        start(text, nextActivityIntent, oldActivity, DEFAULT_TALK_TIME);
    }
    public static void start(String text, Class<?> nextActivityClass, Activity oldActivity) {
        start(text, nextActivityClass, oldActivity, DEFAULT_TALK_TIME);
    }
}
