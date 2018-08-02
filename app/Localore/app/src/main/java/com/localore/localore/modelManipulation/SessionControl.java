package com.localore.localore.modelManipulation;

import android.content.Context;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.Session;
import com.localore.localore.model.User;

/**
 * Static class for session-related operations (manipulate the database).
 */
public class SessionControl {

    /**
     * Return session. If none exists, create one.
     * @return Session in db.
     */
    public static Session load(Context context) {
        Session session = AppDatabase.getInstance(context).sessionDao().load();

        if (session == null) {
            session = new Session();
            AppDatabase.getInstance(context).sessionDao().insert(session);
        }

        return session;
    }

    /**
     * @param context
     * @return Active exercise, or NULL.
     */
    public static Exercise loadExercise(Context context) {
        long exerciseId = load(context).getExerciseId();
        return AppDatabase.getInstance(context).exerciseDao().load(exerciseId);
    }

    /**
     * Call when user logs in. Sets session's userId. Replaces already logged in user.
     * @param userId
     * @param context
     */
    public static void login(long userId, Context context) {
        Session session = load(context);
        session.setUserId(userId);
        session.setExerciseId(-1);
        AppDatabase.getInstance(context).sessionDao().update(session);
    }

    /**
     * Call when user logs out. Sets session userId to -1.
     * @param context
     */
    public static void logout(Context context) {
        Session session = load(context);
        session.setUserId(-1);
        session.setExerciseId(-1);
        AppDatabase.getInstance(context).sessionDao().update(session);
    }

    /**
     * Call when user enters Exercise-view. Sets session's exerciseId.
     * @param exerciseId
     * @param context
     */
    public static void enterExercise(long exerciseId, Context context) {
        Session session = load(context);
        session.setExerciseId(exerciseId);
        AppDatabase.getInstance(context).sessionDao().update(session);
    }

    /**
     * Call when user leaves exercise, i.e goes to select-exercise-screen.
     * @param context
     */
    public static void leaveExercise(Context context) {
        Session session = load(context);
        session.setExerciseId(-1);
        AppDatabase.getInstance(context).sessionDao().update(session);
    }
}
