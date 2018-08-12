package com.localore.localore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Question;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.modelManipulation.RunningQuizControl;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private Activity activity;
    private ProgressBar progressBar;
    private ViewFlipper flipper;
    private FloatingActionButton button_nextQuestion;

    private ImageView imageView_nameItIcon;
    private TextView textView_nameIt;
    private MapView mapView_nameIt;
    private TableLayout tableLayout_nameItAlternatives;

    private ImageView imageView_placeItIcon;
    private TextView textView_placeIt;
    private MapView mapView_placeIt;

    private TextView textView_pairIt;
    private TableLayout tableLayout_pairIt;
    private MapView mapView_pairIt;

    private MapboxMap mapboxMap_nameIt;
    private MapboxMap mapboxMap_placeIt;
    private MapboxMap mapboxMap_pairIt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        this.activity = this;
        this.progressBar = findViewById(R.id.progressBar_quiz);
        this.flipper = findViewById(R.id.viewFlipper_questions);
        this.button_nextQuestion = findViewById(R.id.button_nextQuestion);

        this.imageView_nameItIcon = findViewById(R.id.imageView_nameItIcon);
        this.textView_nameIt = findViewById(R.id.textView_nameIt);
        this.mapView_nameIt = findViewById(R.id.mapView_nameIt);
        this.tableLayout_nameItAlternatives = findViewById(R.id.tableLayout_nameItAlternatives);

        this.imageView_placeItIcon = findViewById(R.id.imageView_placeItIcon);
        this.textView_placeIt = findViewById(R.id.textView_placeIt);
        this.mapView_placeIt = findViewById(R.id.mapView_placeIt);

        this.textView_pairIt = findViewById(R.id.textView_pairIt);
        this.tableLayout_pairIt = findViewById(R.id.tableLayout_pairIt);
        this.mapView_pairIt = findViewById(R.id.mapView_pairIt);

        mapView_nameIt.onCreate(savedInstanceState);
        mapView_placeIt.onCreate(savedInstanceState);
        mapView_pairIt.onCreate(savedInstanceState);

        updateLayout();
    }

    /**
     * Sets layout based on current question in running-quiz.
     * Calls setContentView() with xml-layout based on question.
     */
    private void updateLayout() {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuiz runningQuiz = RunningQuizControl.load(db);

        int currentQuestionIndex = runningQuiz.getCurrentQuestionIndex();
        int noQuestions = RunningQuizControl.noQuestions(db);
        updateProgressBar(currentQuestionIndex, noQuestions);

        Question question =
                db.questionDao().loadWithRunningQuizAndIndex(runningQuiz.getId(), currentQuestionIndex);
        QuizCategory quizCategory =
                RunningQuizControl.loadQuizCategoryFromRunningQuiz(runningQuiz, db);

        switch (question.getType()) {
            case Question.NAME_IT:
                updateLayout_nameIt(question, quizCategory);
                break;
            case Question.PLACE_IT:
                updateLayout_placeIt(question, quizCategory);
                break;
            case Question.PAIR_IT:
                updateLayout_pairIt(question);
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        if (question.isAnsweredCorrectly())
            this.button_nextQuestion.show();
        else
            this.button_nextQuestion.hide();
    }

    /**
     * @param currentIndex [0, tot)
     * @param tot
     */
    private void updateProgressBar(int currentIndex, int tot) {
        int progress = 0;
        if (tot == 0) progress = 0;
        else if (tot == 1 && currentIndex == 0) progress = 0;
        else if (tot == 1 && currentIndex == 1) progress = 99;
        else {
            progress = Math.round(100f * currentIndex / (tot - 1)) - 1;
        }
        this.progressBar.setProgress(progress);
    }

    //region name-it

    /**
     * Icon, header, map and alternatives.
     *
     * @param question
     * @param quizCategory
     */
    private void updateLayout_nameIt(Question question, QuizCategory quizCategory) {
        AppDatabase db = AppDatabase.getInstance(this);
        GeoObject geoObject = RunningQuizControl.loadGeoObjectFromQuestion(question, db);

        this.imageView_nameItIcon.setImageResource( quizCategory.getIconResource() );
        this.textView_nameIt.setText(
                String.format("%s:\n%s", getString(R.string.name_it), geoObject.getCategory()));

        if (mapboxMap_nameIt == null) loadAndUpdateMap_nameIt(geoObject);
        else updateMap_nameIt(geoObject);

        updateAlternatives_nameIt(geoObject, question.getContent());

        this.flipper.setDisplayedChild(Question.NAME_IT);
    }

    /**
     * Loads the map and then update.
     * @param geoObject
     */
    private void loadAndUpdateMap_nameIt(GeoObject geoObject) {
        this.mapView_nameIt.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                QuizActivity.this.mapboxMap_nameIt = mapboxMap;
                updateMap_nameIt(geoObject);
            }
        });
    }

    /**
     * Updates the map: add a geo-object.
     * @param geoObject
     */
    private void updateMap_nameIt(GeoObject geoObject) {
        LocaUtils.addGeoObject(geoObject, this.mapboxMap_nameIt, this);
    }

    /**
     * Updates the alternatives section of the layout.
     * Enables user-interactions (select alternative).
     * @param geoObject
     * @param alternatives
     */
    private void updateAlternatives_nameIt(GeoObject geoObject, List<GeoObject> alternatives) {
        for (int i = 0; i < alternatives.size(); i += 2) {
            TableRow tableRow = new TableRow(this);

            //tableRow.setId(-1);
            //tableRow.setBackgroundColor(Color.GRAY);
//            tableRow.setLayoutParams(new TableLayout.LayoutParams(
//                    TableLayout.LayoutParams.MATCH_PARENT,
//                    TableLayout.LayoutParams.WRAP_CONTENT));

            TextView alt0 = new TextView(this);
            //alt0.setId(-1);
            alt0.setText(alternatives.get(i).getName());
            alt0.setTextColor(Color.WHITE);
            alt0.setPadding(8, 8, 8, 8);
//            alt0.setLayoutParams(new TableRow.LayoutParams(
//                    TableRow.LayoutParams.WRAP_CONTENT,
//                    TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.addView(alt0);

            TextView alt1 = new TextView(this);
            //alt1.setId(-1);
            alt1.setText(alternatives.get(i+1).getName());
            alt1.setTextColor(Color.WHITE);
            alt1.setPadding(8, 8, 8, 8);
//            alt1.setLayoutParams(new TableRow.LayoutParams(
//                    TableRow.LayoutParams.WRAP_CONTENT,
//                    TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.addView(alt1);

            this.tableLayout_nameItAlternatives.addView(tableRow);
        }

    }

    //endregion

    /**
     * @param question
     * @param quizCategory
     */
    private void updateLayout_placeIt(Question question, QuizCategory quizCategory) {
        this.flipper.setDisplayedChild(Question.PLACE_IT);
    }

    /**
     *
     * @param question
     */
    private void updateLayout_pairIt(Question question) {
        this.flipper.setDisplayedChild(Question.PAIR_IT);
    }

    /**
     * Called when user clicks next-question button.
     * Next question or done?
     *
     * @param view
     */
    public void onNextQuestion(View view) {

    }


    //endregion

    //region handle mapView's lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        mapView_nameIt.onResume();
        mapView_placeIt.onResume();
        mapView_pairIt.onResume();
    }
    @Override
    protected void onStart() {
        super.onStart();
        mapView_nameIt.onStart();
        mapView_placeIt.onStart();
        mapView_pairIt.onStart();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView_nameIt.onPause();
        mapView_placeIt.onPause();
        mapView_pairIt.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
        mapView_nameIt.onStop();
        mapView_placeIt.onStop();
        mapView_pairIt.onStop();
    }
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        mapView_nameIt.onSaveInstanceState(bundle);
        mapView_placeIt.onSaveInstanceState(bundle);
        mapView_pairIt.onSaveInstanceState(bundle);
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView_nameIt.onLowMemory();
        mapView_placeIt.onLowMemory();
        mapView_pairIt.onLowMemory();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView_nameIt.onDestroy();
        mapView_placeIt.onDestroy();
        mapView_pairIt.onDestroy();
    }
    //endregion

    /**
     * Called when user clicks exit-button.
     * Only way out!
     * @param view
     */
    public void onExitQuiz(View view) {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuizControl.deleteRunningQuiz(db);
        ExerciseActivity.start(SessionControl.load(db).getExerciseId(), this);
        finish();
    }


    /**
     * Starts a new quiz by creating a running-quiz in db and starts the activity.
     * Loads the first question.
     *
     * @param quizType
     * @param quizCategory
     * @param oldActivity
     *
     * @pre Session-exercise set.
     * @pre If quizType=followup: A running-quiz in db.
     */
    public static void freshStart(int quizType, int quizCategory, Activity oldActivity) {
        AppDatabase db = AppDatabase.getInstance(oldActivity);
        long exerciseId = SessionControl.load(db).getExerciseId();

        switch (quizType) {
            case RunningQuiz.LEVEL_QUIZ:
                RunningQuizControl.newLevelQuiz(exerciseId, quizCategory, db);
                break;
            case RunningQuiz.FOLLOW_UP_QUIZ:
                RunningQuizControl.newFollowUpQuiz(db);
                break;

            case RunningQuiz.QUIZ_CATEGORY_REMINDER:
                RunningQuizControl.newLevelReminder(exerciseId, quizCategory, db);
                break;
            case RunningQuiz.EXERCISE_REMINDER:
                RunningQuizControl.newExerciseReminder(exerciseId, db);
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        RunningQuizControl.nextQuestion(db);

        LocaUtils.fadeInActivity(QuizActivity.class, oldActivity);
    }

    /**
     * Use when an exercise is running (i.e a running-quiz exists).
     * @param oldActivity
     */
    public static void resumedStart(Activity oldActivity) {
        LocaUtils.fadeInActivity(QuizActivity.class, oldActivity);
    }

    /**
     * Quit app on back-press.
     */
    @Override
    public void onBackPressed() {
        LocaUtils.quitSecondTime(this);
    }
}
