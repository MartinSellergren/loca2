package com.localore.localore.modelManipulation;

import android.content.Context;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.ExerciseDao;
import com.localore.localore.model.Session;
import com.localore.localore.model.SessionDao;
import com.localore.localore.model.User;

/**
 * Static class for session-related operations (manipulate the database).
 */
public class SessionControl {

    //region setters getters

    /**
     * Return session. If none exists, create one.
     * @param db
     * @return Session in db.
     */
    public static Session load(AppDatabase db) {
        Session session = db.sessionDao().load();

        if (session == null) {
            session = new Session();
            db.sessionDao().insert(session);
        }

        return session;
    }


    /**
     * @param db
     * @return Active exercise, or NULL.
     */
    public static Exercise loadExercise(AppDatabase db) {
        long exerciseId = load(db).getExerciseId();
        return db.exerciseDao().load(exerciseId);
    }

    //endregion

    /**
     * Call when user logs in. Sets session's userId. Replaces already logged in user.
     * @param userId
     * @param db
     */
    public static void login(long userId, AppDatabase db) {
        Session session = load(db);
        session.setUserId(userId);
        db.sessionDao().update(session);
    }

    /**
     * Call when user logs out. Sets session userId to -1.
     * @param db
     */
    public static void logout(AppDatabase db) {
        Session session = load(db);
        session.setUserId(-1);
        session.setExerciseId(-1);
        db.sessionDao().update(session);
    }

    /**
     * Call when user enters Exercise-view. Sets session's exerciseId.
     * @param exerciseId
     * @param db
     */
    public static void enterExercise(long exerciseId, AppDatabase db) {
        Session session = load(db);
        session.setExerciseId(exerciseId);
        db.sessionDao().update(session);
    }

    /**
     * Call when user leaves exercise, i.e goes to select-exercise-screen.
     * @param db
     */
    public static void leaveExercise(AppDatabase db) {
        Session session = load(db);
        session.setExerciseId(-1);
        db.sessionDao().update(session);
    }
}
