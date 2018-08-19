package com.localore.localore;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizActivity extends AppCompatActivity {

    /**
     * Defines tags for different zoom-levels: overall working area vs zoomed in question.
     */
    private static final int WORKING_AREA_ZOOM = 0;
    private static final int QUESTION_ZOOM = 1;

    /**
     * Icons for the zoom-action-button.
     */
    private static final int ZOOM_TO_ELEMENTS_ICON = android.R.drawable.ic_menu_search;
    private static final int OVERVIEW_ZOOM_ICON = android.R.drawable.ic_menu_revert;

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
        mapView.getMapAsync(mapboxMap -> {
            QuizActivity.this.mapboxMap = mapboxMap;
            update();
        });
        exitButton.setOnClickListener(view -> interruptionExit());
        nextQuestionButton.setOnClickListener(view -> nextQuestion());
    }

    /**
     * Update quiz (layout etc) based on db. A question is initialized.
     */
    private void update() {
        Exercise exercise = SessionControl.loadExercise(AppDatabase.getInstance(this));
        findViewById(R.id.layout_quiz_background).setBackgroundColor(exercise.getColor());
        topContainer.setBackgroundColor(exercise.getColor());
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(exercise.getColor());
        }

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
        updateExitButton();
        nextQuestionButton.hide();
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
            //progress = Math.round(100f * currentIndex / tot);
            progress = Math.round(100f * currentIndex / (tot - 1)) - 1;
        }
        this.progressBar.setProgress(progress);
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
     * Specify toggle-zoom-button action.
     */
    private void update_nameIt() {
        this.topRecycler.setVisibility(View.GONE);
        this.bottomRecycler.setVisibility(View.VISIBLE);

        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        GeoObject geoObject = RunningQuizControl.loadGeoObjectFromQuestion(currentQuestion, this);
        QuizCategory quizCategory = ExerciseControl.loadQuizCategoryOfGeoObject(geoObject, this);

        this.questionCategoryIcon.setImageResource( QuizCategory.getIconResource(quizCategory.getType()) );
        this.textView.setText(
                String.format("%s:\n%s", getString(R.string.name_it), geoObject.getCategory()));

        updateMap_nameIt(geoObject);
        updateAlternatives_nameIt(geoObject, currentQuestion.getContent());

        NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();
        toggleZoomButton.setOnClickListener(view -> {
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
        });
    }

    /**
     * Updates the map: add a geo-object.
     * @param geoObject
     */
    private void updateMap_nameIt(GeoObject geoObject) {
        LocaUtils.addGeoObject(geoObject, this.mapboxMap, this);
    }

    /**
     * @param currentZoomLevelTag
     */
    private void updateToggleZoomButton(int currentZoomLevelTag) {
        if (currentZoomLevelTag == WORKING_AREA_ZOOM) {
            toggleZoomButton.setImageResource(ZOOM_TO_ELEMENTS_ICON);
            toggleZoomButton.setTag(WORKING_AREA_ZOOM);
        }
        else if (currentZoomLevelTag == QUESTION_ZOOM) {
            toggleZoomButton.setImageResource(OVERVIEW_ZOOM_ICON);
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
            button_alternative.setOnClickListener(view -> alternativeClicked_nameIt(getAdapterPosition()));
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
            LocaUtils.setHighlighted(button_alternative);
        }

        /**
         * Graphically show that this is an incorrect answer.
         */
        public void setIncorrect() {
            button_alternative.setEnabled(false);
        }

        /**
         * Hide and show again.
         */
        public void blink() {
            int BLINK_TIME = 300;
            itemView.setVisibility(View.INVISIBLE);
            new Handler().postDelayed(() -> itemView.setVisibility(View.VISIBLE), BLINK_TIME);
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
            String correctText = String.format("%s %s", getString(R.string.correct), clickedHolder.getGeoObject().getName());
            Toast.makeText(this, correctText, Toast.LENGTH_SHORT).show();
        }
        else {
            clickedHolder.setIncorrect();
            flashGeoObject(clickedHolder.getGeoObject());
            Toast.makeText(this, R.string.incorrect, Toast.LENGTH_SHORT).show();

            //indicate correct answer
            Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
            GeoObject correctAnswerObject = RunningQuizControl.loadGeoObjectFromQuestion(currentQuestion, this);
            AlternativeHolder correctHolder = findAlternativeHolderOfGeoObject(correctAnswerObject);
            correctHolder.blink();
        }
    }

    /**
     * @param geoObject
     * @return Alternative-holder (bottom-recycler) holding geo-object.
     */
    private AlternativeHolder findAlternativeHolderOfGeoObject(GeoObject geoObject) {
        for (int i = 0; i < bottomRecycler.getChildCount(); i++) {
            AlternativeHolder holder = (AlternativeHolder)bottomRecycler.getChildViewHolder(bottomRecycler.getChildAt(i));
            if (holder.geoObjectAlternative.equals(geoObject)) return holder;
        }
        return null;
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
     * Specify toggle-zoom-button action.
     */
    private void update_placeIt() {
        this.topRecycler.setVisibility(View.GONE);
        this.bottomRecycler.setVisibility(View.GONE);

        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        GeoObject geoObject = RunningQuizControl.loadGeoObjectFromQuestion(currentQuestion, this);
        QuizCategory quizCategory = ExerciseControl.loadQuizCategoryOfGeoObject(geoObject, this);

        this.questionCategoryIcon.setImageResource( QuizCategory.getIconResource(quizCategory.getType()) );
        this.textView.setText(
                String.format("%s:\n%s (%s)", getString(R.string.place_it), geoObject.getName(), geoObject.getCategory()));

        updateMap_placeIt(currentQuestion.getContent());

        NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();
        double[] alternativesBounds = GeoObject.getBounds(currentQuestion.getContent());
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
     * Adds clickable geo-objects to map.
     * @param alternatives
     */
    private void updateMap_placeIt(List<GeoObject> alternatives) {
        Map<Long, Long> markersLookupMap = new HashMap<>();
        Map<Long, Long> polylinesLookupMap = new HashMap<>();
        LocaUtils.addGeoObjects(alternatives, mapboxMap, markersLookupMap, polylinesLookupMap, this);

        mapboxMap.setOnMarkerClickListener(marker -> {
            Long answeredId = markersLookupMap.get(marker.getId());
            if (answeredId == null) return true;

            AppDatabase db = AppDatabase.getInstance(this);
            GeoObject answered = db.geoDao().load(answeredId);
            int contentIndex = alternatives.indexOf(answered);
            alternativeClicked_placeIt(contentIndex, answered, markersLookupMap, polylinesLookupMap);
            return true;
        });
        mapboxMap.setOnPolylineClickListener(polyline -> {
            Long answeredId = polylinesLookupMap.get(polyline.getId());
            if (answeredId == null) return;

            AppDatabase db = AppDatabase.getInstance(this);
            GeoObject answered = db.geoDao().load(answeredId);
            int contentIndex = alternatives.indexOf(answered);
            alternativeClicked_placeIt(contentIndex, answered, markersLookupMap, polylinesLookupMap);
        });
    }

    /**
     * @param contentIndex
     */
    private void alternativeClicked_placeIt(int contentIndex, GeoObject answered,
                                            Map<Long,Long> markersLookupMap, Map<Long,Long> polylinesLookupMap) {
        List<Annotation> selectedAnnotations = LocaUtils.geoObjectAnnotations(answered.getId(), mapboxMap, markersLookupMap, polylinesLookupMap);
        LocaUtils.blinkAnnotations(selectedAnnotations, mapboxMap, this);

        boolean correct = RunningQuizControl.reportNameItPlaceItAnswer(contentIndex, this);

        if (correct) {
            removeAllAnnotationsExcept(selectedAnnotations);
            removeMapListeners();

            String correctText = String.format("%s %s", getString(R.string.correct), answered.getName());
            Toast.makeText(this, correctText, Toast.LENGTH_SHORT).show();
            nextQuestionButton.show();
        }
        else {
//            for (Annotation annotation : selectedAnnotations) {
//                if (annotation instanceof Marker) {
//                    ((Marker) annotation).setIcon(LocaUtils.nodeGeoObjectIcon(Color.GRAY, this));
//                } else {
//                    ((Polyline) annotation).setColor(Color.LTGRAY);
//                }
//            }

            String incorrectText = answered.getName();
            Toast.makeText(this, incorrectText, Toast.LENGTH_SHORT).show();

            //indicate correct answer
            Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
            GeoObject correctAnswerObject = RunningQuizControl.loadGeoObjectFromQuestion(currentQuestion, this);
            List<Annotation> correctAnnotations = LocaUtils.geoObjectAnnotations(correctAnswerObject.getId(), mapboxMap,
                    markersLookupMap, polylinesLookupMap);
            LocaUtils.blinkAnnotations(correctAnnotations, mapboxMap, this);
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
     * @param excepts
     */
    private void removeAllAnnotationsExcept(List<Annotation> excepts) {
        for (Annotation annotation : mapboxMap.getAnnotations()) {
            if (!excepts.contains(annotation)) {
                mapboxMap.removeAnnotation(annotation);
            }
        }
    }

    //endregion

    //region Pair it

    /**
     * Text, top-buttons and map.
     * Specify toggle-zoom-button action.
     */
    private void update_pairIt() {
        this.topRecycler.setVisibility(View.VISIBLE);
        this.bottomRecycler.setVisibility(View.GONE);
        this.questionCategoryIcon.setVisibility(View.GONE);

        Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
        this.textView.setText(getString(R.string.pair_it) + ":");

        updateTopButtonsAndMap_pairIt(currentQuestion.getContent());
        setToggleZoomListener();
    }

    private void setToggleZoomListener() {
        toggleZoomButton.setOnClickListener(view -> {
//            Question currentQuestion = RunningQuizControl.loadCurrentQuestion(this);
//            List<GeoObject> mapObjects = currentQuestion.getContent();
//            double[] alternativesBounds = GeoObject.getBounds(currentQuestion.getContent());
            NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();
            int currentZoomLevelTag = (int)toggleZoomButton.getTag();

            if (currentZoomLevelTag == WORKING_AREA_ZOOM) {
                updateToggleZoomButton(QUESTION_ZOOM);
                List<GeoObject> unpairedGeoObjects = unpairedGeoObjects();
                if (unpairedGeoObjects.size() > 0)
                    LocaUtils.flyToFitBounds(GeoObject.getBounds(unpairedGeoObjects), mapboxMap, LocaUtils.SHORT_FLY_TIME);
                else
                    LocaUtils.flyToFitShape(workingArea, mapboxMap, LocaUtils.SHORT_FLY_TIME);
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
     * @return All unpaired geo-objects (in the top-recycler).
     */
    private List<GeoObject> unpairedGeoObjects() {
        List<GeoObject> geoObjects = new ArrayList<>();
        for (int i = 0; i < topRecycler.getChildCount(); i++) {
            UnpairedObjectHolder holder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(topRecycler.getChildAt(i));
            if (!holder.isPaired()) geoObjects.add(holder.getGeoObject());
        }
        return geoObjects;
    }

    /**
     * Updates the top-recycler with toggle-able buttons with names of geo-objects.
     *
     * Set listeners to the view-holders. When one is clicked, select this one and
     * deselect all others. Also deselect all objects in the map.
     *
     * Then, when top-buttons are laid out, update the map.
     *
     * @param geoObjects
     */
    private void updateTopButtonsAndMap_pairIt(List<GeoObject> geoObjects) {
        topRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        UnpairedObjectAdapter adapter = new UnpairedObjectAdapter(geoObjects);
        topRecycler.setAdapter(adapter);
        topRecycler.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateMap_pairIt();
                topRecycler.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        topRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, bottomRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                View clickedView = topRecycler.getChildAt(position);
                UnpairedObjectHolder clickedHolder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(clickedView);
                boolean clickedIsSelected = clickedHolder.isSelected();
                deselectAllUnpairedButtons();
                clickedHolder.setSelected(!clickedIsSelected);
            }

            @Override
            public void onLongItemClick(View view, int position) {
            }
        }));
    }

    /**
     * Deselect all buttons in the top recycler.
     */
    private void deselectAllUnpairedButtons() {
        for (int i = 0; i < topRecycler.getChildCount(); i++) {
            UnpairedObjectHolder holder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(topRecycler.getChildAt(i));
            holder.setSelected(false);
        }
    }

    private class UnpairedObjectAdapter extends RecyclerView.Adapter<UnpairedObjectHolder> {
        private List<GeoObject> geoObjects;

        public UnpairedObjectAdapter(List<GeoObject> geoObjects) {
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
        public void setSelected(boolean isSelected) {
            this.isSelected = isSelected;
            updateAppearance();
        }

        private void updateAppearance() {
            if (isSelected) {
                LocaUtils.setDimmed(button);
            }
            else {
                LocaUtils.unsetDimmed(button);
            }
        }

        /**
         * Hide this view-holder when correctly paired.
         */
        public void setPaired() {
            itemView.setVisibility(View.INVISIBLE);
            this.isPaired = true;
            this.isSelected = false;
        }

        public boolean isPaired() {
            return isPaired;
        }
    }

    /**
     * Updated map with geoObjects that isn't paired, which is defined by the view-holders in
     * the top-recycler.
     *
     * Add listeners to map-objects - remove if becomes paired, otherwise incorrect.
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
            Long answeredId = markersLookupMap.get(marker.getId());
            if (answeredId == null) return true;

            AppDatabase db = AppDatabase.getInstance(this);
            GeoObject geoObject = db.geoDao().load(answeredId);

            List<Annotation> geoObjectAnnotations = LocaUtils.geoObjectAnnotations(geoObject.getId(), mapboxMap, markersLookupMap, polylinesLookupMap);
            onPairItAnnotationClick(geoObjectAnnotations, geoObject);
            return true;
        });

        mapboxMap.setOnPolylineClickListener(polyline -> {
            Long answeredId = polylinesLookupMap.get(polyline.getId());
            if (answeredId == null) return;

            AppDatabase db = AppDatabase.getInstance(this);
            GeoObject geoObject = db.geoDao().load(answeredId);

            List<Annotation> annotations = LocaUtils.geoObjectAnnotations(geoObject.getId(), mapboxMap, markersLookupMap, polylinesLookupMap);
            onPairItAnnotationClick(annotations, geoObject);
        });
    }

    /**
     * Called when a map-annotation is pressed.
     * @param annotations
     * @param geoObject
     */
    private void onPairItAnnotationClick(List<Annotation> annotations, GeoObject geoObject) {
        LocaUtils.blinkAnnotations(annotations, mapboxMap, this);

        UnpairedObjectHolder selectedHolder = findSelectedTopRecyclerHolder();

        if (selectedHolder == null) {
            //ignore map-click
        }
        else if (!selectedHolder.getGeoObject().equals(geoObject)) {
            String errorText = geoObject.getName();
            Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show();
            selectedHolder.setSelected(false);
            RunningQuizControl.reportIncorrectPairItAnswer(this);
        }
        else {
            String correctText = String.format("%s %s", getString(R.string.correct), geoObject.getName());
            Toast.makeText(this, correctText, Toast.LENGTH_LONG).show();
            selectedHolder.setPaired();
            mapboxMap.removeAnnotations(annotations);

            if (allTopItemsPaired())
                nextQuestionButton.show();
        }
    }

    /**
     * @return True if every item in the top-recycler has been paired.
     */
    private boolean allTopItemsPaired() {
        for (int i = 0; i < topRecycler.getChildCount(); i++) {
            UnpairedObjectHolder holder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(topRecycler.getChildAt(i));
            if (!holder.isPaired()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return The one selected holder, or NULL if none.
     */
    private UnpairedObjectHolder findSelectedTopRecyclerHolder() {
        for (int i = 0; i < topRecycler.getChildCount(); i++) {
            UnpairedObjectHolder holder = (UnpairedObjectHolder)topRecycler.getChildViewHolder(topRecycler.getChildAt(i));
            if (holder.isSelected()) {
                if (holder.isPaired()) return null;
                else return holder;
            }
        }
        return null;
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
            LocaUtils.fadeOutFadeIn(this, () -> update());
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
