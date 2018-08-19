package com.localore.localore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.Question;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.model.Session;
import com.localore.localore.model.User;
import com.localore.localore.modelManipulation.SessionControl;
import com.mapbox.mapboxsdk.annotations.Annotation;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;

public class LocaUtils {

    /**
     * Exception thrown when work as been interrupted. E.g when user press exit-button.
     */
    public static class WorkInterruptedException extends Exception {
        public WorkInterruptedException(String msg) { super(msg); }
        public WorkInterruptedException() { super(); }
    }

    public static Random random = new Random();

    public static final int BLUE_COLOR = Color.parseColor("#3bb2d0");
    public static final int RED_COLOR = Color.parseColor("#AF0000");


    /**
     * Color for dim-overlay.
     */
    public static final int DIM_OVERLAY_COLOR = Color.parseColor("#804c4c4c");

    /**
     * Latitude-max for web-mercator projection.
     */
    private static final double LAT_MAX = 85.051129;

    /**
     * Time when quitSecondTime() is called first time.
     */
    private static long firstQuitRequestTime;

    /**
     * quitSecondTime() quits app if second call arrives before this time (in ms) after the previous.
     */
    private static final int MAX_TIME_BETWEEN_QUIT_REQUESTS = 1900;

    /**
     * Points for dim-overlay.
     */
    public static final List<LatLng> WORLD_CORNER_COORDINATES = new ArrayList<LatLng>() {
        {
            add(new LatLng(-LAT_MAX, -180));
            add(new LatLng(-LAT_MAX, 180));
            add(new LatLng(LAT_MAX, 180));
            add(new LatLng(LAT_MAX, -180));
        }
    };

    /**
     * Quit app if two calls are made within certain max-time.
     */
    public static void quitSecondTime(Context context) {
        if (firstQuitRequestTime + MAX_TIME_BETWEEN_QUIT_REQUESTS > System.currentTimeMillis()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        else {
            Toast.makeText(context, R.string.press_once_again_to_exit, Toast.LENGTH_SHORT).show();
            firstQuitRequestTime = System.currentTimeMillis();
        }
    }

    //region fade transitions

    public static int QUICK_TALK = 500;
    public static int LONG_TALK = 1000;

    /**
     * Fade in overlay on top of screen, run action, fade back.
     * @param overlay
     * @param activity
     * @param betweenAction
     */
    private static void fadeOutFadeInOverlay(ConstraintLayout overlay, Activity activity, boolean fadeBack, Runnable betweenAction) {
        int fadeDuration = 500;

        View absoluteRoot = activity.findViewById(android.R.id.content);
        if (absoluteRoot == null)
            absoluteRoot = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        ConstraintLayout root = (ConstraintLayout)((ViewGroup)(absoluteRoot)).getChildAt(0);
        overlay.setAlpha(0);
        ViewCompat.setElevation(overlay, 100);
        //overlay.setElevation(0);
        root.addView(overlay, -1);
        root.invalidate();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
                overlay.animate()
                        .alpha(1)
                        .setDuration(fadeDuration)
                        .withEndAction(() -> {
                            if (betweenAction != null) {
                                betweenAction.run();
                            }
                            if (fadeBack) {
                                overlay.animate()
                                        .alpha(0f)
                                        .setDuration(fadeDuration)
                                        .withEndAction(() -> {
                                            root.removeView(overlay);
                                            root.invalidate();
                                        })
                                        .start();
                            }
                        })
                        .start();
//            }
//        }, 0);
    }

    /**
     * Fades out screen, runs between-action, fades back.
     * @param activity
     * @param betweenAction
     */
    public static void fadeOutFadeIn(Activity activity, Runnable betweenAction) {
        ConstraintLayout overlay = new ConstraintLayout(activity);
        overlay.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT));
        //overlay.setBackgroundColor(activity.getResources().getColor(R.color.colorPrimary));
        overlay.setBackgroundColor(Color.WHITE);
        overlay.setClickable(true);

