package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.localore.localore.LocaUtils;

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
     * Number of passed levels since a global-reminder-quiz was taken.
     */
    private int noPassedLevelsSinceExerciseReminder = 0;

    /**
     * Number of global reminder-quizzes to complete.
     */
    private int noRequiredExerciseReminders = 0;

    /**
     * Rank of geo-object with highest rank.
     */
    private double maxRankOfGeoObject = 0;

    /**
     * Color "theme" of every view of this exercise.
     */
    private int color;


    /**
     * Default constructor. Display-index = 0 (top of list).
     * @param name
     * @param workingArea
     */
    public Exercise(long userId, String name, NodeShape workingArea) {
        this.userId = userId;
        this.name = name;
        this.workingArea = workingArea;
        this.color = LocaUtils.randomColor();
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

    public int getNoPassedLevelsSinceExerciseReminder() {
        return noPassedLevelsSinceExerciseReminder;
    }

    public double getMaxRankOfGeoObject() {
        return maxRankOfGeoObject;
    }

    public void setMaxRankOfGeoObject(double maxRankOfGeoObject) {
        this.maxRankOfGeoObject = maxRankOfGeoObject;
    }

    public void setNoPassedLevelsSinceExerciseReminder(int noPassedLevelsSinceExerciseReminder) {
        this.noPassedLevelsSinceExerciseReminder = noPassedLevelsSinceExerciseReminder;
    }

    public int getNoRequiredExerciseReminders() {
        return noRequiredExerciseReminders;
    }

    public void setNoRequiredExerciseReminders(int noRequiredExerciseReminders) {
        this.noRequiredExerciseReminders = noRequiredExerciseReminders;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public String toString(){
        return "Exercise: " + this.name + ", index: " + this.displayIndex;
    }
}