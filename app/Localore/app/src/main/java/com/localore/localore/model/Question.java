package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.VisibleForTesting;

import com.localore.localore.LocaUtils;

import java.util.List;
import java.util.Random;

/**
 * A question about a geo-object. Lives inside a running-quiz.
 */
@Entity
public class Question {

    /**
     * The different types of questions.
     */
    public static final int NAME_IT = 0;
    public static final int PLACE_IT = 1;
    public static final int PAIR_IT = 2;


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
     * Question-type.
     */
    private int type;

    /**
     * - For name-it: even number of alternatives (including correct one)
     * - For place-it: >2 alternatives (including correct one)
     * - For pair-it: even number of geo-objects to pair.
     */
    private List<GeoObject> content;


    /**
     * @param runningQuizId
     * @param geoObjectId
     * @param index
     * @param questionType
     * @param content
     */
    public Question(long runningQuizId, long geoObjectId, int index, int questionType, List<GeoObject> content) {
        this.runningQuizId = runningQuizId;
        this.geoObjectId = geoObjectId;
        this.index = index;
        this.type = questionType;
        this.content = content;
    }

    /**
     * Randomize type and generate content based on difficulty.
     *
     * @param runningQuizId
     * @param geoObject
     * @param index
     * @param difficulty Determines level difficulty (no. answer alternatives..).
     *                   0 <= this <= DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT (+NO_EXTRA_QUESTIONS)
     */
    public Question(long runningQuizId, GeoObject geoObject, int index, int difficulty) {
        this.runningQuizId = runningQuizId;
        this.geoObjectId = geoObject.getId();
        this.index = index;
        this.type = new Random().nextInt(3);
        this.content = generateContent(geoObject, type, difficulty);
    }

    /**
     * Generate question-content about geo-object based on question-type.
     *
     * @param geoObject
     * @param questionType
     * @param difficulty
     * @return Content of question.
     */
    @VisibleForTesting
    private List<GeoObject> generateContent(GeoObject geoObject, int questionType, int difficulty) {
        switch (questionType) {
            case 0: return generateContent_nameIt(geoObject, difficulty);
            case 1: return generateContent_placeIt(geoObject, difficulty);
            case 2: return generateContent_PairIt(geoObject, difficulty);
            default: throw new RuntimeException("Dead-end");
        }
    }

    /**
     * @param geoObject
     * @param difficulty
     * @return Name-it content.
     */
    private List<GeoObject> generateContent_nameIt(GeoObject geoObject, int difficulty) {
        //todo
        return null;
    }

    /**
     * @param geoObject
     * @param difficulty
     * @return Place-it content.
     */
    private List<GeoObject> generateContent_placeIt(GeoObject geoObject, int difficulty) {
        //todo
        return null;
    }

    /**
     * @param geoObject
     * @param difficulty
     * @return Pair-it content.
     */
    private List<GeoObject> generateContent_PairIt(GeoObject geoObject, int difficulty) {
        //todo
        return null;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
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

