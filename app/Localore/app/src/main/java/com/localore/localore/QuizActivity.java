package com.localore.localore;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.Question;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.RunningQuizControl;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.annotations.Annotation;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    /**
     * Defines tags for different zoom-levels: overall working area vs zoomed in question.
     */
    private static final int WORKING_AREA_ZOOM = 0;
    private static final int QUESTION_ZOOM = 1;

    private Activity activity;

    private ConstraintLayout topContainer;
    private ImageButton exitButton;
    private ProgressBar progressBar;

    private TextView textView;
    private ImageView questionCategoryIcon;
    private RecyclerView topRecycler;
    private MapView mapView;
    private RecyclerView bottomRecycler;

    private FloatingActionButton nextQuestionButton;
    private FloatingActionButton toggleZoomButton;

    private MapboxMap mapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        this.activity = this;
        this.topContainer = findViewById(R.id.layout_quiz_topContainer);
        this.exitButton = findViewById(R.id.imageButton_quiz_close);
        this.progressBar = findViewById(R.id.progressBar_quiz);

        this.nextQuestionButton = findViewById(R.id.button_quiz_nextQuestion);
        this.toggleZoomButton = findViewById(R.id.button_quiz_toggleZoom);

        this.textView = findViewById(R.id.textView_quiz);
        this.questionCategoryIcon = findViewById(R.id.imageView_quiz_questionCategoryIcon);
        this.topRecycler = findViewById(R.id.recyclerView_quiz_top);
        this.mapView = findViewById(R.id.mapView_quiz);
        this.bottomRecycler = findViewById(R.id.recyclerView_bottom);

        mapView.onCreate(null);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                QuizActivity.this.mapboxMap = mapboxMap;
                update();
            }
        });
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitQuiz();
            }
        });
        nextQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextQuestion();
            }
        });
    }

    /**
     * Update quiz (layout etc) based on db.
     */
    private void update() {
        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        updateProgressBar();
        this.mapboxMap.clear();

        switch (currentQuestion.getType()) {
            case Question.NAME_IT:
                update_nameIt();
                break;
            case Question.PLACE_IT:
                updateLayout_placeIt();
                break;
            case Question.PAIR_IT:
                updateLayout_pairIt();
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        updateNextQuestionButton();
        updateExitButton();
    }

    /**
     * Update progress-bar based on current progress.
     */
    private void updateProgressBar() {
        int currentIndex = RunningQuizControl.load(this).getCurrentQuestionIndex();
        int tot = RunningQuizControl.noQuestions(this);

        int progress = 0;
        if (tot == 0) progress = 0;
        else if (tot == 1 && currentIndex == 0) progress = 0;
        else if (tot == 1 && currentIndex == 1) progress = 99;
        else {
            progress = Math.round(100f * currentIndex / (tot - 1)) - 1;
        }
        this.progressBar.setProgress(progress);
    }

    /**
     * Hide/show depending on current question result.
     */
    private void updateNextQuestionButton() {
        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        if (currentQuestion.isAnsweredCorrectly())
            this.nextQuestionButton.show();
        else
            this.nextQuestionButton.hide();
    }

    /**
     * Hidden if follow-up.
     */
    private void updateExitButton() {
        if (RunningQuizControl.load(this).getType() == RunningQuiz.FOLLOW_UP_QUIZ)
            exitButton.setVisibility(View.INVISIBLE);
        else
            exitButton.setVisibility(View.VISIBLE);
    }

    //region Name-it

    /**
     * Icon, text, map and alternatives.
     */
    private void update_nameIt() {
        this.topRecycler.setVisibility(View.GONE);
        this.bottomRecycler.setVisibility(View.VISIBLE);

        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        GeoObject geoObject = RunningQuizControl.loadGeoObjectFromQuestion(currentQuestion, this);
        QuizCategory quizCategory = ExerciseControl.loadQuizCategoryOfGeoObject(geoObject, this);

        this.questionCategoryIcon.setImageResource( quizCategory.getIconResource() );
        this.textView.setText(
                String.format("%s:\n%s", getString(R.string.name_it), geoObject.getCategory()));

        updateMap_nameIt(geoObject);
        updateAlternatives_nameIt(geoObject, currentQuestion.getContent());
    }

    /**
     * Updates the map: add a geo-object.
     * Fly to working area. Specify toggle-zoom-button action.
     * @param geoObject
     */
    private void updateMap_nameIt(GeoObject geoObject) {
        NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();

        LocaUtils.addGeoObject(geoObject, this.mapboxMap, this);
        LocaUtils.flyToFitShape(workingArea, this.mapboxMap, LocaUtils.LONG_FLY_TIME);

        setToggleZoomButton(WORKING_AREA_ZOOM);
        toggleZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentZoomLevelTag = (int)toggleZoomButton.getTag();

                if (currentZoomLevelTag == WORKING_AREA_ZOOM) {
                    setToggleZoomButton(QUESTION_ZOOM);
                    LocaUtils.flyToFitBounds(geoObject.getBounds(), mapboxMap, LocaUtils.SHORT_FLY_TIME);
                }
                else if (currentZoomLevelTag == QUESTION_ZOOM) {
                    setToggleZoomButton(WORKING_AREA_ZOOM);
                    LocaUtils.flyToFitShape(workingArea, mapboxMap, LocaUtils.SHORT_FLY_TIME);
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
            toggleZoomButton.setImageResource(R.drawable.mapbox_compass_icon); //zoom to element icon
            toggleZoomButton.setTag(WORKING_AREA_ZOOM);
        }
        else if (currentZoomLevelTag == QUESTION_ZOOM) {
            toggleZoomButton.setImageResource(R.drawable.mapbox_info_icon_default); //overview zoom icon
            toggleZoomButton.setTag(QUESTION_ZOOM);
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
        this.bottomRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        AlternativesAdapter alternativesAdapter = new AlternativesAdapter(alternatives);
        bottomRecycler.setAdapter(alternativesAdapter);
    }

    private class AlternativesAdapter extends RecyclerView.Adapter<AlternativeHolder> {
        private List<GeoObject> geoObjectAlternatives;

        public AlternativesAdapter(List<GeoObject> geoObjectAlternatives) {
            this.geoObjectAlternatives = geoObjectAlternatives;
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
            holder.bind(geoObjectAlternative);
        }

        @Override
        public int getItemCount() {
            return this.geoObjectAlternatives.size();
        }
    }

    private class AlternativeHolder extends RecyclerView.ViewHolder {
        private GeoObject geoObjectAlternative;
        private Button button_alternative;

        public AlternativeHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.listitem_question_alternative, parent, false));

            this.button_alternative = super.itemView.findViewById(R.id.button_alternative);
            button_alternative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alternativeClicked_nameIt(getAdapterPosition());
                }
            });
        }

        public void bind(GeoObject geoObjectAlternative) {
            this.geoObjectAlternative = geoObjectAlternative;
            this.button_alternative.setText(geoObjectAlternative.getName());
        }

        public GeoObject getGeoObject() {
            return geoObjectAlternative;
        }

        /**
         * Graphically show that this is the correct answer.
         */
        public void setCorrect() {
            button_alternative.setClickable(false);
            int borderColor = LocaUtils.rankBasedColor(geoObjectAlternative.getRank());
            LocaUtils.addBorder(button_alternative, borderColor);
        }

        /**
         * Graphically show that this is an incorrect answer.
         */
        public void setIncorrect() {
            button_alternative.setEnabled(false);
        }
    }

    /**
     * @param clickedIndex
     */
    private void alternativeClicked_nameIt(int clickedIndex) {
        boolean correct = RunningQuizControl.reportNameItPlaceItAnswer(clickedIndex, this);
        View clickedView = bottomRecycler.getChildAt(clickedIndex);
        AlternativeHolder clickedHolder = (AlternativeHolder) bottomRecycler.getChildViewHolder(clickedView);

        if (correct) {
            for (int i = 0; i < bottomRecycler.getChildCount(); i++) {
                if (i != clickedIndex) {
                    View holder = bottomRecycler.getChildAt(i);
                    holder.setVisibility(View.INVISIBLE);
                }
            }
            clickedHolder.setCorrect();
            nextQuestionButton.show();
        }
        else {
            clickedHolder.setIncorrect();
            flashGeoObject(clickedHolder.getGeoObject());
        }
    }

    /**
     * Flash geo-object in map.
     * @param geoObject
     */
    private void flashGeoObject(GeoObject geoObject) {
        int color = Color.GRAY;
        int displayTime = 1500;
        List<Annotation> annotations = LocaUtils.addGeoObject(geoObject, mapboxMap, color, this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mapboxMap.removeAnnotations(annotations);
            }
        }, displayTime);
    }

    //endregion

    /**
     *
     */
    private void updateLayout_placeIt() {

    }

    /**
     *
     */
    private void updateLayout_pairIt() {

    }

    /**
     * Move to next question in running-quiz in db, and update activity accordingly.
     * Or, quiz done.
     */
    public void nextQuestion() {
        Question nextQuestion = RunningQuizControl.nextQuestion(this);
        if (nextQuestion == null) {
            quizDone();
        }
        else {
            update();
        }
    }

    /**
     * Start result-activity when finished.
     */
    public void quizDone() {
        RunningQuizControl.reportRunningQuizFinished(this);

        int type = RunningQuizControl.load(this).getType();
        if (type != RunningQuiz.FOLLOW_UP_QUIZ) {
            QuizResultActivity.start(this);
        }
        else {
            ExerciseActivity.start(this);
        }
        finish();
    }


    //region handle mapView's lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        mapView.onSaveInstanceState(bundle);
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    //endregion

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
                RunningQuizControl.newLevelQuiz(exerciseId, quizCategory, oldActivity);
                break;
            case RunningQuiz.FOLLOW_UP_QUIZ:
                RunningQuizControl.newFollowUpQuiz(oldActivity);
                break;
            case RunningQuiz.QUIZ_CATEGORY_REMINDER:
                RunningQuizControl.newLevelReminder(exerciseId, quizCategory, oldActivity);
                break;
            case RunningQuiz.EXERCISE_REMINDER:
                RunningQuizControl.newExerciseReminder(exerciseId, oldActivity);
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        RunningQuizControl.nextQuestion(oldActivity);

        LocaUtils.fadeInActivity(QuizActivity.class, oldActivity);
    }

    /**
     * Use when new running-quiz already exists in db.
     * @param oldActivity
     */
    public static void resumedStart(Activity oldActivity) {
        LocaUtils.fadeInActivity(QuizActivity.class, oldActivity);
    }


    /**
     * Called when user clicks exit-button.
     * Only way out!
     */
    public void exitQuiz() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.quit_confirmation_request);

        CharSequence[] dialogOptions = {getString(R.string.Yes), getString(R.string.No)};
        alertBuilder.setItems(dialogOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    AppDatabase db = AppDatabase.getInstance(activity);
                    RunningQuizControl.deleteRunningQuiz(activity);
                    ExerciseActivity.start(SessionControl.load(db).getExerciseId(), activity);
                    QuizActivity.this.finish();
                }
            }
        });

        AlertDialog functionDialog = alertBuilder.create();
        functionDialog.show();
    }

    /**
     * Quit app on back-press.
     */
    @Override
    public void onBackPressed() {
        LocaUtils.quitSecondTime(this);
    }
}
