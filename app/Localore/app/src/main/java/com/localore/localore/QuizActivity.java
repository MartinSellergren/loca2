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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.Question;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.RunningQuizControl;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.annotations.Annotation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                interruptionExit();
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
                update_placeIt();
                break;
            case Question.PAIR_IT:
                update_pairIt();
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();
        LocaUtils.flyToFitShape(workingArea, this.mapboxMap, LocaUtils.LONG_FLY_TIME);
        updateToggleZoomButton(WORKING_AREA_ZOOM);
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
        else if (tot == 1 && currentIndex == 1) progress = 50;
        else {
            progress = Math.round(100f * currentIndex / tot);
            //progress = Math.round(100f * currentIndex / (tot - 1)) - 1;
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
     * Specify toggle-zoom-button action.
     * @param geoObject
     */
    private void updateMap_nameIt(GeoObject geoObject) {
        LocaUtils.addGeoObject(geoObject, this.mapboxMap, this);

        NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();
        toggleZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentZoomLevelTag = (int)toggleZoomButton.getTag();

                if (currentZoomLevelTag == WORKING_AREA_ZOOM) {
                    updateToggleZoomButton(QUESTION_ZOOM);
                    LocaUtils.flyToFitBounds(geoObject.getBounds(), mapboxMap, LocaUtils.SHORT_FLY_TIME);
                }
                else if (currentZoomLevelTag == QUESTION_ZOOM) {
                    updateToggleZoomButton(WORKING_AREA_ZOOM);
                    LocaUtils.flyToFitShape(workingArea, mapboxMap, LocaUtils.SHORT_FLY_TIME);
                }
                else {
                    throw new RuntimeException("Dead end");
                }
            }
        });
    }

    /**
     * @param currentZoomLevelTag
     */
    private void updateToggleZoomButton(int currentZoomLevelTag) {
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
        AlternativeHolder clickedHolder = (AlternativeHolder)bottomRecycler.getChildViewHolder(clickedView);

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
            //TODO: indicate correct answer
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

    //region Place-it

    /**
     * Icon, text, map.
     */
    private void update_placeIt() {
        this.topRecycler.setVisibility(View.GONE);
        this.bottomRecycler.setVisibility(View.GONE);

        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        GeoObject geoObject = RunningQuizControl.loadGeoObjectFromQuestion(currentQuestion, this);
        QuizCategory quizCategory = ExerciseControl.loadQuizCategoryOfGeoObject(geoObject, this);

        this.questionCategoryIcon.setImageResource( quizCategory.getIconResource() );
        this.textView.setText(
                String.format("%s:\n%s (%s)", getString(R.string.place_it), geoObject.getName(), geoObject.getCategory()));

        updateMap_placeIt(currentQuestion.getContent());
    }

    /**
     * Adds clickable geo-objects to map.
     * Specify toggle-zoom-button action.
     * @param alternatives
     */
    private void updateMap_placeIt(List<GeoObject> alternatives) {
        Map<Long, Long> markersLookupMap = new HashMap<>();
        Map<Long, Long> polylinesLookupMap = new HashMap<>();
        LocaUtils.addGeoObjects(alternatives, mapboxMap, markersLookupMap, polylinesLookupMap, this);


        mapboxMap.setOnMarkerClickListener(marker -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long answeredId = markersLookupMap.get(marker.getId());
            GeoObject answered = db.geoDao().load(answeredId);
            int contentIndex = alternatives.indexOf(answered);
            alternativeClicked_placeIt(contentIndex, marker, answered);
            return true;
        });

        mapboxMap.setOnPolylineClickListener(polyline -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long answeredId = polylinesLookupMap.get(polyline.getId());
            GeoObject answered = db.geoDao().load(answeredId);
            int contentIndex = alternatives.indexOf(answered);
            alternativeClicked_placeIt(contentIndex, polyline, answered);
        });

        NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();
        double[] alternativesBounds = GeoObject.getBounds(alternatives);
        toggleZoomButton.setOnClickListener(view -> {
            int currentZoomLevelTag = (int)toggleZoomButton.getTag();

            if (currentZoomLevelTag == WORKING_AREA_ZOOM) {
                updateToggleZoomButton(QUESTION_ZOOM);
                LocaUtils.flyToFitBounds(alternativesBounds, mapboxMap, LocaUtils.SHORT_FLY_TIME);
            }
            else if (currentZoomLevelTag == QUESTION_ZOOM) {
                updateToggleZoomButton(WORKING_AREA_ZOOM);
                LocaUtils.flyToFitShape(workingArea, mapboxMap, LocaUtils.SHORT_FLY_TIME);
            }
            else {
                throw new RuntimeException("Dead end");
            }
        });
    }

    /**
     * @param contentIndex
     */
    private void alternativeClicked_placeIt(int contentIndex, Annotation annotation, GeoObject answered) {
        boolean correct = RunningQuizControl.reportNameItPlaceItAnswer(contentIndex, this);

        if (correct) {
            removeAllAnnotationsExcept(annotation);
            removeMapListeners();

            String correctText = String.format("%s! %s",
                    getString(R.string.correct),
                    answered.getName());
            Toast.makeText(this, correctText, Toast.LENGTH_SHORT).show();
            nextQuestionButton.show();
        }
        else {
            if (annotation instanceof Marker) {
                ((Marker)annotation).setIcon(LocaUtils.nodeGeoObjectIcon(Color.GRAY, this));
            }
            else {
                ((Polyline)annotation).setColor(Color.GRAY);
            }

            String incorrectText = answered.getName();
            Toast.makeText(this, incorrectText, Toast.LENGTH_SHORT).show();
            //TODO: indicate correct answer
        }
    }

    /**
     * Remove listeners from map.
     */
    private void removeMapListeners() {
        mapboxMap.setOnMarkerClickListener(null);
        mapboxMap.setOnPolylineClickListener(null);
    }

    /**
     * Remove all annotations on the map except one.
     * @param except
     */
    private void removeAllAnnotationsExcept(Annotation except) {
        for (Annotation annotation : mapboxMap.getAnnotations()) {
            if (annotation.getId() != except.getId()) {
                mapboxMap.removeAnnotation(annotation);
            }
        }
    }

    //endregion

    //region pair it

    /**
     * Text, alternatives and map.
     */
    private void update_pairIt() {
        this.topRecycler.setVisibility(View.VISIBLE);
        this.bottomRecycler.setVisibility(View.GONE);
        this.questionCategoryIcon.setVisibility(View.GONE);

        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        this.textView.setText(getString(R.string.pair_it) + ":");

        updateAlternatives_pairIt(currentQuestion.getContent());
        updateMap_pairIt();
    }

    /**
     * Updates the top-recycler with toggle-able buttons with names of geo-objects.
     *
     * Set listeners to the view-holders. When one is clicked, select this one and
     * deselect all others. Also deselect all objects in the map.
     *
     * @param geoObjects
     */
    private void updateAlternatives_pairIt(List<GeoObject> geoObjects) {
        topRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        UnpairedObjectAdapter adapter = new UnpairedObjectAdapter(geoObjects);
        topRecycler.setAdapter(adapter);

        topRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, bottomRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                deselectAllUnpairedButtons();
                View clickedView = topRecycler.getChildAt(position);
                UnpairedObjectHolder clickedHolder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(clickedView);
                clickedHolder.toggleSelected();
            }

            @Override
            public void onLongItemClick(View view, int position) {}
        }));
    }

    /**
     * Deselect all buttons in the top recycler.
     */
    private void deselectAllUnpairedButtons() {
        for (int i = 0; i < topRecycler.getChildCount(); i++) {
            UnpairedObjectHolder holder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(topRecycler.getChildAt(i));
            holder.deselect();
        }
    }

    private class UnpairedObjectAdapter extends RecyclerView.Adapter<UnpairedObjectHolder> {
        private List<GeoObject> geoObjects;

        public UnpairedObjectAdapter(List<GeoObject> geoObjectAlternatives) {
            this.geoObjects = geoObjects;
        }

        @NonNull
        @Override
        public UnpairedObjectHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(QuizActivity.this);
            return new UnpairedObjectHolder(inflater, parent);
        }

        /**
         * @param holder
         * @param position
         */
        @Override
        public void onBindViewHolder(@NonNull UnpairedObjectHolder holder, int position) {
            GeoObject geoObject = this.geoObjects.get(position);
            holder.bind(geoObject);
        }

        @Override
        public int getItemCount() {
            return this.geoObjects.size();
        }
    }

    private class UnpairedObjectHolder extends RecyclerView.ViewHolder {
        private GeoObject geoObject;
        private Button button;

        /**
         * The button is toggle-able.
         */
        boolean isSelected = false;

        /**
         * True if paired with corresponding map-object.
         */
        private boolean isPaired = false;

        public UnpairedObjectHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.listitem_question_alternative, parent, false));

            this.button = super.itemView.findViewById(R.id.button_alternative);
        }

        public void bind(GeoObject geoObject) {
            this.geoObject = geoObject;
            this.button.setText(geoObject.getName());
        }

        public GeoObject getGeoObject() {
            return this.geoObject;
        }

        public Button getButton() {
            return this.button;
        }

        public boolean isSelected() {
            return isSelected;
        }

        /**
         * Switches selected-state of button. Also changes appearance.
         * @return
         */
        public boolean toggleSelected() {
            this.isSelected = !isSelected;
            updateAppearance();
            return isSelected;
        }

        /**
         * Deselect the button.
         */
        public void deselect() {
            this.isSelected = false;
            updateAppearance();
        }

        private void updateAppearance() {
            if (isSelected) {
                int borderColor = LocaUtils.rankBasedColor(geoObject.getRank());
                LocaUtils.addBorder(button, borderColor);
            }
            else {
                LocaUtils.removeBorder(button);
            }
        }

        /**
         * Hide this view-holder when correctly paired.
         */
        public void setPaired() {
            itemView.setVisibility(View.INVISIBLE);
            this.isPaired = true;
        }

        public boolean isPaired() {
            return isPaired;
        }
    }

    /**
     * Updated map with geoObjects that isn't paired, which is defined by the view-holders in
     * the top-recycler.
     *
     * @pre Top recycler items set.
     */
    private void updateMap_pairIt() {
        Map<Long, Long> markersLookupMap = new HashMap<>();
        Map<Long, Long> polylinesLookupMap = new HashMap<>();

        for (int i = 0; i < topRecycler.getChildCount(); i++) {
            UnpairedObjectHolder holder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(topRecycler.getChildAt(i));
            if (!holder.isPaired()) {
                GeoObject geoObject = holder.getGeoObject();
                LocaUtils.addGeoObject(geoObject, mapboxMap, markersLookupMap, polylinesLookupMap, this);
            }
        }

        mapboxMap.setOnMarkerClickListener(marker -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long answeredId = markersLookupMap.get(marker.getId());
            GeoObject geoObject = db.geoDao().load(answeredId);
            UnpairedObjectHolder holder = findTopRecyclerHolder(geoObject);
            itemsPaired(marker, holder);
            return true;
        });

        mapboxMap.setOnPolylineClickListener(polyline -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long answeredId = polylinesLookupMap.get(polyline.getId());
            GeoObject geoObject = db.geoDao().load(answeredId);
            UnpairedObjectHolder holder = findTopRecyclerHolder(geoObject);
            itemsPaired(polyline, holder);
        });
    }

    private UnpairedObjectHolder findTopRecyclerHolder(GeoObject geoObject) {
        for (int i = 0; i < topRecycler.getChildCount(); i++) {
            UnpairedObjectHolder holder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(topRecycler.getChildAt(i));
            if (holder.getGeoObject() == geoObject) {
                return holder;
            }
        }
        return null;
    }

    /**
     * Called when a map-object is pressed.
     * @param annotation
     * @param holder Currently selected holder. NULL if none selected.
     */
    private void itemsPaired(Annotation annotation, UnpairedObjectHolder holder) {
        if (holder == null) {
            //ignore map-click
        }
        else if (!holder.isSelected()) {
            Toast.makeText(this, R.string.incorrect, Toast.LENGTH_SHORT).show();
            holder.deselect();
        }
        else {
            Toast.makeText(this, R.string.correct, Toast.LENGTH_LONG).show();
            holder.setPaired();
            mapboxMap.removeAnnotation(annotation);
        }
    }

    //endregion

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
            LocaUtils.fadeOutFadeIn(this, () -> { update(); });
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
            backToExercise();
        }
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
     * @param quizCategory Not used by follow-up and exercise-reminder (just put -1).
     * @param oldActivity
     *
     * @pre Session-exercise set.
     * @pre If quizType=followup: A running-quiz in db.
     */
    public static void freshStart(int quizType, int quizCategory, Activity oldActivity)
            throws RunningQuizControl.QuizConstructionException {
        AppDatabase db = AppDatabase.getInstance(oldActivity);
        long exerciseId = SessionControl.load(db).getExerciseId();
        String initTalk = "";

        switch (quizType) {
            case RunningQuiz.LEVEL_QUIZ:
                int level = RunningQuizControl.newLevelQuiz(exerciseId, quizCategory, oldActivity);
                int displayLevel = level + 1;
                initTalk = oldActivity.getString(R.string.level) + " " + displayLevel;
                break;
            case RunningQuiz.FOLLOW_UP_QUIZ:
                RunningQuizControl.newFollowUpQuiz(oldActivity);
                initTalk = oldActivity.getString(R.string.follow_up_quiz_init_talk);
                break;
            case RunningQuiz.QUIZ_CATEGORY_REMINDER:
                RunningQuizControl.newLevelReminder(exerciseId, quizCategory, oldActivity);
                initTalk = oldActivity.getString(R.string.reminder_quiz_init_talk);
                break;
            case RunningQuiz.EXERCISE_REMINDER:
                RunningQuizControl.newExerciseReminder(exerciseId, oldActivity);
                initTalk = oldActivity.getString(R.string.reminder_quiz_init_talk);
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        RunningQuizControl.nextQuestion(oldActivity);
        LocaUtils.fadeInActivityWithTalk(initTalk, LocaUtils.LONG_TALK, QuizActivity.class, oldActivity);
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
    public void interruptionExit() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.quit_confirmation_request);

        CharSequence[] dialogOptions = {getString(R.string.Yes), getString(R.string.No)};
        alertBuilder.setItems(dialogOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    backToExercise();
                }
            }
        });

        AlertDialog functionDialog = alertBuilder.create();
        functionDialog.show();
    }

    /**
     * Only way out! (unless follow-up).
     */
    public void backToExercise() {
        AppDatabase db = AppDatabase.getInstance(activity);
        RunningQuizControl.deleteRunningQuiz(activity);
        ExerciseActivity.start(SessionControl.load(db).getExerciseId(), activity);
    }

    /**
     * Quit app on back-press.
     */
    @Override
    public void onBackPressed() {
        LocaUtils.quitSecondTime(this);
    }
}
