package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;

/**
 * Currently running quiz.
 * @inv Max one of this object exists in database.
 */
@Entity
public class RunningQuiz {

    /**
     * The different types of running-quizzes.
     */
    public static final int LEVEL_QUIZ = 0;
    public static final int FOLLOW_UP_QUIZ = 1;
    public static final int QUIZ_CATEGORY_REMINDER = 2;
    public static final int EXERCISE_REMINDER = 3;


    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Index of currently asked question.
     */
    private int currentQuestionIndex = -1;

    /**
     * Running-quiz type.
     */
    private int type;

    /**
     * Constructs a running-quiz with given type.
     * @param type
     */
    public RunningQuiz(int type) {
        this.type = type;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("RunningQuiz, id:%s, currentQuestion:%s, type:%s",
                this.id, this.currentQuestionIndex, this.type);
    }
}