package com.localore.localore.model;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class AppDatabaseConverters {

//    /**
//     * Converter of List<Long>
//     */
//    @TypeConverter
//    public static List<Long> toLongList(String json) {
//        Type listType = new TypeToken<List<Long>>() {
//        }.getType();
//        return new Gson().fromJson(json, listType);
//    }
//    @TypeConverter
//    public static String fromLongList(List<Long> ids) {
//        Gson gson = new Gson();
//        String json = gson.toJson(ids);
//        return json;
//    }

    /**
     * Converter of NodeShape
     */
    @TypeConverter
    public static NodeShape toNodeShape(String json) {
        Type listType = new TypeToken<NodeShape>(){}.getType();
        return new Gson().fromJson(json, listType);
    }
    @TypeConverter
    public static String fromNodeShape(NodeShape data) {
        Gson gson = new Gson();
        String json = gson.toJson(data);
        return json;
    }

    /**
     * Converter of List<NodeShape>
     */
    @TypeConverter
    public static List<NodeShape> toNodeShapeList(String json) {
        Type listType = new TypeToken<List<NodeShape>>(){}.getType();
        return new Gson().fromJson(json, listType);
    }
    @TypeConverter
    public static String fromNodeShapeList(List<NodeShape> data) {
        Gson gson = new Gson();
        String gosJson = gson.toJson(data);
        return gosJson;
    }

    /**
     * Converter of List<GeoObject>
     */
    @TypeConverter
    public static List<GeoObject> toGeoObjectList(String json) {
        Type listType = new TypeToken<List<GeoObject>>(){}.getType();
        return new Gson().fromJson(json, listType);
    }
    @TypeConverter
    public static String fromGeoObjectList(List<GeoObject> data) {
        Gson gson = new Gson();
        String gosJson = gson.toJson(data);
        return gosJson;
    }
}