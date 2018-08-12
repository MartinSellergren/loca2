package com.localore.localore;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Question;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.modelManipulation.RunningQuizControl;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.List;

public class QuizActivity extends AppCompatActivity {

    /**
     * Defines tags for different zoom-levels: overall working area vs zoomed in question.
     */
    private static final int WORKING_AREA_ZOOM = 0;
    private static final int QUESTION_ZOOM = 1;

    private Activity activity;
    private ProgressBar progressBar;
    private ViewFlipper flipper;
    private FloatingActionButton button_nextQuestion;
    private FloatingActionButton button_toggleZoom;

    private ImageView imageView_nameItIcon;
    private TextView textView_nameIt;
    private MapView mapView_nameIt;
    private RecyclerView recyclerView_nameItAlternatives;

    private ImageView imageView_placeItIcon;
    private TextView textView_placeIt;
    private MapView mapView_placeIt;

    private TextView textView_pairIt;
    private RecyclerView recyclerView_pairIt;
    private MapView mapView_pairIt;

    private MapboxMap mapboxMap_nameIt;
    private MapboxMap mapboxMap_placeIt;
    private MapboxMap mapboxMap_pairIt;

    private Exercise exercise;


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
        this.button_toggleZoom = findViewById(R.id.button_toggleZoom);

        this.imageView_nameItIcon = findViewById(R.id.imageView_nameItIcon);
        this.textView_nameIt = findViewById(R.id.textView_nameIt);
        this.mapView_nameIt = findViewById(R.id.mapView_nameIt);
        this.recyclerView_nameItAlternatives = findViewById(R.id.recyclerView_nameItAlternatives);

        this.imageView_placeItIcon = findViewById(R.id.imageView_placeItIcon);
        this.textView_placeIt = findViewById(R.id.textView_placeIt);
        this.mapView_placeIt = findViewById(R.id.mapView_placeIt);

        this.textView_pairIt = findViewById(R.id.textView_pairIt);
        this.recyclerView_pairIt = findViewById(R.id.recyclerView_pairIt);
        this.mapView_pairIt = findViewById(R.id.mapView_pairIt);

        mapView_nameIt.onCreate(savedInstanceState);
        mapView_placeIt.onCreate(savedInstanceState);
        mapView_pairIt.onCreate(savedInstanceState);

        this.exercise = SessionControl.loadExercise(AppDatabase.getInstance(this));

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

        LocaUtils.flyToFitShape(this.exercise.getWorkingArea(), this.mapboxMap_nameIt);

        setToggleZoomButton(WORKING_AREA_ZOOM);
        button_toggleZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentZoomLevelTag = (int)button_toggleZoom.getTag();

                if (currentZoomLevelTag == WORKING_AREA_ZOOM) {
                    setToggleZoomButton(QUESTION_ZOOM);
                    LocaUtils.flyToFitBounds(geoObject.getBounds(), mapboxMap_nameIt);
                }
                else if (currentZoomLevelTag == QUESTION_ZOOM) {
                    setToggleZoomButton(WORKING_AREA_ZOOM);
                    LocaUtils.flyToFitShape(exercise.getWorkingArea(), mapboxMap_nameIt);
                }
                else {
                    throw new RuntimeException("Dead end");
                }
            }
        });
    }

    /**
     *
     * @param currentZoomLevelTag
     */
    private void setToggleZoomButton(int currentZoomLevelTag) {
        if (currentZoomLevelTag == WORKING_AREA_ZOOM) {
            button_toggleZoom.setImageResource(R.drawable.mapbox_compass_icon); //zoom to element icon
            button_toggleZoom.setTag(WORKING_AREA_ZOOM);
        }
        else if (currentZoomLevelTag == QUESTION_ZOOM) {
            button_toggleZoom.setImageResource(R.drawable.mapbox_info_icon_default); //overview zoom icon
            button_toggleZoom.setTag(QUESTION_ZOOM);
        }
        else {
            throw new RuntimeException("Dead end");
        }
    }

    /**
     * Updates the alternatives section of the layout.
     * Enables user-interactions (select alternative).
     * @param correct
     * @param alternatives
     */
    private void updateAlternatives_nameIt(GeoObject correct, List<GeoObject> alternatives) {
        this.recyclerView_nameItAlternatives.setLayoutManager(new GridLayoutManager(this, 2));
        AlternativesAdapter alternativesAdapter = new AlternativesAdapter(alternatives, correct);
        recyclerView_nameItAlternatives.setAdapter(alternativesAdapter);
    }

    private class AlternativesAdapter extends RecyclerView.Adapter<AlternativeHolder> {
        private List<GeoObject> geoObjectAlternatives;
        private GeoObject geoObjectCorrect;

        public AlternativesAdapter(List<GeoObject> geoObjectAlternatives, GeoObject geoObjectCorrect) {
            this.geoObjectAlternatives = geoObjectAlternatives;
            this.geoObjectCorrect = geoObjectCorrect;
        }

        @NonNull
        @Override
        public AlternativeHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(QuizActivity.this);
            return new AlternativeHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull AlternativeHolder holder, int position) {
            GeoObject geoObjectAlternative = this.geoObjectAlternatives.get(position);
            holder.bind(geoObjectAlternative, this.geoObjectCorrect);
        }

        @Override
        public int getItemCount() {
            return this.geoObjectAlternatives.size();
        }
    }

    private class AlternativeHolder extends RecyclerView.ViewHolder {
        private GeoObject geoObjectAlternative;
        private Button button_alternative;

        /**
         * For checking answer on click.
         */
        private GeoObject geoObjectCorrect;


        public AlternativeHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.listitem_question_alternative, parent, false));

            this.button_alternative = super.itemView.findViewById(R.id.button_alternative);

            button_alternative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (geoObjectAlternative.equals(geoObjectCorrect))
                        Toast.makeText(QuizActivity.this, "correct", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(QuizActivity.this, "incorrect", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void bind(GeoObject geoObjectAlternative, GeoObject geoObjectCorrect) {
            this.geoObjectAlternative = geoObjectAlternative;
            this.geoObjectCorrect = geoObjectCorrect;
            this.button_alternative.setText(geoObjectAlternative.getName());
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
