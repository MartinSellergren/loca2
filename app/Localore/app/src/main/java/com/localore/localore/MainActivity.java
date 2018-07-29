package com.localore.localore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.User;
import com.localore.localore.modelManipulation.ExerciseControl;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // listen to broadcasts from CreateExerciseService
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String status = intent.getStringExtra(CreateExerciseService.REPORT_KEY);
                        onCreateExerciseServiceDone(status);
                    }
                },
                new IntentFilter(CreateExerciseService.BROADCAST_ACTION));
    }

    /**
     * Listener to create-exercise-action.
     * @param view
     */
    public void onCreateExercise(View view) {
        Button b = (Button) view;
        b.setText("Loading");
        b.setEnabled(false);

        User user = new User("Martin");
        long userId = AppDatabase.getInstance(this).userDao().insert(user);

        NodeShape workingArea = getWorkingArea();
        CreateExerciseService.start(userId, "My exercise", workingArea, this);
        CreateExerciseService.start(userId, "My exercise2", workingArea, this);
        CreateExerciseService.start(userId, "My exercise3", workingArea, this);
    }

    /**
     * Called with the CreateExerciseService is done.
     * @param report Network-error?
     */
    public void onCreateExerciseServiceDone(String report) {
        if (AppDatabase.getInstance(this).exerciseDao().size() == 3) {
            Button b = findViewById(R.id.doIt_button);
            b.setText(report);
            Exercise exercise = AppDatabase.getInstance(this).exerciseDao().loadWithDisplayIndex(1);
            ExerciseControl.deleteExercise(exercise, this);
            LocaUtils.logDatabase(this);
        }
    }

    /**
     * @return Area of interest. A closed shape.
     */
    private NodeShape getWorkingArea() {
        //uppsala
//         double w = 17.558212280273438;
//         double s = 59.78301472732963;
//         double e = 17.731246948242188;
//         double n = 59.91097597079679;

        //mefjärd
//        double w = 18.460774;
//        double s = 58.958251;
//        double e = 18.619389;
//        double n = 59.080544;

        //lidingö
        // double w = 18.08246612548828;
        // double s = 59.33564087770051;
        // double e = 18.27404022216797;
        // double n = 59.39407306645033;

        //rudboda
//         double w = 18.15;
//         double s = 59.372;
//         double e = 18.19;
//         double n = 59.383;

        //new york
        // double w = -74.016259;
        // double s = 40.717569;
        // double e = -73.972399;
        // double n = 40.737473;


//        return new NodeShape(Arrays.asList(
//                new double[]{w, s},
//                new double[]{w, n},
//                new double[]{e, n},
//                new double[]{e, s}));

        //sandböte
        return new NodeShape(Arrays.asList(
                new double[]{18.4603786,59.0560492},
                new double[]{18.455658,59.0427181},
                new double[]{18.4662151,59.0455437},
                new double[]{18.4645844,59.0563581}));
    }
}
