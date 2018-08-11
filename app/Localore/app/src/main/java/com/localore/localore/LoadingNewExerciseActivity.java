package com.localore.localore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.Session;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;

/**
 * Singleton-activity that handles loading of a new exercise.
 *
 * Before freshStart:
 *  - New exercise name and working-area stored in Session (in db).
 *  - Set loading-exercise's not-started-status in Session for a fresh freshStart of the loading-process.
 *
 *  Before leave:
 *   - Set exercise-creation's not-started-status in Session (restarted).
 */
public class LoadingNewExerciseActivity extends AppCompatActivity {

    private TextView textView_loadingStatus;
    private Button button_enterOrRetry;
    private ImageButton button_exitExerciseLoading;

    //region status-codes
    public static final int NOT_STARTED = 0;
    public static final int RUNNING = 1;
    public static final int COMPLETED = 2;
    public static final int UNSPECIFIED_ERROR = -1;
    public static final int NETWORK_ERROR = -2;
    public static final int TOO_FEW_GEO_OBJECTS_ERROR = -3;
    public static final int DEAD_SERVICE_ERROR = -4;
    public static final int INTERRUPTED_ERROR = -5;
    //endregion

    /**
     * Starts the activity and begins loading an new exercise.
     * @param name
     * @param workingArea
     */
    public static void freshStart(String name, NodeShape workingArea, Context context) {
        SessionControl.initLoadingOfNewExercise(name, workingArea, AppDatabase.getInstance(context));
        Intent intent = new Intent(context, LoadingNewExerciseActivity.class);
        context.startActivity(intent);
    }

    /**
     * Starts the activity and resumes construction from a previous state.
     * Possibilities:
     * - The construction service is running.
     * - The construction service is completed.
     * - The construction service has failed.
     *
     * @pre Loading-status in exercise != NOT_STARTED.
     *
     * @param context
     */
    public static void resumedStart(Context context) {
        Intent intent = new Intent(context, LoadingNewExerciseActivity.class);
        context.startActivity(intent);
    }

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_new_exercise);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        this.textView_loadingStatus = findViewById(R.id.textView_loadingStatus);
        this.button_enterOrRetry = findViewById(R.id.button_enterOrRetry);
        this.button_exitExerciseLoading = findViewById(R.id.button_exitExerciseLoading);

        // listen to broadcasts from CreateExerciseService
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        statusBasedUpdate();
                    }
                },
                new IntentFilter(CreateExerciseService.BROADCAST_ACTION)
        );

