package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.VisibleForTesting;

import com.localore.localore.LocaUtils;
import com.localore.localore.modelManipulation.RunningQuizControl;

import java.util.ArrayList;
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
     * True if question has been answered in quiz-run.
     * True even if sub-answer given (pair-it).
     */
    private boolean answered = false;

    /**
     * Question-type.
     */
    private int type;

    /**
     * - For name-it: even number of alternatives (including correct one).
     * - For place-it: >2 alternatives (including correct one).
     * - For pair-it: even number of geo-objects to pair.
     */
    private List<GeoObject> content;

    public Question() {}

//    /**
//     * @param runningQuizId
//     * @param geoObjectId
//     * @param index
//     * @param questionType
//     * @param content
//     */
//    public Question(long runningQuizId, long geoObjectId, int index, int questionType, List<GeoObject> content) {
//        this.runningQuizId = runningQuizId;
//        this.geoObjectId = geoObjectId;
//        this.index = index;
//        this.type = questionType;
//        this.content = content;
//    }

    /**
     * Randomize type and generate content based on difficulty.
     * Loads geo-objects from db for question-content (answer-alternatives etc).
     *
     * @param runningQuizId
     * @param geoObject
     * @param index
     * @param difficulty Determines level difficulty (no. answer alternatives..).
     *                   0 <= this <= DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT (+NO_EXTRA_QUESTIONS)
     * @param db
     */
    public Question(long runningQuizId, GeoObject geoObject, int index, int difficulty, AppDatabase db) {
        this.runningQuizId = runningQuizId;
        this.geoObjectId = geoObject.getId();
        this.index = index;
        this.type = 0;//LocaUtils.randi(3);

        if (difficulty > RunningQuizControl.DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT)
            difficulty = RunningQuizControl.DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT;
        this.content = generateContent(geoObject, type, difficulty, db);
    }

    /**
     * Generates question-content about geo-object based on question-type, by consulting db.
     *
     * @param geoObject
     * @param questionType
     * @param difficulty <- [0, DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT]
     * @param db
     * @return Content of question.
     */
    @VisibleForTesting
    private List<GeoObject> generateContent(GeoObject geoObject, int questionType, int difficulty, AppDatabase db) {
        switch (questionType) {
            case 0: return generateContent_nameIt(geoObject, difficulty, db);
            case 1: return generateContent_placeIt(geoObject, difficulty, db);
            case 2: return generateContent_PairIt(geoObject, difficulty, db);
            default: throw new RuntimeException("Dead-end");
        }
    }

    /**
     * @param geoObject
     * @param difficulty
     * @return Name-it content.
     */
    private List<GeoObject> generateContent_nameIt(GeoObject geoObject, int difficulty, AppDatabase db) {
        int minNoAlternativePairs = 1;
        int maxNoAlternativePairs = 3;
        int noPairs = minNoAlternativePairs +
                scaleBasedOnDifficulty(maxNoAlternativePairs - minNoAlternativePairs, difficulty);
        int noAlternatives = noPairs * 2;

        //todo: smart alternatives

        List<GeoObject> content = loadRandomGeoObjectsIncluding(geoObject, noAlternatives, db);
        if (content.size() % 2 != 0) removeAnyExcept(geoObject, content);

        if (content.size() < minNoAlternativePairs * 2)
            throw new RuntimeException("Not enough geo-objects in db");

        return content;
    }

    /**
     * @param geoObject
     * @param difficulty
     * @return Place-it content.
     */
    private List<GeoObject> generateContent_placeIt(GeoObject geoObject, int difficulty, AppDatabase db) {
        int minNoAlternatives = 2;
        int maxNoAlternatives = 6;
        int noAlternatives = minNoAlternatives +
                scaleBasedOnDifficulty(maxNoAlternatives - minNoAlternatives, difficulty);

        //todo: smart alternatives

        List<GeoObject> content = loadRandomGeoObjectsIncluding(geoObject, noAlternatives, db);

        if (content.size() < minNoAlternatives)
            throw new RuntimeException("Not enough geo-objects in db");

        return content;
    }

    /**
     * @param geoObject
     * @param difficulty
     * @return Pair-it content.
     */
    private List<GeoObject> generateContent_PairIt(GeoObject geoObject, int difficulty, AppDatabase db) {
        int minNoAlternativePairs = 1;
        int maxNoAlternativePairs = 3;
        int noPairs = minNoAlternativePairs +
                scaleBasedOnDifficulty(maxNoAlternativePairs - minNoAlternativePairs, difficulty);
        int noAlternatives = noPairs * 2;

        //todo: smart alternatives

        List<GeoObject> content = loadRandomGeoObjectsIncluding(geoObject, noAlternatives, db);
        if (content.size() % 2 != 0) removeAnyExcept(geoObject, content);

        if (content.size() < minNoAlternativePairs * 2)
            throw new RuntimeException("Not enough geo-objects in db");

        return content;
    }

    /**
     *
     * @param value
     * @param difficulty <- [0, DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT]
     * @return Value scaled based on difficulty <- [0, value].
     */
    @VisibleForTesting
    private int scaleBasedOnDifficulty(double value, int difficulty) {
        double maxDifficulty = RunningQuizControl.DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT;
        double scaledValue = (difficulty / maxDifficulty) * value;
        return (int)Math.round(scaledValue);
    }

    /**
     * @param includeGeoObject
     * @param preferredCount
     * @param db
     * @return List of geo-objects of length preferredCount or less (if not enough geo-objects in db).
     *         List includes specified geo-object.
     */
    private List<GeoObject> loadRandomGeoObjectsIncluding(GeoObject includeGeoObject, int preferredCount, AppDatabase db) {
        List<GeoObject> geoObjects = db.geoDao().loadRandoms(preferredCount);
        if (geoObjects.size() == 0) return new ArrayList<>();

        if (!geoObjects.contains(includeGeoObject))
            geoObjects.set(LocaUtils.randi(geoObjects.size()), includeGeoObject);

        return geoObjects;
    }

    /**
     * Remove any geo-object from list, but not specified geoObject.
     * @param geoObject
     * @param geoObjects
     */
    private void removeAnyExcept(GeoObject geoObject, List<GeoObject> geoObjects) {
        for (GeoObject go : geoObjects) {
            if (!go.equals(geoObject)) {
                geoObjects.remove(go);
                return;
            }
        }
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

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
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

