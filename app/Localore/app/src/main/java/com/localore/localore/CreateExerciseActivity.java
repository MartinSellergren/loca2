package com.localore.localore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.NodeShape;
import com.localore.localore.modelManipulation.SessionControl;

import java.util.List;

public class CreateExerciseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exercise);
    }

    public void onCreateExercise(View view) {
        Button b = (Button) view;
        b.setText("Loading");
        b.setEnabled(false);

        // listen to broadcasts from CreateExerciseService
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Long result = intent.getLongExtra(
                                CreateExerciseService.REPORT_KEY,
                                CreateExerciseService.UNSPECIFIED_ERROR);
                        onExerciseCreated(result);
                    }
                },
                new IntentFilter(CreateExerciseService.BROADCAST_ACTION)
        );

        String name = "My exercise" + LocaUtils.randi(1000);
        NodeShape workingArea = LocaUtils.getWorkingArea();
        CreateExerciseService.start("My exercise" + LocaUtils.randi(1000), workingArea, this);
    }

    public void onExerciseCreated(final long result) {
        Button b = findViewById(R.id.button_createExercise);
        b.setText(result + "");

        if (result < 0) return;

        Button b2 = findViewById(R.id.button_enterCreatedExercise);
        b2.setVisibility(View.VISIBLE);

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppDatabase db = AppDatabase.getInstance(CreateExerciseActivity.this);
                SessionControl.enterExercise(result, db);
                Intent intent = new Intent(CreateExerciseActivity.this, ExerciseActivity.class);
                startActivity(intent);
            }
        });
    }
}
