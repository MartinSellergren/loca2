package com.localore.localore;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.maps.MapView;

import java.util.List;

public class TappingActivity extends AppCompatActivity {

    private static final String QUIZ_CATEGORY_TYPE_PARAM_KEY = "com.localore.localore.CreateExerciseService.QUIZ_CATEGORY_TYPE_PARAM_KEY";

    private FloatingActionButton toggleZoomButton;

    /**
     * Quiz-category-type of all geo-objects.
     */
    private int quizCategoryType;

    /**
     * Active exercise.
     */
    private Exercise exercise;

    private MapView mapView;
    private GeoObjectMap geoObjectMap;



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
        LocaUtils.colorActionBar(exercise.getColor(), this);
        LocaUtils.colorStatusBar(exercise.getColor(), this);

        this.toggleZoomButton = findViewById(R.id.button_tapping_toggleZoom);
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

        mapView.getMapAsync(mapboxMap -> {
            NodeShape workingArea = SessionControl.loadExercise(this).getWorkingArea();
            boolean showBorder = true;
            FloatingActionButton toggleZoomButton = findViewById(R.id.button_tapping_toggleZoom);
            geoObjectMap = new GeoObjectMap(mapboxMap, Color.GRAY, workingArea, showBorder, toggleZoomButton, this);
            geoObjectMap.flyToOverview(GeoObjectMap.LONG_FLY_TIME);
            updateMap(nextLevelObjects);

            geoObjectMap.setOnGeoObjectClick(new GeoObjectMap.OnGeoObjectClickListener() {

                /**
                 * Display toast on clicked geo-object.
                 * Also on working-area-border click (id -1).
                 *
                 * @param geoObjectId
                 */
                @Override
                public void onGeoObjectClick(long geoObjectId) {
                    GeoObject geoObject = AppDatabase.getInstance(TappingActivity.this).geoDao().load(geoObjectId);
                    //geoObjectMap.blinkGeoObject(geoObjectId);
                    geoObjectMap.flashGeoObjectInColor(geoObject.getId(), geoObject.getColor());

                    String str = String.format("%s (%s)",
                            geoObject.getName(),
                            geoObject.getCategory());
                    Toast.makeText(TappingActivity.this, str, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onWorkingAreaBorderClick() {
                    Toast.makeText(TappingActivity.this, R.string.exercise_border, Toast.LENGTH_LONG).show();
                }
            });

            switch_tapping.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                boolean updateWithNextLevelObjects = isChecked;
                updateMap(updateWithNextLevelObjects);
            });
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
     * @param nextLevelObjects Next vs pasts level objects.
     */
    private void updateMap(boolean nextLevelObjects) {
        AppDatabase db = AppDatabase.getInstance(this);
        List<GeoObject> geoObjects = ExerciseControl.loadGeoObjectsForTapping(
                exercise.getId(), quizCategoryType, nextLevelObjects, db);
        geoObjectMap.addGeoObjects(geoObjects);

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
        ExerciseActivity.start(this);
    }
}
