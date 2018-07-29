package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.List;

/**
 * A question about a geo-object. Lives inside a quiz-run.
 */
@Entity
public class Question {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Running-quiz of this question.
     */
    private long runningQuizId;

    /**
     * Qeo-object defining question.
     */
    private long geoObjectId;

    /**
     * Index of this question.
     * @Unique with RunningQuizId
     */
    private int index;

    /**
     * True if question has been answered correctly in quiz-run.
     */
    private boolean answeredCorrectly = false;

    /**
     * name-it/ place-it/ pair-it"
     */
    private String type;

    /**
     * - For name-it: even number of alternatives (including correct one)
     * - For place-it: >2 alternatives (including correct one)
     * - For pair-it: even number of geo-objects to pair.
     */
    private List<GeoObject> content;


    public Question(long runningQuizId, long geoObjectId, int index, String type, List<GeoObject> content) {
        this.runningQuizId = runningQuizId;
        this.geoObjectId = geoObjectId;
        this.index = index;
        this.type = type;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRunningQuizId() {
        return runningQuizId;
    }

    public void setRunningQuizId(long runningQuizId) {
        this.runningQuizId = runningQuizId;
    }

    public long getGeoObjectId() {
        return geoObjectId;
    }

    public void setGeoObjectId(long geoObjectId) {
        this.geoObjectId = geoObjectId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isAnsweredCorrectly() {
        return answeredCorrectly;
    }

    public void setAnsweredCorrectly(boolean answeredCorrectly) {
        this.answeredCorrectly = answeredCorrectly;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<GeoObject> getContent() {
        return content;
    }

    public void setContent(List<GeoObject> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return String.format("id: %s, runningQuizId: %s, geoObjectId: %s, index: %s, answeredCorrectly: %s, type: %s",
                id, runningQuizId, geoObjectId, index, answeredCorrectly, type);
    }
}

