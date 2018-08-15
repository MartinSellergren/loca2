package com.localore.localore;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Quiz;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TappingActivity extends AppCompatActivity {

    private static final String QUIZ_CATEGORY_TYPE_PARAM_KEY = "com.localore.localore.CreateExerciseService.QUIZ_CATEGORY_TYPE_PARAM_KEY";

    /**
     * Quiz-category-type of all geo-objects.
     */
    private int quizCategoryType;

    /**
     * Active exercise.
     */
    private Exercise exercise;

    private MapView mapView;
    private MapboxMap mapboxMap;

    /**
     * Mapping marker/polyline id to geo-object id.
     */
    private Map<Long, Long> markersMap = new HashMap<>();
    private Map<Long, Long> polylinesMap = new HashMap<>();


    /**
     * Start activity through this to pass quiz-category correctly.
     * @param quizCategoryType
     * @param oldActivity
     */
    public static void start(int quizCategoryType, Activity oldActivity) {
        Intent intent = new Intent(oldActivity, TappingActivity.class);
        intent.putExtra(QUIZ_CATEGORY_TYPE_PARAM_KEY, quizCategoryType);
        LocaUtils.fadeInActivity(intent, oldActivity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tapping);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.quizCategoryType = getIntent().getIntExtra(QUIZ_CATEGORY_TYPE_PARAM_KEY, -1);
        if (quizCategoryType == -1) throw new RuntimeException("Start activity with freshStart()");

        this.exercise = SessionControl.loadExercise(this);
        setTitle(exercise.getName());

        this.mapView = findViewById(R.id.mapView_quiz);
        mapView.onCreate(null);
    }

    //region create exercise-button

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tapping_actions, menu);

        MenuItem item = menu.findItem(R.id.menuItem_switch_tapping);
        RelativeLayout layout = (RelativeLayout)item.getActionView();
        Switch switch_tapping = layout.findViewById(R.id.switch_tapping);
        boolean nextLevelObjects = switch_tapping.isChecked();

        TappingActivity.this.mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                TappingActivity.this.mapboxMap = mapboxMap;
                LocaUtils.flyToFitShape(exercise.getWorkingArea(), mapboxMap, LocaUtils.LONG_FLY_TIME);
                updateMap(nextLevelObjects);

                mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        Long geoObjectId = markersMap.get(marker.getId());
                        if (geoObjectId != null) onGeoObjectClick(geoObjectId);
                        return true;
                    }
                });

                mapboxMap.setOnPolylineClickListener(new MapboxMap.OnPolylineClickListener() {
                    @Override
                    public void onPolylineClick(@NonNull Polyline polyline) {
                        Long geoObjectId = polylinesMap.get(polyline.getId());
                        if (geoObjectId != null) onGeoObjectClick(geoObjectId);
                    }
                });
            }
        });

        switch_tapping.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                boolean nextLevelObjects = isChecked;
                updateMap(nextLevelObjects);
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Display toast on clicked geo-object.
     * @param geoObjectId
     */
    public void onGeoObjectClick(long geoObjectId) {
        AppDatabase db = AppDatabase.getInstance(this);
        GeoObject geoObject = db.geoDao().load(geoObjectId);

//        String str = String.format("%s (%s)",
//                geoObject.getName(),
//                geoObject.getCategory());
        String str = geoObject.toString();
        Toast.makeText(TappingActivity.this, str, Toast.LENGTH_SHORT).show();
    }


    /**
     * @param nextLevelObjects Next vs pasts level objects.
     */
    private void updateMap(boolean nextLevelObjects) {
        AppDatabase db = AppDatabase.getInstance(this);
        List<GeoObject> geoObjects = ExerciseControl.loadGeoObjectsForTapping(
                exercise.getId(), quizCategoryType, nextLevelObjects, db);

        //todo: remove
        if (nextLevelObjects == false) {
            List<Quiz> quizzes = ExerciseControl.loadQuizzesInExercise(exercise.getId(), db);
            geoObjects = new ArrayList<>();

            for (Quiz quiz : quizzes) {
                List<GeoObject> geoObjects1 = db.geoDao().loadWithQuiz(quiz.getId());
                geoObjects.addAll(geoObjects1);
            }
        }

        this.mapboxMap.clear();
        this.markersMap.clear();
        this.polylinesMap.clear();

        //LocaUtils.highlightWorkingArea(mapboxMap, exercise.getWorkingArea());
        LocaUtils.addGeoObjects(geoObjects, mapboxMap, markersMap, polylinesMap, this);

        if (nextLevelObjects)
            Toast.makeText(TappingActivity.this, "Next level", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(TappingActivity.this, "Past levels", Toast.LENGTH_SHORT).show();
    }

    //region handle mapView's lifecycle
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
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


    @Override
    public void onBackPressed() {
        SelectExerciseActivity.start(this);
    }
}
