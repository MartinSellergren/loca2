package com.localore.localore.modelManipulation;

import android.content.Context;

import com.localore.localore.LoadingNewExerciseActivity;
import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.ExerciseDao;
import com.localore.localore.model.NodeShape;
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
            long id = db.sessionDao().insert(session);
            session.setId(id);
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
        userId = load(db).getUserId();
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
     * Sets session's exerciseId.
     * @param exerciseId
     * @param db
     */
    public static void setActiveExercise(long exerciseId, AppDatabase db) {
        Session session = load(db);
        session.setExerciseId(exerciseId);
        db.sessionDao().update(session);

        session = load(db);
    }

    /**
     * Call when user leaves exercise, i.e goes to select-exercise-screen.
     * @param db
     */
    public static void setNoActiveExercise(AppDatabase db) {
        Session session = load(db);
        session.setExerciseId(-1);
        db.sessionDao().update(session);
    }

    //region loading-exercise-control

    /**
     * Call before starting the LoadingNewExerciseActivity.
     * @param name
     * @param workingArea
     */
    public static void initLoadingOfNewExercise(String name, NodeShape workingArea, AppDatabase db) {
        Session session = load(db);
        session.setLoadingExerciseName(name);
        session.setLoadingExerciseWorkingArea(workingArea);
        session.setLoadingExerciseStatus(LoadingNewExerciseActivity.NOT_STARTED);
        db.sessionDao().update(session);
    }

    public static void finalizeLoadingOfNewExercise(AppDatabase db) {
        updateLoadingExerciseStatus(LoadingNewExerciseActivity.NOT_STARTED, db);
    }

    public static void updateLoadingExerciseStatus(int status, AppDatabase db) {
        Session session = load(db);
        session.setLoadingExerciseStatus(status);
        db.sessionDao().update(session);
    }

    //
}
