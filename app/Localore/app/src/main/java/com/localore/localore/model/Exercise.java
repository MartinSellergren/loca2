package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Class representing an exercise.
 */
@Entity
public class Exercise {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * User owning this exercise.
     */
    private long userId;

    /**
     * Determines the order of exercises in the select-exercise screen.
     * @Unique
     */
    private int displayIndex = 0;

    /**
     * @Unique
     */
    private String name;

    /**
     * Area to study.
     */
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
    public Exercise(long userId, String name, NodeShape workingArea) {
        this.userId = userId;
        this.name = name;
        this.workingArea = workingArea;
    }

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

    public int getDisplayIndex() {
        return displayIndex;
    }

    public void setDisplayIndex(int displayIndex) {
        this.displayIndex = displayIndex;
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
        return "Exercise: " + this.name + ", index: " + this.displayIndex;
    }
}