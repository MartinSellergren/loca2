package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Data model of a quiz.
 * The execution of a quiz is randomized from Quiz-objects and specified by RunningQuiz.
 */
@Entity
public class Quiz {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Category of this quiz.
     */
    private long quizCategoryId;

    /**
     * Level. Low has motorways, high has paths.
     * @Unique with quizCategoryId.
     */
    private int level;

    /**
     * True if quiz is completed with sufficient result.
     */
    private boolean isPassed = false;


    /**
     * @param quizCategoryId
     * @param level
     */
    public Quiz(long quizCategoryId, int level) {
        this.quizCategoryId = quizCategoryId;
        this.level = level;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getQuizCategoryId() {
        return quizCategoryId;
    }

    public void setQuizCategoryId(long quizCategoryId) {
        this.quizCategoryId = quizCategoryId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public String toString() {
        return String.format("id:%s, categoryId:%s, level:%s, passed:%s",
                this.id, this.quizCategoryId, this.level, this.isPassed);
    }
}
