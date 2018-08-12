package com.localore.localore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;
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
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

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

    private static Random random = new Random();

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

    /**
     * Fade out old activity, start new new and fade it in.
     * @param newActivityClass
     * @param oldActivity
     */
    public static void fadeInActivity(Class<?> newActivityClass, Activity oldActivity) {
        Intent intent = new Intent(oldActivity, newActivityClass);
        oldActivity.startActivity(intent);
        oldActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
     * @return Color based on rank..
     */
    public static int rankBasedColor(double rank) {
        //todo: color based on rank
        int color = Color.argb(255, randi(256), randi(256), randi(256));
        return color;
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
     * @param markersMap Updated with mappings: markerId -> geoObjectId. May be null.
     * @param polylinesMap Updated with mappings: polylineId -> geoObjectId. May be null.
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
     * @param context
     */
    public static void addGeoObject(GeoObject geoObject, MapboxMap mapboxMap,
                              Map<Long,Long> markersMap, Map<Long,Long> polylinesMap, Context context) {
        int color = LocaUtils.rankBasedColor(geoObject.getRank());

        for (NodeShape nodeShape : geoObject.getShapes()) {
            if (nodeShape.getNodes().size() == 1) {
                addMarker(nodeShape.getNodes().get(0), geoObject.getId(), color, mapboxMap, markersMap, context);
            }
            else {
                addPolyline(nodeShape.getNodes(), geoObject.getId(), color, mapboxMap, polylinesMap);
            }
        }
    }

    /**
     * Adds a geo-object to the map.
     * @param geoObject
     * @param mapboxMap
     * @param context
     */
    public static void addGeoObject(GeoObject geoObject, MapboxMap mapboxMap, Context context) {
        addGeoObject(geoObject, mapboxMap, null, null, context);
    }


    /**
     * Adds a marker. Updates id-map if not null.
     * @param node
     * @param geoObjectId
     * @param color
     */
    private static void addMarker(double[] node, long geoObjectId, int color, MapboxMap mapboxMap,
                           Map<Long,Long> markersMap, Context context) {
        Marker marker = mapboxMap.addMarker(new MarkerOptions()
                .icon(LocaUtils.nodeGeoObjectIcon(color, context))
                .position(LocaUtils.toLatLng(node)));

        if (markersMap != null) markersMap.put(marker.getId(), geoObjectId);
    }

    /**
     * Adds a polyline. Updates id-map if not null.
     * @param nodes
     * @param geoObjectId
     * @param color
     */
    private static void addPolyline(List<double[]> nodes, long geoObjectId, int color, MapboxMap mapboxMap,
                             Map<Long,Long> polylinesMap) {
        Polyline polyline = mapboxMap.addPolyline(new PolylineOptions()
                .addAll(LocaUtils.toLatLngs(nodes))
                .color(color)
                .width(2));

        if (polylinesMap != null) polylinesMap.put(polyline.getId(), geoObjectId);
    }

    //endregion

    //region Map-camera motion

    /**
     * Time in ms for flying (moving camera to new pos).
     */
    public static final int FLY_TIME = 2500;

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
     */
    public static void flyToFitShape(NodeShape shape, MapboxMap map) {
//        if (shape.isNode()) {
//            flyToLocation(toLatLng(shape.getFirst()), CAMERA_FITTING_MAX_ZOOM, map);
//        }
//        else {
        flyToFitBounds(shape.getBounds(), map);
        //}
    }

    /**
     * Move camera gradually to fit the bounds including padding.
     * @param bs
     * @param map
     */
    public static void flyToFitBounds(double[] bs, MapboxMap map) {
        LatLngBounds latLngBounds = toLatLngBounds(bs);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, CAMERA_FITTING_PADDING);
        CameraPosition cameraPosition = cameraUpdate.getCameraPosition(map);

        if (cameraPosition.zoom > CAMERA_FITTING_MAX_ZOOM) {
            flyToLocation(cameraPosition.target, CAMERA_FITTING_MAX_ZOOM, map);
        }
        else {
            map.animateCamera(cameraUpdate, FLY_TIME);
        }
    }

    /**
     * Move camera gradually to new location.
     * @param latLng
     * @param map
     */
    public static void flyToLocation(LatLng latLng, double zoom, MapboxMap map) {
        CameraPosition position = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();

        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), FLY_TIME);
    }
    public static void flyToLocation(Location location, double zoom, MapboxMap map) {
        LatLng latLng = toLatLng(new double[]{location.getLongitude(), location.getLatitude()});
        flyToLocation(latLng, zoom, map);
    }

    //endregion

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
