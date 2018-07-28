package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.graphics.drawable.Icon;

/**
 * A quiz-category of a certain exercise.
 */
@Entity
public class QuizCategory {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Exercise of this quiz-category.
     */
    private long exerciseId;

    /**
     * 0=Settlements
     * 1=Roads
     * 2=Nature
     * 3=Transport
     * 4=Constructions
     */
    private int type;

    /**
     * Number of category reminder-quizzes to complete.
     */
    private int requiredCategoryReminders = 0;

    /**
     * @param exerciseId
     * @param type
     */
    public QuizCategory(long exerciseId, int type) {
        this.exerciseId = exerciseId;
        this.type = type;
    }

    /**
     * @return Icon representing category.
     */
    public Icon getIcon() {
        switch(this.type) {
            case 0: return null;
            case 1: return null;
            case 2: return null;
            case 3: return null;
            case 4: return null;
            default: throw new RuntimeException("Deda-end");
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getRequiredCategoryReminders() {
        return requiredCategoryReminders;
    }

    public void setRequiredCategoryReminders(int requiredCategoryReminders) {
        this.requiredCategoryReminders = requiredCategoryReminders;
    }

    @Override
    public String toString() {
        switch(this.type) {
            case 0: return "Settlements";
            case 1: return "Roads";
            case 2: return "Nature";
            case 3: return "Transport";
            case 4: return "Constructions";
            default: throw new RuntimeException("Deda-end");
        }
    }
}