//        validateLoadingExerciseStatus();
//        statusBasedUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        NotificationManagerCompat.from(this).cancel(CreateExerciseService.FINAL_NOTIFICATION_ID);
        validateLoadingExerciseStatus();
        statusBasedUpdate();
    }


    /**
     * If Session-status says Working, but service isn't running, the service has been
     * prematurely destroyed. If so, Session-status is set to an error.
     */
    private void validateLoadingExerciseStatus() {
        AppDatabase db = AppDatabase.getInstance(this);
        int status = SessionControl.load(db).getLoadingExerciseStatus();

        if (status == RUNNING && !createExerciseServiceIsRunning())
            SessionControl.updateLoadingExerciseStatus(DEAD_SERVICE_ERROR, db);
    }

    /**
     * @return True if the create-exercise-service is running.
     */
    private boolean createExerciseServiceIsRunning() {
        return CreateExerciseService.start(null, null, this) != null;
    }

    /**
     * Delegates layout-work etc. based on current status of construction.
     * Status read from Session in db.
     *
     * If error-status: also cleans up database by wiping the new data.
     */
    private void statusBasedUpdate() {
        Session session = AppDatabase.getInstance(this).sessionDao().load();
        int status = session.getLoadingExerciseStatus();

        if (status == NOT_STARTED) {
            String name = session.getLoadingExerciseName();
            NodeShape workingArea = session.getLoadingExerciseWorkingArea();
            SessionControl.updateLoadingExerciseStatus(RUNNING, AppDatabase.getInstance(this));

            CreateExerciseService.start(name, workingArea, this);
            runningLayout();
        }
        else if (status == RUNNING) {
            runningLayout();
        }
        else if (status == COMPLETED) {
            completedLayout();
        }
        else if (status == INTERRUPTED_ERROR) {
            //handled on exit-button-click
        }
        else {
            ExerciseControl.wipeConstruction(session.getExerciseId(), this);
            errorLayout(status);
        }
    }

    private void runningLayout() {
        this.textView_loadingStatus.setText(R.string.loading_new_exercise_HEADS_UP);
        this.button_enterOrRetry.setVisibility(View.INVISIBLE);
    }

    private void completedLayout() {
        this.textView_loadingStatus.setText(R.string.completed_loading_of_new_exercise_HEADS_UP);
        this.button_enterOrRetry.setText(R.string.enter_exercise);
        this.button_enterOrRetry.setVisibility(View.VISIBLE);
        this.button_exitExerciseLoading.setVisibility(View.INVISIBLE);
    }

    private void errorLayout(int status) {
        this.button_enterOrRetry.setText(R.string.retry);
        this.button_enterOrRetry.setVisibility(View.VISIBLE);

        if (status == UNSPECIFIED_ERROR)
            this.textView_loadingStatus.setText(R.string.exercice_loading_unspecified_error_HEADS_UP);
        else if (status == NETWORK_ERROR)
            this.textView_loadingStatus.setText(R.string.exercice_loading_network_error_HEADS_UP);
        else if (status == TOO_FEW_GEO_OBJECTS_ERROR)
            this.textView_loadingStatus.setText(R.string.exercice_loading_too_few_geo_objects_error_HEADS_UP);
        else if (status == DEAD_SERVICE_ERROR)
            this.textView_loadingStatus.setText(R.string.exercice_loading_dead_service_error_HEADS_UP);
        else
            throw new RuntimeException("Dead-end");
    }

    /**
     * Take action depending on status in session.
     * - Enter exercise: Exercise loading is complete. Leave loading-activity and enter exercise.
     * - Retry loading exercise: Error loading exercise, now retry.
     * @param view
     */
    public void onEnterExerciseOrRetryConstruction(View view) {
        Session session = AppDatabase.getInstance(this).sessionDao().load();
        int status = session.getLoadingExerciseStatus();

        if (status == COMPLETED) { //enter exercise
            boolean successful = true;
            leave(successful);
        }
        else if (status < 0) { //retry construction
            killService();
            ExerciseControl.wipeConstruction(session.getExerciseId(), this);

            String name = session.getLoadingExerciseName();
            NodeShape workingArea = session.getLoadingExerciseWorkingArea();
            LoadingNewExerciseActivity.freshStart(name, workingArea, this);
        }
    }

    /**
     * User-action: exit exercise loading.
     * Clean up and go to select-exercise-activity.
     * @param view
     */
    public void onExitButton(View view) {
        killService();
        boolean successful = false;
        leave(successful);
    }

    /**
     * Service cleans up construction and exits.
     */
    public void killService() {
        ExerciseControl.interruptAcquisition = true;
    }

    /**
     * All way out of this activity goes through this method.
     * Cleans-up and starts new activity.
     *
     * @param successful True if exercise loaded according to plan.
     */
    public void leave(boolean successful) {
        SessionControl.finalizeLoadingOfNewExercise(AppDatabase.getInstance(this));
        NotificationManagerCompat.from(this).cancel(CreateExerciseService.FINAL_NOTIFICATION_ID);

        if (successful) {
            ExerciseControl.wipeConstructionJunk(this);
            Intent intent = new Intent(this, ExerciseActivity.class);
            startActivity(intent);
        }
        else {
            AppDatabase db = AppDatabase.getInstance(this);
            long exerciseId = SessionControl.load(db).getExerciseId();
            ExerciseControl.wipeConstruction(exerciseId, this);

            SessionControl.setNoActiveExercise(AppDatabase.getInstance(this));
            CreateExerciseActivity.start(this);
        }

        finish();
    }

    /**
     * Quit app on back-press.
     */
    @Override
    public void onBackPressed() {
        LocaUtils.quitSecondTime(this);
    }
}
