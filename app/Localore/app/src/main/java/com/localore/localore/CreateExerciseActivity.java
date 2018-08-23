package com.localore.localore;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.ArrayList;
import java.util.List;

public class CreateExerciseActivity extends AppCompatActivity {

    /**
     * Above this level, exercise-creation is not allowed.
     */
    private static final double MIN_WORKING_AREA_ZOOM_LEVEL = 10;

    private EditText editText_exerciseName;
    private MenuItem menuItem_createExercise;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FloatingActionButton button_clearNodes;
    private FloatingActionButton button_validZoom;

    /**
     * List of names of existing exercises.
     */
    private List<String> existingExerciseNames;

    /**
     * Icons for nodes in working-area.
     */
    private Icon WORKING_AREA_NODE_ICON_FRONTIER;
    private Icon WORKING_AREA_NODE_ICON;

    /**
     * Color of drawn path.
     */
    private static final int SEGMENTS_COLOR = LocaUtils.BLUE_COLOR;
    private static final int FRONTIER_COLOR = Color.BLACK;

    /**
     * Dim-overlay over whole map.
     */
    private static final PolygonOptions DIM_OVERLAY_POLYGON = new PolygonOptions()
            .addAll(LocaUtils.WORLD_CORNER_COORDINATES)
            .fillColor(LocaUtils.DIM_OVERLAY_COLOR);


    /**
     * Map-tapping-events ignored if set to false.
     */
    private volatile boolean mapTappingEnabled = false;

