package com.localore.localore;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.NodeShape;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.List;

public class CreateExerciseActivity extends AppCompatActivity {

    private MapView mapView;
    private EditText editText_exerciseName;
    private MenuItem menuItem_createExercise;

    /**
     * List of names of existing exercises.
     */
    private List<String> existingExerciseNames;


    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exercise);
        setTitle(getString(R.string.new_exercise));

        this.editText_exerciseName = findViewById(R.id.editText_exerciseName);
        this.menuItem_createExercise = findViewById(R.id.menuItem_createExercise);
        this.mapView = findViewById(R.id.mapView_createExercise);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                customizeMap(mapboxMap);
            }
        });

        AppDatabase db = AppDatabase.getInstance(this);
        long userId = SessionControl.load(db).getUserId();
        this.existingExerciseNames = db.exerciseDao().loadNamesWithUser(userId);
    }

    //region create exercise-button

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_exercise_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItem_createExercise) {
            startLoadingNewExerciseActivity();
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    /**
     * Customize the map.
     *
     * - Set to user's location at min allowed zoom.
     * - Make it clickable to insert nodes: select an area.
     *   - "Fill area" if >2 nodes (auto-close path).
     *   - Don't allow clicks that make segments crossed.
     *   - Test updateDoneButton() after each new node.
     * - If nodes: floating button to delete all nodes.
     * - If too zoomed out: Floating button "zoom in" to allowed zoom + dimmed map.
     *
     * @param mapboxMap
     */
    private void customizeMap(MapboxMap mapboxMap) {

    }

    /**
     * Enable/disable create-exercise-button based on user-input.
     */
    private void updateDoneButton() {
        String exerciseName = this.editText_exerciseName.getText().toString();
        NodeShape workingArea = selectedShape();
        this.menuItem_createExercise.setEnabled(okExerciseName(exerciseName) && okWorkingArea(workingArea));
    }

    /**
     * @param name
     * @return True if name is ok for a new exercise (i.e is unique).
     */
    private boolean okExerciseName(String name) {
        return !this.existingExerciseNames.contains(name);
    }

    /**
     * @param workingArea
     * @return True if shape is ok as a working area for a new exercise.
     */
    private boolean okWorkingArea(NodeShape workingArea) {
        return false;
    }

    /**
     * @return Shape constructed from user-specified nodes in map.
     */
    private NodeShape selectedShape() {
        return LocaUtils.getWorkingArea();
    }

    //region create the exercise (after name and area specified)

    /**
     * Starts the loading-new-exercise activity.
     */
    public void startLoadingNewExerciseActivity() {
        String name = this.editText_exerciseName.getText().toString();
        NodeShape workingArea = selectedShape();

        LoadingNewExerciseActivity.freshStart(name, workingArea, this);
    }

    //endregion

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
