package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.graphics.drawable.Icon;

import com.localore.localore.R;

/**
 * A quiz-category of a certain exercise.
 */
@Entity
public class QuizCategory {

    /**
     * The different types of quiz-categories.
     * Index defines display-order in exercise-view.
     * String match with strings in json-conversion-table.
     */
    public static final String[] TYPES = new String[]{
            "settlements",
            "roads",
            "nature",
            "transport",
            "constructions"};

    /**
     * Corresponding names but formatted for display.
     */
    public static final int[] DISPLAY_TYPES = new int[]{
            R.string.Settlements,
            R.string.Roads,
            R.string.Nature,
            R.string.Transport,
            R.string.Constructions};

    public static final int SETTLEMENTS = 0;
    public static final int ROADS = 1;
    public static final int NATURE = 2;
    public static final int TRANSPORT = 3;
    public static final int CONSTRUCTIONS = 4;


    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Exercise of this quiz-category.
     */
    private long exerciseId;

    /**
     * Quiz-category type.
     */
    private int type;

    /**
     * Number of quiz-category reminder-quizzes to complete.
     */
    private int noRequiredReminders = 0;

    /**
     * @param exerciseId
     * @param type
     */
    public QuizCategory(long exerciseId, int type) {
        this.exerciseId = exerciseId;
        this.type = type;
    }

    /**
     * @param type
     * @return Icon representing category.
     */
    public static int getIconResource(int type) {
        switch(type) {
            case SETTLEMENTS: return R.drawable.settlements;
            case ROADS: return R.drawable.roads;
            case NATURE: return R.drawable.nature;
            case TRANSPORT: return R.drawable.transport;
            case CONSTRUCTIONS: return R.drawable.constructions;
            default: throw new RuntimeException("Dead end");
        }
    }

    /**
     * @return Type (name) of this category, formatted nicely.
     */
    public String getDisplayType(Context context) {
        return context.getResources().getString(DISPLAY_TYPES[ getType() ]);
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

    public int getNoRequiredReminders() {
        return noRequiredReminders;
    }

    public void setNoRequiredReminders(int noRequiredReminders) {
        this.noRequiredReminders = noRequiredReminders;
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
