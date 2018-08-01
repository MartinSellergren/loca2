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
import com.localore.localore.model.Question;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.model.Session;
import com.localore.localore.model.User;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.RunningQuizControl;
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
        List<Exercise> exercises = AppDatabase.getInstance(this).exerciseDao()
                .loadWithUserOrderedByDisplayIndex(user.getId());
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(user.getId(), this);
        Log.i("_VIEW_", "exercises: " + exercises.toString());
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
        long userId = SessionControl.load(this).getUserId();
        List<String> existingExerciseNames =
                AppDatabase.getInstance(this).exerciseDao()
                        .loadNamesWithUser(userId);

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

        //region quiz-categories data

        List<int[]> quizCategoriesData = ExerciseControl.loadQuizCategoriesData(exerciseId, this);
        for (int i = 0; i < quizCategoriesData.size(); i++) {
            int[] quizCategoryData = quizCategoriesData.get(i);
            Log.i("_VIEW_", "Quiz-category: " + QuizCategory.types[i]);
            Log.i("_VIEW_", "  no levels: " + quizCategoryData[0]);
            Log.i("_VIEW_", "  no passed levels: " + quizCategoryData[1]);
            Log.i("_VIEW_", "  no reminders: " + quizCategoryData[2]);
        }

        //endregion
    }

    public void onLeaveExercise(View v) {
        SessionControl.leaveExercise(this);
        LocaUtils.logDatabase(this);

        long userId = SessionControl.load(this).getUserId();
        Log.i("_VIEW_", "LEAVE EXERCISE");
        List<Exercise> exercises = AppDatabase.getInstance(this).exerciseDao()
                .loadWithUserOrderedByDisplayIndex(userId);
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(userId, this);
        Log.i("_VIEW_", "exercises: " + exercises.toString());
        Log.i("_VIEW_", "exerciseProgress: " + exerciseProgresses.toString());
    }

    public void onTapping(View v) {
        //TODO
        Log.i("_VIEW_", "TAPPING");
    }

    //region start level quiz

    public void onLevelQuiz_settlements(View v) {
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.SETTLEMENTS,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW LEVEL QUIZ: SETTLEMENTS");
    }
    public void onLevelQuiz_roads(View v) {
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.ROADS,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW LEVEL QUIZ: ROADS");
    }
    public void onLevelQuiz_nature(View v) {
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.NATURE,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW LEVEL QUIZ: NATURE");
    }
    public void onLevelQuiz_transport(View v) {
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.TRANSPORT,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW LEVEL QUIZ: TRANSPORT");
    }
    public void onLevelQuiz_constructions(View v) {
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.CONSTRUCTIONS,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW LEVEL QUIZ: CONSTRUCTIONS");
    }

    //endregion

    //region start quiz-reminder
    public void onLevelCategoryReminder_settlements(View v) {
        RunningQuizControl.newLevelReminder(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.SETTLEMENTS,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW QUIZ-CATEGORY-REMINDER: SETTLEMENTS");
    }
    public void onLevelCategoryReminder_roads(View v) {
        RunningQuizControl.newLevelReminder(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.ROADS,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW QUIZ-CATEGORY-REMINDER: ROADS");
    }
    public void onLevelCategoryReminder_nature(View v) {
        RunningQuizControl.newLevelReminder(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.NATURE,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW QUIZ-CATEGORY-REMINDER: NATURE");
    }
    public void onLevelCategoryReminder_transport(View v) {
        RunningQuizControl.newLevelReminder(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.TRANSPORT,
                this);
    }
    public void onLevelCategoryReminder_constructions(View v) {
        RunningQuizControl.newLevelReminder(
                SessionControl.load(this).getExerciseId(),
                QuizCategory.CONSTRUCTIONS,
                this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW QUIZ-CATEGORY-REMINDER: CONSTRUCTIONS");
    }
    //endregion

    public void onFollowUpQuiz(View v) {
        RunningQuizControl.newFollowUpQuiz(this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW FOLLOW-UP QUIZ");
    }

    public void onExerciseReminder(View v) {
        RunningQuizControl.newExerciseReminder(SessionControl.load(this).getExerciseId(), this);
        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEW EXERCISE REMINDER");
    }

    public void onReportQuestionResult_correct(View v) {
        RunningQuiz runningQuiz = RunningQuizControl.load(this);
        int questionIndex = runningQuiz.getCurrentQuestionIndex();
        Question question = AppDatabase.getInstance(this).questionDao()
                .loadWithRunningQuizAndIndex(runningQuiz.getId(), questionIndex);
        RunningQuizControl.reportQuestionResult(question, true, this);

        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "REPORT QUESTION: CORRECT");
    }

    public void onReportQuestionResult_incorrect(View v) {
        RunningQuiz runningQuiz = RunningQuizControl.load(this);
        int questionIndex = runningQuiz.getCurrentQuestionIndex();
        Question question = AppDatabase.getInstance(this).questionDao()
                .loadWithRunningQuizAndIndex(runningQuiz.getId(), questionIndex);
        RunningQuizControl.reportQuestionResult(question, false, this);

        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "REPORT QUESTION: INCORRECT");
    }

    public void onNextQuestion(View v) {
        Question question = RunningQuizControl.nextQuestion(this);

        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "NEXT QUESTION");
        Log.i("_VIEW_", question.toString());
    }

    public void onFinishedRunningQuiz(View v) {
        Exercise exercise = SessionControl.loadExercise(this);
        RunningQuizControl.onFinishedRunningQuiz(exercise, this);

        LocaUtils.logDatabase(this);
        Log.i("_VIEW_", "FINISHED QUIZ");
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
