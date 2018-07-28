package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

/**
 * Class representing an exercise.
 */
@Entity
public class Exercise {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Determines the order of exercises in the select-exercise screen.
     * @Unique
     */
    private int display_index;

    /**
     * @Unique
     */
    private String name;

    private NodeShape workingArea;

    /**
     * Number of cleared levels since a global-reminder-quiz was taken.
     */
    private int levelsSinceGlobalReminder = 0;

    /**
     * Number of global reminder-quizzes to complete.
     */
    private int requiredGlobalReminders = 0;

    /**
     * Color theme of every view of this exercise.
     */
    //private ColorTheme colorTheme;


    /**
     * Default constructor. Display-index = 0 (top of list).
     * @param name
     * @param workingArea
     */
    public Exercise(String name, NodeShape workingArea) {
        this.name = name;
        this.workingArea = workingArea;

        this.display_index = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getDisplay_index() {
        return display_index;
    }

    public void setDisplay_index(int display_index) {
        this.display_index = display_index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeShape getWorkingArea() {
        return workingArea;
    }

    public void setWorkingArea(NodeShape workingArea) {
        this.workingArea = workingArea;
    }

    public int getLevelsSinceGlobalReminder() {
        return levelsSinceGlobalReminder;
    }

    public void setLevelsSinceGlobalReminder(int levelsSinceGlobalReminder) {
        this.levelsSinceGlobalReminder = levelsSinceGlobalReminder;
    }

    public int getRequiredGlobalReminders() {
        return requiredGlobalReminders;
    }

    public void setRequiredGlobalReminders(int requiredGlobalReminders) {
        this.requiredGlobalReminders = requiredGlobalReminders;
    }

    @Override
    public String toString(){
        return "Exercise: " + this.name + ", index: " + this.display_index;
    }
}