    private boolean okExerciseName = false;
    private boolean okWorkingArea = false;


    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_create_exercise);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.editText_exerciseName = findViewById(R.id.editText_exerciseName);
        this.mapView = findViewById(R.id.mapView_quiz);
        this.button_clearNodes = findViewById(R.id.button_clearNodes);
        this.button_validZoom = findViewById(R.id.button_validZoom);

        int iconDim = GeoObjectMap.STANDALONE_NODE_ICON_DIAMETER;
        this.WORKING_AREA_NODE_ICON = LocaUtils.generateCircleIcon(SEGMENTS_COLOR, iconDim, this);
        this.WORKING_AREA_NODE_ICON_FRONTIER = LocaUtils.generateCircleIcon(FRONTIER_COLOR, iconDim, this);

        mapView.onCreate(null);
        mapView.getMapAsync(mapboxMap -> {
            CreateExerciseActivity.this.mapboxMap = mapboxMap;
            initializeMap();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        AppDatabase db = AppDatabase.getInstance(this);
        long userId = SessionControl.load(db).getUserId();
        this.existingExerciseNames = db.exerciseDao().loadNamesWithUser(userId);
    }

    //region create exercise-button

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_exercise_actions, menu);
        this.menuItem_createExercise = menu.findItem(R.id.menuItem_createExercise);
        updateCreateExerciseButton();

        editText_exerciseName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                okExerciseName = validateExerciseName(charSequence.toString());
                updateCreateExerciseButton();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItem_createExercise) {
            startLoadingNewExerciseActivity();
        }
        else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    /**
     * Initialize the map.
     *
     * - Make it clickable to insert nodes: select an area.
     *   - "Fill area" if >2 nodes (auto-close path).
     *   - Don't allow clicks that make segments crossed.
     *   - Test updateCreateExerciseButton() after each new node.
     * - If nodes: floating button to delete all nodes.
     * - If too zoomed out: Floating button "zoom in" to allowed zoom + dimmed map.
     */
    private void initializeMap() {
        addZoomControl();
        flyToUserLocation();
        addWorkingAreaTapping();
    }

    //region fly to user's current location

    /**
     * Requests permission and flies to user location if granted or already have it.
     */
    private void flyToUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        flyToUserLocation_();
    }

    /**
     * Flies to user location. Does nothing if can't access user-location.
     */
    private void flyToUserLocation_() {
        try {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            String locationProvider = LocationManager.GPS_PROVIDER;
            Location location = locationManager.getLastKnownLocation(locationProvider);
            if (location != null) {
                flyToLocation(location, MIN_WORKING_AREA_ZOOM_LEVEL + 0.001, GeoObjectMap.LONG_FLY_TIME);
                return;
            }

            locationProvider = LocationManager.NETWORK_PROVIDER;
            location = locationManager.getLastKnownLocation(locationProvider);
            if (location != null) {
                flyToLocation(location, MIN_WORKING_AREA_ZOOM_LEVEL + 0.001, GeoObjectMap.LONG_FLY_TIME);
                return;
            }

            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    flyToLocation(location, MIN_WORKING_AREA_ZOOM_LEVEL + 0.001, GeoObjectMap.LONG_FLY_TIME);
                    locationManager.removeUpdates(this);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, Integer.MAX_VALUE, Integer.MAX_VALUE, locationListener);
        }
        catch (SecurityException e) {}
    }

    /**
     * Fly to specified location on map.
     * @param location
     * @param zoom
     * @param flyTime
     */
    private void flyToLocation(Location location, double zoom, int flyTime) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition position = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), flyTime);
    }

    /**
     * Called when answer from the permission-request of current location.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            flyToUserLocation_();
    }

    //endregion

    /**
     * Tap on map to create a marker and add this node to the working-area.
     * - Add a clear-nodes button after first tap (connected with lines).
     * - Lock gestures after first tap.
     * - Update okWorkingArea and createExerciseButton after each new node.
     */
    private void addWorkingAreaTapping() {
        this.mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {

            /**
             * Called when the user clicks on the map view.
             * @param point The projected map coordinate the user clicked on.
             */
            @Override
            public void onMapClick(@NonNull LatLng point) {
                if (!CreateExerciseActivity.this.mapTappingEnabled) return;

                mapboxMap.addMarker(new MarkerOptions()
                        .icon(WORKING_AREA_NODE_ICON_FRONTIER)
                        .position(point));

                List<Marker> markers = mapboxMap.getMarkers();

                if (markers.size() == 1) {
                    button_clearNodes.show();
                    enableGestures(false);
                }

                if (markers.size() > 1) {
                    Marker prevMarker = markers.get(markers.size() - 2);
                    prevMarker.setIcon(WORKING_AREA_NODE_ICON);

                    LatLng prevPoint = prevMarker.getPosition();
                    mapboxMap.addPolyline(new PolylineOptions()
                            .add(prevPoint, point)
                            .color(SEGMENTS_COLOR)
                            .width(2));
                }

                List<Polyline> polylines = mapboxMap.getPolylines();

                if (markers.size() > 3) {
                    Polyline prevClosingLine = polylines.get(polylines.size() - 2);
                    mapboxMap.removePolyline(prevClosingLine);
                }

                if (markers.size() > 2) {
                    LatLng firstPoint = markers.get(0).getPosition();
                    mapboxMap.addPolyline(new PolylineOptions()
                            .add(point, firstPoint)
                            .color(SEGMENTS_COLOR)
                            .width(2));
                }

                CreateExerciseActivity.this.okWorkingArea = okWorkingArea(markers);
                if (!okWorkingArea) Toast.makeText(CreateExerciseActivity.this, R.string.invalid_shape, Toast.LENGTH_LONG).show();
                updateCreateExerciseButton();
            }
        });
    }

    /**
     * Enable/disable user-gestures: scrolling/ zooming. (Other gestures disabled in xml).
     * @param enable
     */
    private void enableGestures(boolean enable) {
        mapboxMap.getUiSettings().setScrollGesturesEnabled(enable);
        mapboxMap.getUiSettings().setZoomGesturesEnabled(enable);
    }

    /**
     * Too zoomed out:
     *  - Dim-overlay when too zoomed out too much.
     *  - Valid-zoom-button.
     *  - Disable tapping (new nodes).
     */
    private void addZoomControl() {
        this.mapboxMap.addOnCameraMoveListener(new MapboxMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                boolean tooZoomedOut = mapboxMap.getCameraPosition().zoom < MIN_WORKING_AREA_ZOOM_LEVEL;
                showValidZoomButton(tooZoomedOut);
                CreateExerciseActivity.this.mapTappingEnabled = !tooZoomedOut;
                showDimOverlay(tooZoomedOut);
            }
        });
    }

    /**
     * Show/hide an dim-overlay covering the whole map.
     * @param show
     */
    private void showDimOverlay(boolean show) {
        List<Polygon> polygons = this.mapboxMap.getPolygons();

        if (polygons.size() == 0 && show) {
            mapboxMap.addPolygon(DIM_OVERLAY_POLYGON);
        }
        else if (polygons.size() == 1 && !show) {
            mapboxMap.removePolygon(polygons.get(0));
        }
    }

    /**
     * Show or hide the valid-zoom-button.
     * @param show
     */
    private void showValidZoomButton(boolean show) {
        if (this.button_validZoom != null) {
            if (show && !button_validZoom.isShown()) button_validZoom.show();
            else if (!show && button_validZoom.isShown()) button_validZoom.hide();
        }
    }

    /**
     * Clear all nodes in tapped working-area.
     * @param view
     */
    public void onClearNodesButtonClick(View view) {
        this.mapboxMap.clear();
        this.button_clearNodes.hide();
        this.okWorkingArea = false;
        updateCreateExerciseButton();
        enableGestures(true);
    }

    /**
     * Zoom to max valid level (zoom in).
     * @param view
     */
    public void onValidZoomButtonClick(View view) {
        CameraPosition position = new CameraPosition.Builder()
                .zoom(MIN_WORKING_AREA_ZOOM_LEVEL + 0.001)
                .build();

        this.mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), GeoObjectMap.SHORT_FLY_TIME);
    }

    /**
     * Enable/disable create-exercise-button based on user-input.
     * Call after every change.
     * @pre okExerciseName and okWorkingArea set according to state.
     */
    private void updateCreateExerciseButton() {
        if (okExerciseName && okWorkingArea) {
            this.menuItem_createExercise.setEnabled(true);
            menuItem_createExercise.getIcon().setAlpha(255);
        }
        else {
            this.menuItem_createExercise.setEnabled(false);
            menuItem_createExercise.getIcon().setAlpha(100);
        }
    }

    /**
     * @param name
     * @return True if name is ok for a new exercise (i.e is unique).
     *         Toast text describes problem.
     */
    private boolean validateExerciseName(String name) {
        name = name.trim();
        boolean exists = this.existingExerciseNames.contains(name);
        if (exists) Toast.makeText(this, R.string.name_already_exists, Toast.LENGTH_LONG).show();
        return !exists && name.length() > 0;
    }

    /**
     * @param markers
     * @return True if shape is ok as a working area for a new exercise.
     */
    private boolean okWorkingArea(List<Marker> markers) {
        if (markers.size() < 3) return false;
        return toNodeShape(markers) != null;
    }

    /**
     * Markers to node-shape.
     * @param markers
     * @return
     */
    private NodeShape toNodeShape(List<Marker> markers) {
        List<double[]> nodes = new ArrayList<>();

        for (Marker marker : markers) {
            LatLng latLng = marker.getPosition();
            nodes.add(new double[]{
                    latLng.getLongitude(),
                    latLng.getLatitude()});
        }

        if (NodeShape.validNodeShapeNodes(nodes)) {
            return new NodeShape(nodes);
        }
        else {
            return null;
        }
    }

    /**
     * @return Shape constructed from user-specified nodes in map.
     */
    private NodeShape selectedShape() {
        return toNodeShape(this.mapboxMap.getMarkers());
    }

    //region create the exercise (after name and area specified)

    /**
     * Starts the loading-new-exercise activity.
     */
    public void startLoadingNewExerciseActivity() {
        String name = this.editText_exerciseName.getText().toString().trim();
        NodeShape workingArea = selectedShape();
        if (workingArea == null) throw new RuntimeException("Dead-end");

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
     * Start the activity.
     * @param oldActivity
     */
    public static void start(Activity oldActivity) {
        LocaUtils.fadeInActivity(CreateExerciseActivity.class, oldActivity);
    }

    /**
     * Need to start select-activity again because it might not exist.
     */
    @Override
    public void onBackPressed() {
        SelectExerciseActivity.start(this);
    }
}
