package com.localore.localore;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TappingActivity extends AppCompatActivity {

    private static final String QUIZ_CATEGORY_TYPE_PARAM_KEY = "com.localore.localore.CreateExerciseService.QUIZ_CATEGORY_TYPE_PARAM_KEY";

    /**
     * Padding around working area for the initial camera-view.
     */
    private static final int WORKING_AREA_CAMERA_PADDING = 50;

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
     * Start activity through this to pass quiz-category correctly.
     * @param quizCategoryType
     * @param context
     */
    public static void start(int quizCategoryType, Context context) {
        Intent intent = new Intent(context, TappingActivity.class);
        intent.putExtra(QUIZ_CATEGORY_TYPE_PARAM_KEY, quizCategoryType);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tapping);

        this.quizCategoryType = getIntent().getIntExtra(QUIZ_CATEGORY_TYPE_PARAM_KEY, -1);
        if (quizCategoryType == -1) throw new RuntimeException("Start activity with start()");

        AppDatabase db = AppDatabase.getInstance(this);
        this.exercise = SessionControl.loadExercise(db);
        setTitle(exercise.getName());

        this.mapView = findViewById(R.id.mapView_tapping);
        mapView.onCreate(savedInstanceState);
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
                initCameraView();
                setGeoObjects(nextLevelObjects);
            }
        });


        switch_tapping.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                boolean nextLevelObjects = isChecked;
                setGeoObjects(nextLevelObjects);
            }
        });

        return true;
    }

    /**
     * Set camera view to fit working area of exercise.
     */
    private void initCameraView() {
        double[] workingAreaBounds = this.exercise.getWorkingArea().getBounds();
        LatLngBounds latLngBounds = LocaUtils.toLatLngBounds(workingAreaBounds);
        this.mapboxMap.moveCamera(CameraUpdateFactory
                .newLatLngBounds(latLngBounds, WORKING_AREA_CAMERA_PADDING));
    }

    /**
     * @param nextLevelObjects Next vs pasts level objects.
     */
    private void setGeoObjects(boolean nextLevelObjects) {
        AppDatabase db = AppDatabase.getInstance(this);

        List<GeoObject> geoObjects = ExerciseControl.loadGeoObjectsForTapping(
                exercise.getId(), quizCategoryType, nextLevelObjects, db);

        //for fun
        if (nextLevelObjects == false) {
            List<Quiz> quizzes = ExerciseControl.loadQuizzesInExercise(exercise.getId(), db);
            geoObjects = new ArrayList<>();

            for (Quiz quiz : quizzes) {
                List<GeoObject> geoObjects1 = db.geoDao().loadWithQuiz(quiz.getId());
                geoObjects.addAll(geoObjects1);
            }
        }

        mapboxMap.clear();
        addGeoObjects(geoObjects);

        if (nextLevelObjects)
            Toast.makeText(TappingActivity.this, "Next level", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(TappingActivity.this, "Past levels", Toast.LENGTH_SHORT).show();
    }

    /**
     * All all geo-objects to the map.
     * @param geoObjects
     */
    private void addGeoObjects(List<GeoObject> geoObjects) {
        for (GeoObject geoObject : geoObjects) addGeoObject(geoObject);
    }

    /**
     * Add a geo-object to the map.
     * @param geoObject
     */
    private void addGeoObject(GeoObject geoObject) {
        int color = Color.argb(255, LocaUtils.randi(256), LocaUtils.randi(256), LocaUtils.randi(256)); //todo: color of geo-objects

        for (NodeShape nodeShape : geoObject.getShapes()) {
            if (nodeShape.getNodes().size() == 1) {
                addMarker(nodeShape.getNodes().get(0), geoObject.getName(), color);
            }
            else {
                addPolyline(nodeShape.getNodes(), geoObject.getName(), color);
            }
        }
    }

    /**
     * Adds a clickable marker. Show name on click.
     * @param node
     * @param name
     * @param color
     */
    private void addMarker(double[] node, String name, int color) {
        this.mapboxMap.addMarker(new MarkerOptions()
                //.icon(WORKING_AREA_NODE_ICON_FRONTIER)
                .position(LocaUtils.toLatLng(node)));
    }

    /**
     * Adds a clickable polyline. Show name on click.
     * @param nodes
     * @param name
     * @param color
     */
    private void addPolyline(List<double[]> nodes, String name, int color) {
        this.mapboxMap.addPolyline(new PolylineOptions()
                .addAll(LocaUtils.toLatLngs(nodes))
                .color(color)
                .width(2));
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
}
