package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

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

    public String toString() {
        return String.format("Session-user: %s, Session-exercise: %s", userId, exerciseId);
    }
}
