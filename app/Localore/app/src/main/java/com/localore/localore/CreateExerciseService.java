package com.localore.localore;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class CreateExerciseService extends IntentService {
    private static final String AREA_PARAM = "com.localore.localore.extra.AREA_PARAM";
    public static final String BROADCAST_ACTION = "com.localore.localore.broadcast.CreateExerciseService";

    public CreateExerciseService() {
        super("CreateExerciseService");
    }

    public static void start(Context context, NodeShape workingArea) {
        Intent intent = new Intent(context, CreateExerciseService.class);
        intent.putExtra(AREA_PARAM, workingArea);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // request foreground

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            NodeShape workingArea = (NodeShape)intent.getSerializableExtra(AREA_PARAM);
            Log.i("_ME_", "Create-exercise started");
            Log.i("_ME_", "Area: " + workingArea.toString());

            AppDatabase db = AppDatabase.getInstance(this);
            db.geoDao().insert(new GeoObject("0", "lidingö", null, 0, "bla", "bla"));
            db.geoDao().insert(new GeoObject("1", "mefjärd", null, 0, "bla", "bla"));
        }

        reportDone();
    }

    private void reportDone() {
        Intent localIntent = new Intent(BROADCAST_ACTION);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}