        fadeOutFadeInOverlay(overlay, activity, true, betweenAction);
    }

    /**
     * Fades out screen, show text, fades back.
     * @param text
     */
    public static void talk(String text, int talkDuration, Activity activity) {
        ConstraintLayout overlay = talkOverlay(text, activity);
        fadeOutFadeInOverlay(overlay, activity, true, () -> {
            SystemClock.sleep(talkDuration);
        });
    }

    /**
     * Talk, then fade in new activity.
     * @param text
     * @param newActivityClass
     * @param oldActivity
     */
    public static void fadeInActivityWithTalk(String text, int talkDuration, Class<?> newActivityClass, Activity oldActivity) {
        ConstraintLayout talkOverlay = talkOverlay(text, oldActivity);
        fadeOutFadeInOverlay(talkOverlay, oldActivity, false, () -> {
            SystemClock.sleep(talkDuration);
            fadeInActivity(newActivityClass, oldActivity);
        });
    }

    /**
     * @param text
     * @param activity
     * @return Talk-view with text.
     */
    private static ConstraintLayout talkOverlay(String text, Activity activity) {
        ConstraintLayout overlay = new ConstraintLayout(activity);
        overlay.setId(generateViewId());
        overlay.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(Color.WHITE);
        overlay.setClickable(true);

        TextView textView = new TextView(activity);
        textView.setId(generateViewId());
        textView.setText(text);

        textView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT));
        overlay.addView(textView);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(overlay);

        constraintSet.connect(textView.getId(), ConstraintSet.TOP, overlay.getId(), ConstraintSet.TOP);
        constraintSet.connect(textView.getId(), ConstraintSet.BOTTOM, overlay.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(textView.getId(), ConstraintSet.LEFT, overlay.getId(), ConstraintSet.LEFT);
        constraintSet.connect(textView.getId(), ConstraintSet.RIGHT, overlay.getId(), ConstraintSet.RIGHT);
        constraintSet.applyTo(overlay);

        return overlay;
    }

    /**
     * Fade out old activity, start new new and fade it in. Also finishes old activity
     * (removed from task-stack).
     * @param newActivityClass
     * @param oldActivity
     */
    public static void fadeInActivity(Class<?> newActivityClass, Activity oldActivity) {
        Intent intent = new Intent(oldActivity, newActivityClass);
        fadeInActivity(intent, oldActivity);
    }
    public static void fadeInActivity(Intent intent, Activity oldActivity) {
        oldActivity.startActivity(intent);
        oldActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        oldActivity.finish();
    }

    //endregion

    /**
     * Adds a border around a view.
     * @param view
     */
    public static void setDimmed(View view) {
        view.setAlpha(0.3f);
    }

    /**
     * @param view
     */
    public static void unsetDimmed(View view) {
        view.setAlpha(1f);
    }

    /**
     * Make the view stand out. Can't be undone.
     * @param view
     */
    public static void setHighlighted(View view) {
        GradientDrawable border = new GradientDrawable();
        border.setColor(0xFFFFFFFF);
//        border.setStroke(10, color);
//        border.setCornerRadius(2);
        view.setBackground(border);
    }

    /**
     * @param mapboxMap
     * @param workingArea
     */
    public static void highlightWorkingArea(MapboxMap mapboxMap, NodeShape workingArea) {
//        JsonArray tl = new JsonArray(); tl.add(-180); tl.add(-LAT_MAX);
//        JsonArray tr = new JsonArray(); tr.add(180); tr.add(-LAT_MAX);
//        JsonArray br = new JsonArray(); br.add(180); br.add(LAT_MAX);
//        JsonArray bl = new JsonArray(); bl.add(-180); bl.add(LAT_MAX);
//        JsonArray outerPolygon = new JsonArray();
//        outerPolygon.add(tl); outerPolygon.add(tr); outerPolygon.add(br); outerPolygon.add(bl);
//
//        JsonArray hole = new JsonArray();
//        for (double[] node : workingArea.asClosed().getNodes()) {
//            JsonArray holeNode = new JsonArray();
//            holeNode.add(node[0]);
//            holeNode.add(node[1]);
//            hole.add(holeNode);
//        }
//
//
//        JsonArray coordinates = new JsonArray();
//        coordinates.add(outerPolygon);
//        coordinates.add(hole);
//
//        JsonObject top = new JsonObject();
//        top.addProperty("type", "Feature");
//
//        JsonObject properties = new JsonObject();
//        properties.addProperty("name", "GeoObject");
//        top.add("properties", properties);
//
//        JsonObject geometry = new JsonObject();
//        geometry.addProperty("type", "Polygon");
//        geometry.add("coordinates", coordinates);
//        top.add("geometry", geometry);
//
//        String geoJson = new Gson().toJson(top);
//
//        Feature polygon = Feature.fromJson(geoJson);
//        FeatureCollection collection = FeatureCollection.fromFeatures(new Feature[]{polygon});
//
//        Source geoJsonSource = new GeoJsonSource("poly-source", collection);
//        mapboxMap.addSource(geoJsonSource);
//
//        FillLayer fillLayer = new FillLayer("layerId", "poly-source");
//        fillLayer.setProperties(
//                fillColor(DIM_OVERLAY_COLOR)
//                //fillOpacity(0.7f)
//        );
//
//        mapboxMap.addLayer(fillLayer);


        List<LatLng> hole = toLatLngs(workingArea.asClosed().getNodes());
//        Polygon polygon = mapboxMap.addPolygon(new PolygonOptions()
//                .addAll(LocaUtils.WORLD_CORNER_COORDINATES)
//                .addHole(hole)
//                .fillColor(LocaUtils.DIM_OVERLAY_COLOR));

        mapboxMap.addPolyline(new PolylineOptions()
                .addAll(hole)
                .width(5)
                .color(LocaUtils.DIM_OVERLAY_COLOR));
    }

    /**
     * @param color
     * @param context
     * @return Icon for a one-node geo-objects.
     */
    public static Icon nodeGeoObjectIcon(int color, Context context) {
        //todo color icon
        return IconFactory.getInstance(context).fromResource(R.drawable.mapbox_marker_icon_default);
    }

    /**
     * @param rank
     * @param context
     * @return Color based on rank and max-rank of object in session-exercise.
     */
    public static int rankBasedColor(double rank, Context context) {
        double maxRank = SessionControl.loadExercise(context).getMaxRankOfGeoObject();
        double ratio = rank / maxRank;

        int start = 60;
        int red = Math.round((float)(start + ratio * (255-60)));

        int color = Color.argb(255, red, 0, 0);
        return color;
    }

    /**
     * @return A random color, full alpha.
     */
    public static int randomColor() {
        return Color.argb(255, randi(256), randi(256), randi(256));
    }

    /**
     * @param lowerBound
     * @param upperBound
     * @return Random integer <- [lowerBound, upperBound)
     */
    public static int randi(int lowerBound, int upperBound) {
        if (upperBound < lowerBound || lowerBound < 0 || upperBound < 1) return 0;
        return lowerBound + random.nextInt(upperBound - lowerBound);
    }

    /**
     * @param upperBound
     * @return Random integer <- [0, upperBound)
     */
    public static int randi(int upperBound) {
        if (upperBound < 1) return 0;
        return random.nextInt(upperBound);
    }

    /**
     * @param res Resource id of text-file.
     * @return Whole content of file.
     */
    public static String readTextFile(int res, Context context) {
        InputStream stream = context.getResources().openRawResource(res);
        Scanner scanner = new Scanner(stream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * @param lon,lat
     * @return Distance in meters between points.
     */
    public static double distance(double[] lon, double[] lat) {
        double lng1 = lon[0]; double lat1 = lon[1];
        double lng2 = lat[0]; double lat2 = lat[1];

        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        return dist;
    }


    //region LatLng converters

    /**
     * @param point [lon lat]
     * @return Converted to LatLng-mapbox-object
     */
    public static LatLng toLatLng(double[] point) {
        return new LatLng(point[1], point[0]);
    }

    /**
     *
     * @param points [lon lat]
     * @return Converted to LatLng-mapbox-objects
     */
    public static List<LatLng> toLatLngs(List<double[]> points) {
        List<LatLng> latLngs = new ArrayList<>();
        for (double[] point : points) latLngs.add(toLatLng(point));
        return latLngs;
    }

    /**
     * @param bounds [wsen|
     * @return Converted to LatLngBounds-mapbox-object.
     */
    public static LatLngBounds toLatLngBounds(double[] bounds) {
        return new LatLngBounds.Builder()
                .include( new LatLng(bounds[3], bounds[2]) ) // Northeast
                .include( new LatLng(bounds[1], bounds[0])) // Southwest
                .build();
    }

    //endregion

    //region Draw geo-objects on map


    /**
     * Adds geo-objects to the map (markers and polylines). Also updates id-maps.
     *
     * @param geoObjects
     * @param mapboxMap
     * @param markersMap Afterwards updated with mappings: markerId -> geoObjectId. May be null.
     * @param polylinesMap Afterwards updated with mappings: polylineId -> geoObjectId. May be null.
     */
    public static void addGeoObjects(List<GeoObject> geoObjects, MapboxMap mapboxMap,
                              Map<Long,Long> markersMap, Map<Long,Long> polylinesMap, Context context) {
        for (GeoObject geoObject : geoObjects)
            addGeoObject(geoObject, mapboxMap, markersMap, polylinesMap, context);
    }

    /**
     * Adds geo-objects to the map (markers and polylines).
     *
     * @param geoObjects
     * @param mapboxMap
     */
    public static void addGeoObjects(List<GeoObject> geoObjects, MapboxMap mapboxMap, Context context) {
        addGeoObjects(geoObjects, mapboxMap, null, null, context);
    }



    /**
     * Adds a geo-object to the map. Also updates id-maps if not null.
     * @param geoObject
     * @param mapboxMap
     * @param markersMap
     * @param polylinesMap
     * @param color
     * @param context
     * @return Added stuff.
     */
    public static List<Annotation> addGeoObject(GeoObject geoObject, MapboxMap mapboxMap, Map<Long,Long> markersMap,
                                                Map<Long,Long> polylinesMap, int color, Context context) {
        double SMALL_SIDE_LENGTH = 100; //meters

        List<Annotation> annotations = new ArrayList<>();

        for (NodeShape nodeShape : geoObject.getShapes()) {
            double[] bounds = nodeShape.getBounds();
            double[] ws = new double[]{bounds[0], bounds[1]};
            double[] es = new double[]{bounds[2], bounds[1]};
            double[] wn = new double[]{bounds[0], bounds[3]};

            if (nodeShape.getNodes().size() == 1) {
                Marker marker = addMarker(nodeShape.getNodes().get(0), geoObject.getId(), color, mapboxMap, markersMap, context);
                annotations.add(marker);
            }
            else if (distance(ws, es) < SMALL_SIDE_LENGTH &&
                    distance(ws, wn) < SMALL_SIDE_LENGTH) {
                Marker marker = addMarker(nodeShape.getCenter(), geoObject.getId(), color, mapboxMap, markersMap, context);
                annotations.add(marker);
            }
            else {
                nodeShape = nodeShape.isClosed() ? nodeShape.asExtraClosed() : nodeShape;
                Polyline polyline = addPolyline(nodeShape.getNodes(), geoObject.getId(), color, mapboxMap, polylinesMap);
                annotations.add(polyline);
            }
        }

        return annotations;
    }

    /**
     * With color based on object-rank.
     */
    public static List<Annotation> addGeoObject(GeoObject geoObject, MapboxMap mapboxMap,
                                                Map<Long,Long> markersMap, Map<Long,Long> polylinesMap, Context context) {
        int color = LocaUtils.rankBasedColor(geoObject.getRank(), context);
        return addGeoObject(geoObject, mapboxMap, markersMap, polylinesMap, color, context);
    }

    /**
     * Without lookup-maps.
     */
    public static List<Annotation> addGeoObject(GeoObject geoObject, MapboxMap mapboxMap, int color, Context context) {
        return addGeoObject(geoObject, mapboxMap, null, null, color, context);
    }
    public static List<Annotation> addGeoObject(GeoObject geoObject, MapboxMap mapboxMap, Context context) {
        return addGeoObject(geoObject, mapboxMap, null, null, context);
    }

    /**
     * Adds a marker. Updates id-map if not null.
     * @param node
     * @param geoObjectId
     * @param color
     */
    public static Marker addMarker(double[] node, long geoObjectId, int color, MapboxMap mapboxMap,
                           Map<Long,Long> markersMap, Context context) {
        Marker marker = mapboxMap.addMarker(new MarkerOptions()
                .icon(LocaUtils.nodeGeoObjectIcon(color, context))
                .position(LocaUtils.toLatLng(node)));

        if (markersMap != null) markersMap.put(marker.getId(), geoObjectId);
        return marker;
    }

    /**
     * Adds a polyline. Updates id-map if not null.
     * @param nodes
     * @param geoObjectId
     * @param color
     */
    public static Polyline addPolyline(List<double[]> nodes, long geoObjectId, int color, MapboxMap mapboxMap,
                                        Map<Long,Long> polylinesMap) {
        int GEO_OBJECT_LINE_WIDTH = 4;


        Polyline polyline = mapboxMap.addPolyline(new PolylineOptions()
                .addAll(LocaUtils.toLatLngs(nodes))
                .color(color)
                .width( GEO_OBJECT_LINE_WIDTH ));

        if (polylinesMap != null) polylinesMap.put(polyline.getId(), geoObjectId);
        return polyline;
    }

    //endregion


    //region Map-camera motion

    /**
     * Time in ms for flying (moving camera to new pos).
     */
    public static final int SHORT_FLY_TIME = 2000;
    public static final int LONG_FLY_TIME = 5000;

    /**
     * When fitting camera to shape, don't zoom more than this.
     */
    private static final double CAMERA_FITTING_MAX_ZOOM = 18;

    /**
     * Padding around working area for the initial camera-view.
     */
    private static final int CAMERA_FITTING_PADDING = 50;

    /**
     * Move camera gradually to fit shape including padding.
     * @param shape
     * @param map
     * @param flyTime
     */
    public static void flyToFitShape(NodeShape shape, MapboxMap map, int flyTime) {
//        if (shape.isNode()) {
//            flyToLocation(toLatLng(shape.getFirst()), CAMERA_FITTING_MAX_ZOOM, map);
//        }
//        else {
        flyToFitBounds(shape.getBounds(), map, flyTime);
        //}
    }

    /**
     * Move camera gradually to fit the bounds including padding.
     * @param bs
     * @param map
     * @param flyTime
     */
    public static void flyToFitBounds(double[] bs, MapboxMap map, int flyTime) {
        LatLngBounds latLngBounds = toLatLngBounds(bs);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, CAMERA_FITTING_PADDING);
        CameraPosition cameraPosition = cameraUpdate.getCameraPosition(map);

        if (cameraPosition.zoom > CAMERA_FITTING_MAX_ZOOM) {
            flyToLocation(cameraPosition.target, CAMERA_FITTING_MAX_ZOOM, map, flyTime);
        }
        else {
            map.animateCamera(cameraUpdate, flyTime);
        }
    }

    /**
     * Move camera gradually to new location.
     * @param latLng
     * @param map
     */
    public static void flyToLocation(LatLng latLng, double zoom, MapboxMap map, int flyTime) {
        CameraPosition position = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();

        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), flyTime);
    }
    public static void flyToLocation(Location location, double zoom, MapboxMap map, int flyTime) {
        LatLng latLng = toLatLng(new double[]{location.getLongitude(), location.getLatitude()});
        flyToLocation(latLng, zoom, map, flyTime);
    }

    //endregion

    //region geo-object -> annotations

    /**
     * @param geoObjectId
     * @param markersMap
     * @param polylinesMap
     * @return All annotations used to draw specified geo-object.
     */
    public static List<Annotation> geoObjectAnnotations(long geoObjectId, MapboxMap mapboxMap, Map<Long,Long> markersMap, Map<Long,Long> polylinesMap) {
        List<Long> markerIds = getKeysFromValue(geoObjectId, markersMap);
        List<Long> polylinesIds = getKeysFromValue(geoObjectId, polylinesMap);

        List<Annotation> annotations = new ArrayList<>();
        annotations.addAll(getMarkersFromIds(markerIds, mapboxMap));
        annotations.addAll(getPolylinesFromIds(polylinesIds, mapboxMap));
        return annotations;
    }

    /**
     * @param value
     * @param map
     * @return Keys with certain value.
     */
    private static List<Long> getKeysFromValue(long value, Map<Long,Long> map) {
        List<Long> keys = new ArrayList<>();
        for (Map.Entry<Long,Long> entry : map.entrySet()) {
            if (entry.getValue() == value) keys.add(entry.getKey());
        }
        return keys;
    }

    /**
     * @param ids
     * @return Markers in map with id in ids.
     */
    private static List<Marker> getMarkersFromIds(List<Long> ids, MapboxMap mapboxMap) {
        List<Marker> markers = new ArrayList<>();
        for (Marker marker : mapboxMap.getMarkers()) {
            if (ids.contains(marker.getId())) markers.add(marker);
        }
        return markers;
    }

    /**
     * @param ids
     * @return Polylines in map with id in ids.
     */
    private static List<Polyline> getPolylinesFromIds(List<Long> ids, MapboxMap mapboxMap) {
        List<Polyline> polylines = new ArrayList<>();
        for (Polyline polyline : mapboxMap.getPolylines()) {
            if (ids.contains(polyline.getId())) polylines.add(polyline);
        }
        return polylines;
    }

    //endregion

    //region blink annotations

    /**
     * Remove, then add annotations.
     * @param annotations
     * @param map
     */
    public static void blinkAnnotations(List<Annotation> annotations, MapboxMap map, Context context) {
        int BLINK_TIME = 300;

        List<Icon> markerIcons = new ArrayList<>();
        List<Integer> polylineColors = new ArrayList<>();

        for (Annotation annotation : annotations) {
            if (annotation instanceof Marker) {
                Marker marker = (Marker)annotation;
                markerIcons.add(marker.getIcon());
                marker.setIcon(IconFactory.getInstance(context)
                        .fromResource(R.drawable.transparent_icon));
                map.updateMarker(marker);
            }
            else if (annotation instanceof Polyline) {
                Polyline polyline = (Polyline)annotation;
                polylineColors.add(polyline.getColor());
                polyline.setColor(0x00000000);
                map.updatePolyline(polyline);
            }
        }

        new Handler().postDelayed(() -> {
            for (Annotation annotation : annotations) {
                if (annotation instanceof Marker) {
                    Marker marker = (Marker)annotation;
                    marker.setIcon(markerIcons.remove(0));
                    map.updateMarker(marker);
                }
                else if (annotation instanceof Polyline) {
                    Polyline polyline = (Polyline)annotation;
                    polyline.setColor(polylineColors.remove(0));
                    map.updatePolyline(polyline);
                }
            }

        }, BLINK_TIME);
    }

    //endregion

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    //region Logging

    /**
     * Log all objects in specified database.
     * @param db
     */
    public static void logGeoObjects(AppDatabase db) {
        List<Long> ids = db.geoDao().loadAllIds();
        Log.i("<ME>", "log geo objects, count: " + ids.size());

        for (long id : ids) {
            GeoObject go = db.geoDao().load(id);
            Log.i("<ME>", go.toString() + "\n");
        }

        Log.i("<ME>", "COUNT: " + db.geoDao().count());
    }


    /**
     * Log the main database.
     */
    public static void logDatabase(AppDatabase db) {
        Log.i("<DB>", "********** DB START **********");

        List<User> users = db.userDao().loadAll();
        for (User user : users) {
            Log.i("<DB>", "*USER: " + user.toString());
            logExercises(user.getId(), db);
        }

        logRunningQuiz(db);
        logSession(db);

        Log.i("<DB>", "*********** DB END ***********");
    }

    /**
     * Log exercises (and all underlying content) of a user.
     * @param userId
     * @param db
     */
    public static void logExercises(long userId, AppDatabase db) {
        List<Exercise> exercises =
                db.exerciseDao()
                        .loadWithUserOrderedByDisplayIndex(userId);

        for (Exercise exercise : exercises) {
            Log.i("<DB>", "**EXERCISE: " + exercise.toString());
            logQuizCategories(exercise.getId(), db);
        }
    }

    /**
     * Log quiz-categories (and all underlying content) of an exercise.
     * @param exerciseId
     * @param db
     */
    public static void logQuizCategories(long exerciseId, AppDatabase db) {
        List<QuizCategory> quizCategories = db.quizCategoryDao()
                .loadWithExerciseOrderedByType(exerciseId);

        for (QuizCategory quizCategory : quizCategories) {
            Log.i("<DB>", "***QUIZ-CATEGORY: " + quizCategory.toString());
            logQuizzes(quizCategory.getId(), db);
        }
    }

    /**
     * Log quizzes (and all underlying content) of a quiz-category.
     * @param quizCategoryId
     * @param db
     */
    public static void logQuizzes(long quizCategoryId, AppDatabase db) {
        List<Quiz> quizzes = db.quizDao()
                .loadWithQuizCategoryOrderedByLevel(quizCategoryId);

        for (Quiz quiz : quizzes) {
            Log.i("<DB>", "****QUIZ: " + quiz.toString());
            logGeoObjects(quiz.getId(), db);
        }
    }

    /**
     * Log geo-objects of a quiz.
     * @param quizId
     * @param db
     */
    public static void logGeoObjects(long quizId, AppDatabase db) {
        Log.i("<DB>", "*****NO GEO-OBJECTS: " + db.geoDao().countInQuiz(quizId));
//
//        List<Long> geoObjectIds = db.geoDao().loadIdsWithQuizOrderedByRank(quizId);
//
//        for (long geoObjectId : geoObjectIds) {
//            GeoObject geoObject = db.geoDao().load(geoObjectId);
//            Log.i("<DB>", "*****GEO-OBJECT: " + geoObject.toString());
//        }
    }

    /**
     * Log running-quiz, with questions.
     * @param db
     */
    public static void logRunningQuiz(AppDatabase db) {
        RunningQuiz runningQuiz = db.runningQuizDao().loadOne();
        if (runningQuiz == null) {
            Log.i("<DB>", "-No running quiz");
            return;
        }
        Log.i("<DB>", "-RUNNING-QUIZ: " + runningQuiz.toString());

        List<Question> questions = db.questionDao()
                .loadWithRunningQuizOrderedByIndex(runningQuiz.getId());
        for (Question question : questions) {
            Log.i("<DB>", "--QUESTION: " + question.toString());
        }
    }

    /**
     * Log session..
     * @param db
     */
    public static void logSession(AppDatabase db) {
        Session session = SessionControl.load(db);
        Log.i("<DB>", session.toString());
    }

    //endregion

    //region working-areas for testing


    /**
     * @return Area of interest.
     */
    public static NodeShape getWorkingArea() {
        //uppsala
//         double w = 17.558212280273438;
//         double s = 59.78301472732963;
//         double e = 17.731246948242188;
//         double n = 59.91097597079679;

        //mefjärd
//        double w = 18.460774;
//        double s = 58.958251;
//        double e = 18.619389;
//        double n = 59.080544;

        //lidingö
        // double w = 18.08246612548828;
        // double s = 59.33564087770051;
        // double e = 18.27404022216797;
        // double n = 59.39407306645033;

        //rudboda
//         double w = 18.15;
//         double s = 59.372;
//         double e = 18.19;
//         double n = 59.383;

        //new york
        // double w = -74.016259;
        // double s = 40.717569;
        // double e = -73.972399;
        // double n = 40.737473;


//        return new NodeShape(Arrays.asList(
//                new double[]{w, s},
//                new double[]{w, n},
//                new double[]{e, n},
//                new double[]{e, s}));

        //stockholm1
//        return new NodeShape(Arrays.asList(
//                new double[]{18.1015205, 59.3598395},
//                new double[]{18.0986881, 59.3616766},
//                new double[]{18.0858994, 59.3587896},
//                new double[]{18.0714798, 59.3566461},
//                new double[]{18.0508804, 59.3552461},
//                new double[]{18.0340576, 59.3512210},
//                new double[]{18.0258179, 59.3461451},
//                new double[]{18.0124283, 59.3463201},
//                new double[]{18.0072784, 59.3408934},
//                new double[]{18.0055618, 59.3344152},
//                new double[]{18.0055618, 59.3344152},
//                new double[]{18.0146599, 59.3302124},
//                new double[]{18.0146599, 59.3254837},
//                new double[]{18.0039310, 59.3274979},
//                new double[]{18.0016136, 59.3285925},
//                new double[]{17.9978371, 59.3260092},
//                new double[]{18.0001545, 59.3225060},
//                new double[]{18.0072784, 59.3212799},
//                new double[]{18.0135441, 59.3241701},
//                new double[]{18.0195522, 59.3246080},
//                new double[]{18.0253887, 59.3246517},
//                new double[]{18.0268478, 59.3273665},
//                new double[]{18.0419540, 59.3266222},
//                new double[]{18.0440140, 59.3324890},
//                new double[]{18.0483055, 59.3382672},
//                new double[]{18.0567169, 59.3396679},
//                new double[]{18.0589485, 59.3362537},
//                new double[]{18.0610943, 59.3335396},
//                new double[]{18.0626392, 59.3315258},
//                new double[]{18.0594635, 59.3266660},
//                new double[]{18.0586910, 59.3241263},
//                new double[]{18.0642700, 59.3220243},
//                new double[]{18.0792904, 59.3214550},
//                new double[]{18.0780029, 59.3266660},
//                new double[]{18.0826378, 59.3304313},
//                new double[]{18.0951691, 59.3314383},
//                new double[]{18.1066704, 59.3311756},
//                new double[]{18.1099319, 59.3210171},
//                new double[]{18.1195450, 59.3187398},
//                new double[]{18.1495857, 59.3197909},
//                new double[]{18.1643486, 59.3265346},
//                new double[]{18.1631470, 59.3365164},
//                new double[]{18.1458092, 59.3626826}));

        //stockholm2
//        return new NodeShape(Arrays.asList(
//                new double[]{18.1181717, 59.3654819},
//                new double[]{17.9921722, 59.3468453},
//                new double[]{18.0210114, 59.3065623},
//                new double[]{18.1377411, 59.3307378},
//                new double[]{18.1555939, 59.3561211}));

        //uppsala
//        return new NodeShape(Arrays.asList(
//                new double[]{17.5767517, 59.9577605},
//                new double[]{17.5286865, 59.8806685},
//                new double[]{17.5836182, 59.7937257},
//                new double[]{17.6509094, 59.7764482},
//                new double[]{17.7580261, 59.8033973},
//                new double[]{17.7456665, 59.8882479}));

        //mefjärd
//        return new NodeShape(Arrays.asList(
//                new double[]{-341.5065765+360, 59.0837983},
//                new double[]{-341.5491486+360, 59.0414377},
//                new double[]{-341.5567017+360, 59.0094562},
//                new double[]{-341.5148163+360, 59.0076883},
//                new double[]{-341.5010834+360, 59.061742},
//                new double[]{-341.4571381+360, 59.0807995},
//                new double[]{-341.4835739+360, 59.0915587}));

        //sandböte (4 osm-objects)
        return new NodeShape(Arrays.asList(
                new double[]{18.4603786,59.056049},
                new double[]{18.455658,59.0427181},
                new double[]{18.4662151,59.0455437},
                new double[]{18.4645844,59.0563581}));

        //nowhere (nothing here)
//        return new NodeShape(Arrays.asList(
//                new double[]{-158.8780975, 42.3463653},
//                new double[]{-158.8842773, 42.3417978},
//                new double[]{-158.875351, 42.3402752}));
    }

    //endregion
}
