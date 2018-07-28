package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Currently running quiz.
 * @inv Max one of this object exists in database.
 */
@Entity
public class QuizRun {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Index of currently asked question.
     */
    private int currentQuestionIndex = 0;

    /**
     * level / followup / reminder
     */
    private String type;


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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("QuizRun, id:%s, currentQuestion:%s",
                this.id, this.currentQuestionIndex);
    }
}