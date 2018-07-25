package com.localore.localore;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


/**
 * Service for completing exercise-construction by updating the database.
 * Fetches OSM-geo-elements using Overpass service, processes these into GeoObjects and
 * constructs quizzes.
 *
 * In: An exercise with name and working area.
 * - Adds geo-objects of this exercise to the database.
 * - Completes exercise with predefined quizzes.
 */
public class CreateExerciseService extends IntentService {
    private static final String EXERCISE_ID_PARAM_KEY = "com.localore.localore.CreateExerciseService.EXERCISE_PARAM_KEY";
    public static final String BROADCAST_ACTION = "com.localore.localore.CreateExerciseService.BROADCAST_ACTION";
    public static final String REPORT_KEY = "com.localore.localore.CreateExerciseService.REPORT_KEY";

    public CreateExerciseService() {
        super("CreateExerciseService");
    }

    /**
     * Starts the service and passes parameter exercise-id.
     *
     * @param context
     * @param exerciseId Id of exercise (currently in AppDatabase!) to be created completely.
     */
    public static void start(Context context, long exerciseId) {
        Intent intent = new Intent(context, CreateExerciseService.class);
        intent.putExtra(EXERCISE_ID_PARAM_KEY, exerciseId);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // request foreground

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("_ME_", "CreateExerciseService started");

        if (intent == null) return;
        long exerciseId = intent.getLongExtra(EXERCISE_ID_PARAM_KEY, -1);
        AppDatabase db = AppDatabase.getInstance(this);

        Exercise exercise = db.exerciseDao().loadExercise(exerciseId);
        if (exercise == null) throw new RuntimeException("Exercise not in db");
        NodeShape workingArea = exercise.getWorkingArea();

        boolean ok = acquireGeoObjects(exerciseId, workingArea, db);
        if (!ok) {
            report("Failed");
            return;
        }

        report("Done!");
    }

    /**
     * Fetches geo-objects in the working-area. Process raw OSM. Updates database with geo-objects.
     *
     * @param exerciseId Exercise of objects.
     * @param workingArea Area containing objects.
     * @param db Database to be updated.
     * @return True if database updated as planned. False means network error (etc?).
     */
    private boolean acquireGeoObjects(long exerciseId, NodeShape workingArea, AppDatabase db) {
//        db.geoDao().insert(new GeoObject("0", "lidingö", null, 0, "bla", "bla"));
//        db.geoDao().insert(new GeoObject("1", "mefjärd", null, 0, "bla", "bla"));

        JsonObject convTable = openConversionTable();
        GeoObjInstructionsIter iter = new GeoObjInstructionsIter(workingArea, this);
        iter.open();
        List<String> instr;

        while ((instr=iter.next()) != null) {
            try {
                GeoObject go = new GeoObject(instr, convTable);
                db.geoDao().insert(go);
            }
            catch (GeoObject.BuildException e) {
                Log.i("_ME_", "Can't build: " + e.toString());
            }
        }

        return true;
    }

    /**
     * Open table for conversion from tags to category.
     */
    private JsonObject openConversionTable() {
        String json = LocaUtils.readTextFile(R.raw.tag_categories, this);
        return new JsonParser().parse(json).getAsJsonObject();
    }

    /**
     * Broadcast status-report.
     * @param report
     */
    private void report(String report) {
        Intent localIntent = new Intent(BROADCAST_ACTION);
        localIntent.putExtra(REPORT_KEY, report);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}
