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

        NodeShape workingArea = getWorkingArea();
        CreateExerciseService.start("My exercise" + LocaUtils.randi(1000), workingArea, this);

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
        Button b = findViewById(R.id.button4);
        b.setText(String.format("(%s) %s", report, "create exercise"));
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

        //stockholm1
//        return new NodeShape(Arrays.asList(
//                new double[]{18.1015205, 59.3598395},
//                new double[]{18.0986881, 59.3616766},
//                new double[]{18.0858994, 59.3587896},
//                new double[]{18.0714798, 59.3566461},
//                new double[]{18.0508804, 59.3552461},
//                new double[]{18.0340576, 59.3512210},
//                new double[]{18.0258179, 59.3461451},
//                new double[]{18.0124283, 59.3463201},
//                new double[]{18.0072784, 59.3408934},
//                new double[]{18.0055618, 59.3344152},
//                new double[]{18.0055618, 59.3344152},
//                new double[]{18.0146599, 59.3302124},
//                new double[]{18.0146599, 59.3254837},
//                new double[]{18.0039310, 59.3274979},
//                new double[]{18.0016136, 59.3285925},
//                new double[]{17.9978371, 59.3260092},
//                new double[]{18.0001545, 59.3225060},
//                new double[]{18.0072784, 59.3212799},
//                new double[]{18.0135441, 59.3241701},
//                new double[]{18.0195522, 59.3246080},
//                new double[]{18.0253887, 59.3246517},
//                new double[]{18.0268478, 59.3273665},
//                new double[]{18.0419540, 59.3266222},
//                new double[]{18.0440140, 59.3324890},
//                new double[]{18.0483055, 59.3382672},
//                new double[]{18.0567169, 59.3396679},
//                new double[]{18.0589485, 59.3362537},
//                new double[]{18.0610943, 59.3335396},
//                new double[]{18.0626392, 59.3315258},
//                new double[]{18.0594635, 59.3266660},
//                new double[]{18.0586910, 59.3241263},
//                new double[]{18.0642700, 59.3220243},
//                new double[]{18.0792904, 59.3214550},
//                new double[]{18.0780029, 59.3266660},
//                new double[]{18.0826378, 59.3304313},
//                new double[]{18.0951691, 59.3314383},
//                new double[]{18.1066704, 59.3311756},
//                new double[]{18.1099319, 59.3210171},
//                new double[]{18.1195450, 59.3187398},
//                new double[]{18.1495857, 59.3197909},
//                new double[]{18.1643486, 59.3265346},
//                new double[]{18.1631470, 59.3365164},
//                new double[]{18.1458092, 59.3626826}));

        //stockholm2
//        return new NodeShape(Arrays.asList(
//                new double[]{18.1181717, 59.3654819},
//                new double[]{17.9921722, 59.3468453},
//                new double[]{18.0210114, 59.3065623},
//                new double[]{18.1377411, 59.3307378},
//                new double[]{18.1555939, 59.3561211}));

        //uppsala
//        return new NodeShape(Arrays.asList(
//                new double[]{17.5767517, 59.9577605},
//                new double[]{17.5286865, 59.8806685},
//                new double[]{17.5836182, 59.7937257},
//                new double[]{17.6509094, 59.7764482},
//                new double[]{17.7580261, 59.8033973},
//                new double[]{17.7456665, 59.8882479}));

        //mefjärd
        return new NodeShape(Arrays.asList(
                new double[]{-341.5065765+360, 59.0837983},
                new double[]{-341.5491486+360, 59.0414377},
                new double[]{-341.5567017+360, 59.0094562},
                new double[]{-341.5148163+360, 59.0076883},
                new double[]{-341.5010834+360, 59.061742},
                new double[]{-341.4571381+360, 59.0807995},
                new double[]{-341.4835739+360, 59.0915587}));

        //sandböte (4 osm-objects)
//        return new NodeShape(Arrays.asList(
//                new double[]{18.4603786,59.056049},
//                new double[]{18.455658,59.0427181},
//                new double[]{18.4662151,59.0455437},
//                new double[]{18.4645844,59.0563581}));

        //nowhere (nothing here)
//        return new NodeShape(Arrays.asList(
//                new double[]{-158.8780975, 42.3463653},
//                new double[]{-158.8842773, 42.3417978},
//                new double[]{-158.875351, 42.3402752}));
    }
}
