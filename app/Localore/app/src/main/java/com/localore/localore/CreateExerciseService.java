package com.localore.localore;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.model.NodeShape;
import com.localore.localore.modelManipulation.SessionControl;


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
    private static final String EXERCISE_NAME_PARAM_KEY = "com.localore.localore.CreateExerciseService.EXERCISE_NAME_PARAM_KEY";
    private static final String WORKING_AREA_PARAM_KEY = "com.localore.localore.CreateExerciseService.WORKING_AREA_PARAM_KEY";

    public static final String BROADCAST_ACTION = "com.localore.localore.CreateExerciseService.BROADCAST_ACTION";
    public static final String REPORT_KEY = "com.localore.localore.CreateExerciseService.REPORT_KEY";

    public CreateExerciseService() {
        super("CreateExerciseService");
    }

    /**
     * Starts the service and passes parameters in intent.
     *
     * @param exerciseName
     * @param workingArea
     * @param context
     */
    public static void start(String exerciseName, NodeShape workingArea, Context context) {
        Intent intent = new Intent(context, CreateExerciseService.class);
        intent.putExtra(EXERCISE_NAME_PARAM_KEY, exerciseName);
        intent.putExtra(WORKING_AREA_PARAM_KEY, workingArea);
        context.startService(intent);
    }

    /**
     * Request foreground.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // request foreground
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "default_channel_id")
                .setSmallIcon(R.drawable.loca_notification_icon)
                .setContentTitle("Creating new exercise...")
                //.setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();

        int NOTIFICATION_ID = 1;
        startForeground(NOTIFICATION_ID, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    // required for Android 8.0 and higher
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "whatever";
            String description = "whatever";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("default_channel_id", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Fetch and process.
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("_ME_", "CreateExerciseService started");
        if (intent == null) return;

        String exerciseName = intent.getStringExtra(EXERCISE_NAME_PARAM_KEY);
        NodeShape workingArea = (NodeShape)intent.getSerializableExtra(WORKING_AREA_PARAM_KEY);
        long userId = SessionControl.load(this).getUserId();
        long exerciseId = ExerciseControl.newExercise(userId, exerciseName, workingArea, this);

        boolean ok = ExerciseControl.acquireGeoObjects(workingArea, this);
        if (!ok) {
            report("Failed");
            return;
        }

        Log.d("_ME_", "Post processing");
        ExerciseControl.postProcessing(exerciseId, this);

        report("Done!");
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
