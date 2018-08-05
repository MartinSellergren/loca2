package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.localore.localore.LoadingNewExerciseActivity;

/**
 * Entity for current session-state.
 */
@Entity
public class Session {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Logged in user. -1 if none.
     */
    private long userId = -1;

    /**
     * Currently active exercise. -1 in none.
     */
    private long exerciseId = -1;


    //region loading-exercise fields

    /**
     * Status of exercise-loading.
     */
    private int loadingExerciseStatus = LoadingNewExerciseActivity.NOT_STARTED;

    /**
     * Name of currently loading exercise.
     */
    private String loadingExerciseName;

    /**
     * Working-area of currently loading exercise.
     */
    private NodeShape loadingExerciseWorkingArea;
    //endregion


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getLoadingExerciseStatus() {
        return loadingExerciseStatus;
    }

    public void setLoadingExerciseStatus(int loadingExerciseStatus) {
        this.loadingExerciseStatus = loadingExerciseStatus;
    }

    public String getLoadingExerciseName() {
        return loadingExerciseName;
    }

    public void setLoadingExerciseName(String loadingExerciseName) {
        this.loadingExerciseName = loadingExerciseName;
    }

    public NodeShape getLoadingExerciseWorkingArea() {
        return loadingExerciseWorkingArea;
    }

    public void setLoadingExerciseWorkingArea(NodeShape loadingExerciseWorkingArea) {
        this.loadingExerciseWorkingArea = loadingExerciseWorkingArea;
    }

    public String toString() {
        return String.format("Session-user: %s, Session-exercise: %s", userId, exerciseId);
    }
}
