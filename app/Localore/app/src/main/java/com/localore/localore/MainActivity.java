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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSignUp(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        User user = new User("Martin" + LocaUtils.randi(1000));
        db.userDao().insert(user);

        Log.i("<VIEW>", "SIGN UP");
        LocaUtils.logDatabase(db);
    }

    public void onLogin(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        User user = db.userDao().loadAll().get(0);
        SessionControl.login(user.getId(), db);

        Log.i("<VIEW>", "LOG IN");
        List<Exercise> exercises = db.exerciseDao().loadWithUserOrderedByDisplayIndex(user.getId());
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(user.getId(), db);
        Log.i("<VIEW>", "exercises: " + exercises.toString());
        Log.i("<VIEW>", "exerciseProgress: " + exerciseProgresses.toString());

        LocaUtils.logDatabase(db);
    }

    public void onLogout(View v) {
        Log.i("<VIEW>", "LOG OUT");

        SessionControl.logout(AppDatabase.getInstance(this));
        LocaUtils.logDatabase(AppDatabase.getInstance(this));
    }

    /**
     * Listener to create-exercise-action.
     * @param view
     */
    public void onCreateExercise(View view) {
        AppDatabase db = AppDatabase.getInstance(this);
        long userId = SessionControl.load(db).getUserId();
        List<String> existingExerciseNames = db.exerciseDao().loadNamesWithUser(userId);
        Log.i("<VIEW>", "CREATE EXERCISE");
        Log.i("<VIEW>", "Existing exercise names: " + existingExerciseNames.toString());

        Button b = (Button) view;
        b.setText("Loading");
        b.setEnabled(false);

        NodeShape workingArea = LocaUtils.getWorkingArea();
        CreateExerciseService.start("My exercise" + LocaUtils.randi(1000), workingArea, this);

        // listen to broadcasts from CreateExerciseService
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Long result = intent.getLongExtra(CreateExerciseService.REPORT_KEY, -1);
                        onCreateExerciseServiceDone(result);
                    }
                },
                new IntentFilter(CreateExerciseService.BROADCAST_ACTION)
        );
    }

    /**
     * Called with the CreateExerciseService is done.
     * @param result Network-error?
     */
    public void onCreateExerciseServiceDone(long result) {
        Button b = findViewById(R.id.button4);
        b.setText(String.format("(%s) %s", result, "create exercise"));
        b.setEnabled(true);

        Log.i("<VIEW>", "EXERCISE CREATED");
        LocaUtils.logDatabase(AppDatabase.getInstance(this));
    }

    public void onDeleteExercise(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        List<Exercise> exercises = db.exerciseDao().loadAll();
        Exercise exercise = exercises.get( LocaUtils.randi(exercises.size()) );
        ExerciseControl.deleteExercise(exercise, db);

        Log.i("<VIEW>", "DELETE EXERCISE: " + exercise.getName());
        LocaUtils.logDatabase(db);
    }

    public void onEnterExercise(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        List<Exercise> exercises = db.exerciseDao().loadAll();
        Exercise exercise = exercises.get( LocaUtils.randi(exercises.size()) );
        SessionControl.enterExercise(exercise.getId(), db);

        Log.i("<VIEW>", "ENTER EXERCISE");
        long exerciseId = SessionControl.loadExercise(db).getId();
        int progress = ExerciseControl.progressOfExercise(exerciseId, db);
        int requiredNoExerciseReminders = SessionControl.loadExercise(db).getNoRequiredExerciseReminders();
        int noPassedLevels = ExerciseControl.loadPassedQuizzesInExercise(exerciseId, db).size();
        Log.i("<VIEW>", "Progress: " + progress + ". Reminders: " + requiredNoExerciseReminders + ". No passed: " + noPassedLevels);

        //region quiz-categories data

        List<int[]> quizCategoriesData = ExerciseControl.loadQuizCategoriesData(exerciseId, db);
        for (int i = 0; i < quizCategoriesData.size(); i++) {
            int[] quizCategoryData = quizCategoriesData.get(i);
            Log.i("<VIEW>", "Quiz-category: " + QuizCategory.types[ quizCategoryData[0] ]);
            Log.i("<VIEW>", "  no levels: " + quizCategoryData[1]);
            Log.i("<VIEW>", "  no passed levels: " + quizCategoryData[2]);
            Log.i("<VIEW>", "  no reminders: " + quizCategoryData[3]);
        }

        //endregion
        LocaUtils.logDatabase(db);
    }

    public void onLeaveExercise(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        SessionControl.leaveExercise(db);

        long userId = SessionControl.load(db).getUserId();
        Log.i("<VIEW>", "LEAVE EXERCISE");
        List<Exercise> exercises = db.exerciseDao().loadWithUserOrderedByDisplayIndex(userId);
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(userId, db);
        Log.i("<VIEW>", "exercises: " + exercises.toString());
        Log.i("<VIEW>", "exerciseProgress: " + exerciseProgresses.toString());

        LocaUtils.logDatabase(db);
    }

    public void onTapping_nextLevel(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(db);

        List<GeoObject> nextLevelGeoObjects = ExerciseControl.loadGeoObjectsForTapping(
                exercise.getId(), QuizCategory.NATURE, true, db);

        Log.i("<VIEW>", "TAPPING: next-level nature");
        Log.i("<VIEW>", nextLevelGeoObjects.toString());
    }

    public void onTapping_pastLevels(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(db);

        List<GeoObject> nextLevelGeoObjects = ExerciseControl.loadGeoObjectsForTapping(
                exercise.getId(), QuizCategory.NATURE, false, db);

        Log.i("<VIEW>", "TAPPING: past-levels nature");
        Log.i("<VIEW>", nextLevelGeoObjects.toString());
    }

    //region start level quiz

    public void onLevelQuiz_settlements(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.SETTLEMENTS,
                db);
        Log.i("<VIEW>", "NEW LEVEL QUIZ: SETTLEMENTS");
        LocaUtils.logDatabase(db);
    }
    public void onLevelQuiz_roads(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.ROADS,
                db);
        Log.i("<VIEW>", "NEW LEVEL QUIZ: ROADS");
        LocaUtils.logDatabase(db);
    }
    public void onLevelQuiz_nature(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.NATURE,
                db);
        int noQuestions = RunningQuizControl.noQuestions(db);
        Log.i("<VIEW>", "NEW LEVEL QUIZ: NATURE, " + noQuestions + " questions");
        LocaUtils.logDatabase(db);
    }
    public void onLevelQuiz_transport(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.TRANSPORT,
                db);
        int noQuestions = RunningQuizControl.noQuestions(db);
        Log.i("<VIEW>", "NEW LEVEL QUIZ: TRANSPORT, " + noQuestions + " questions");
        LocaUtils.logDatabase(db);
    }
    public void onLevelQuiz_constructions(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelQuiz(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.CONSTRUCTIONS,
                db);
        int noQuestions = RunningQuizControl.noQuestions(db);
        Log.i("<VIEW>", "NEW LEVEL QUIZ: CONSTRUCTIONS, " + noQuestions + " questions");
        LocaUtils.logDatabase(db);
    }

    //endregion

    //region start quiz-reminder
    public void onLevelCategoryReminder_settlements(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelReminder(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.SETTLEMENTS,
                db);
        Log.i("<VIEW>", "NEW QUIZ-CATEGORY-REMINDER: SETTLEMENTS");
        LocaUtils.logDatabase(db);
    }
    public void onLevelCategoryReminder_roads(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelReminder(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.ROADS,
                db);
        Log.i("<VIEW>", "NEW QUIZ-CATEGORY-REMINDER: ROADS");
        LocaUtils.logDatabase(db);
    }
    public void onLevelCategoryReminder_nature(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelReminder(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.NATURE,
                db);
        Log.i("<VIEW>", "NEW QUIZ-CATEGORY-REMINDER: NATURE");
        LocaUtils.logDatabase(db);
    }
    public void onLevelCategoryReminder_transport(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelReminder(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.TRANSPORT,
                db);
        Log.i("<VIEW>", "NEW QUIZ-CATEGORY-REMINDER: TRANSPORT");
        LocaUtils.logDatabase(db);
    }
    public void onLevelCategoryReminder_constructions(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newLevelReminder(
                SessionControl.load(db).getExerciseId(),
                QuizCategory.CONSTRUCTIONS,
                db);
        Log.i("<VIEW>", "NEW QUIZ-CATEGORY-REMINDER: CONSTRUCTIONS");
        LocaUtils.logDatabase(db);
    }
    //endregion

    public void onFollowUpQuiz(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newFollowUpQuiz(db);
        Log.i("<VIEW>", "NEW FOLLOW-UP QUIZ");
        LocaUtils.logDatabase(db);
    }

    public void onExerciseReminder(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.newExerciseReminder(SessionControl.load(db).getExerciseId(), db);
        Log.i("<VIEW>", "NEW EXERCISE REMINDER");
        LocaUtils.logDatabase(db);
    }

    public void onReportQuestionResult_correct(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuiz runningQuiz = RunningQuizControl.load(db);
        int questionIndex = runningQuiz.getCurrentQuestionIndex();
        Question question = db.questionDao()
                .loadWithRunningQuizAndIndex(runningQuiz.getId(), questionIndex);
        RunningQuizControl.reportQuestionResult(question, true, db);

        Log.i("<VIEW>", "REPORT QUESTION: CORRECT");
        LocaUtils.logDatabase(db);
    }

    public void onReportQuestionResult_incorrect(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuiz runningQuiz = RunningQuizControl.load(db);
        int questionIndex = runningQuiz.getCurrentQuestionIndex();
        Question question = db.questionDao()
                .loadWithRunningQuizAndIndex(runningQuiz.getId(), questionIndex);
        RunningQuizControl.reportQuestionResult(question, false, db);

        Log.i("<VIEW>", "REPORT QUESTION: INCORRECT");
        LocaUtils.logDatabase(db);
    }

    public void onNextQuestion(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        Question question = RunningQuizControl.nextQuestion(db);

        Log.i("<VIEW>", "NEXT QUESTION");
        if (question == null) Log.i("<VIEW>", "Done!");
        else {
            Log.i("<VIEW>", question.toString());
            Log.i("<VIEW>", question.getContent().toString());
        }
        LocaUtils.logDatabase(db);
    }

    public void onFinishedRunningQuiz(View v) {
        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(db);
        RunningQuizControl.onFinishedRunningQuiz(exercise, db);

        Log.i("<VIEW>", "FINISHED QUIZ");
        LocaUtils.logDatabase(db);
    }
}
