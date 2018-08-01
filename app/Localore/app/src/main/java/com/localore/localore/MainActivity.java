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
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.User;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSignUp(View v) {
        User user = new User("Martin" + new Random().nextInt(1000));
        AppDatabase.getInstance(this).userDao().insert(user);
        LocaUtils.logDatabase(this);

        Log.i("_VIEW_", "SIGN UP");
    }

    public void onLogin(View v) {
        User user = AppDatabase.getInstance(this).userDao().loadAll().get(0);
        SessionControl.login(user.getId(), this);

        LocaUtils.logDatabase(this);

        Log.i("_VIEW_", "LOG IN");
        List<String> exerciseNames = ExerciseControl.exerciseNames(this);
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(this);
        Log.i("_VIEW_", "exercises: " + exerciseNames.toString());
        Log.i("_VIEW_", "exerciseProgress: " + exerciseProgresses.toString());
    }

    public void onLogout(View v) {
        SessionControl.logout(this);
        LocaUtils.logDatabase(this);

        Log.i("_VIEW_", "LOG OUT");
    }

    /**
     * Listener to create-exercise-action.
     * @param view
     */
    public void onCreateExercise(View view) {
        List<String> existingExerciseNames = ExerciseControl.exerciseNames(this);
        Log.i("_VIEW_", "CREATE EXERCISE");
        Log.i("_VIEW_", "Existing exercise names: " + existingExerciseNames.toString());

        Button b = (Button) view;
        b.setText("Loading");
        b.setEnabled(false);

        NodeShape workingArea = getWorkingArea();
        CreateExerciseService.start("My exercise" + new Random().nextInt(1000), workingArea, this);

        // listen to broadcasts from CreateExerciseService
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String status = intent.getStringExtra(CreateExerciseService.REPORT_KEY);
                        onCreateExerciseServiceDone(status);
                    }
                },
                new IntentFilter(CreateExerciseService.BROADCAST_ACTION)
        );
    }

    /**
     * Called with the CreateExerciseService is done.
     * @param report Network-error?
     */
    public void onCreateExerciseServiceDone(String report) {
        Button b = findViewById(R.id.doIt_button);
        b.setText(report);
        LocaUtils.logDatabase(this);

        Log.i("_VIEW_", "EXERCISE CREATED");
    }

    public void onDeleteExercise(View v) {
        Exercise exercise = AppDatabase.getInstance(this).exerciseDao().loadAll().get(0);
        ExerciseControl.deleteExercise(exercise, this);
        LocaUtils.logDatabase(this);

        Log.i("_VIEW_", "DELETE EXERCISE");
    }

    public void onEnterExercise(View v) {
        List<Exercise> exercises = AppDatabase.getInstance(this).exerciseDao().loadAll();
        Exercise exercise = exercises.get(new Random().nextInt(exercises.size()));
        SessionControl.enterExercise(exercise.getId(), this);
        LocaUtils.logDatabase(this);


        Log.i("_VIEW_", "ENTER EXERCISE");
        long exerciseId = SessionControl.loadExercise(this).getId();
        int progress = ExerciseControl.progressOfExercise(exerciseId, this);
        int requiredNoExerciseReminders = SessionControl.loadExercise(this).getRequiredGlobalReminders();
        Log.i("_VIEW_", "Progress: " + progress + ". Reminders: " + requiredNoExerciseReminders);

        //region quiz-category data
        int passed = ExerciseControl.noPassedLevelsInQuizCategory(QuizCategory.SETTLEMENTS, this);
        int tot = ExerciseControl.noLevelsInQuizCategory(QuizCategory.SETTLEMENTS, this);
        int rems = ExerciseControl.noRequiredQuizCategoryReminders(QuizCategory.SETTLEMENTS, this);
        Log.i("_VIEW_", "Settlements: " + passed + " / " + tot + ". Reminders: " + rems);

        passed = ExerciseControl.noPassedLevelsInQuizCategory(QuizCategory.ROADS, this);
        tot = ExerciseControl.noLevelsInQuizCategory(QuizCategory.ROADS, this);
        rems = ExerciseControl.noRequiredQuizCategoryReminders(QuizCategory.ROADS, this);
        Log.i("_VIEW_", "Settlements: " + passed + " / " + tot + ". Reminders: " + rems);

        passed = ExerciseControl.noPassedLevelsInQuizCategory(QuizCategory.NATURE, this);
        tot = ExerciseControl.noLevelsInQuizCategory(QuizCategory.NATURE, this);
        rems = ExerciseControl.noRequiredQuizCategoryReminders(QuizCategory.NATURE, this);
        Log.i("_VIEW_", "Settlements: " + passed + " / " + tot + ". Reminders: " + rems);

        passed = ExerciseControl.noPassedLevelsInQuizCategory(QuizCategory.TRANSPORT, this);
        tot = ExerciseControl.noLevelsInQuizCategory(QuizCategory.TRANSPORT, this);
        rems = ExerciseControl.noRequiredQuizCategoryReminders(QuizCategory.TRANSPORT, this);
        Log.i("_VIEW_", "Settlements: " + passed + " / " + tot + ". Reminders: " + rems);

        passed = ExerciseControl.noPassedLevelsInQuizCategory(QuizCategory.CONSTRUCTIONS, this);
        tot = ExerciseControl.noLevelsInQuizCategory(QuizCategory.CONSTRUCTIONS, this);
        rems = ExerciseControl.noRequiredQuizCategoryReminders(QuizCategory.CONSTRUCTIONS, this);
        Log.i("_VIEW_", "Settlements: " + passed + " / " + tot + ". Reminders: " + rems);
        //endregion
    }

    public void onLeaveExercise(View v) {
        SessionControl.leaveExercise(this);
        LocaUtils.logDatabase(this);

        Log.i("_VIEW_", "LEAVE EXERCISE");
        List<String> exerciseNames = ExerciseControl.exerciseNames(this);
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(this);
        Log.i("_VIEW_", "exercises: " + exerciseNames.toString());
        Log.i("_VIEW_", "exerciseProgress: " + exerciseProgresses.toString());
    }

    public void onTapping(View v) {
        //TODO
        Log.i("_VIEW_", "TAPPING");
    }

    public void onLevelQuiz(View v) {

    }

    public void onFollowUpQuiz(View v) {

    }

    public void onExerciseReminder(View v) {

    }

    public void onQuizCategoryReminder(View v) {

    }

    public void onReportQuestionResult(View v) {

    }

    public void onNextQuestion(View v) {

    }

    public void onFinishedRunningQuiz(View v) {

    }


    /**
     * @return Area of interest.
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
                new double[]{18.4603786,59.056049},
                new double[]{18.455658,59.0427181},
                new double[]{18.4662151,59.0455437},
                new double[]{18.4645844,59.0563581}));
    }
}
