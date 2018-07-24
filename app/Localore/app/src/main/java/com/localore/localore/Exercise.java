package com.localore.localore;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * Class representing an exercise.
 * No geo-objects, instead each geo-object specifies its exercise.
 */
@Entity
public class Exercise {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private NodeShape workingArea;

    public Exercise() {}
    public Exercise(String name, NodeShape wa) {
        this.name = name;
        this.workingArea = wa;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    @Override
    public String toString(){
        return "Exercise: " + this.name + ", id: " + this.id;
    }
}

class ExerciseConverter {
    @TypeConverter
    public static NodeShape fromString(String json) {
        Type listType = new TypeToken<NodeShape>(){}.getType();
        return new Gson().fromJson(json, listType);
    }

    @TypeConverter
    public static String fromArrayList(NodeShape nodeShape) {
        Gson gson = new Gson();
        String json = gson.toJson(nodeShape);
        return json;
    }
}