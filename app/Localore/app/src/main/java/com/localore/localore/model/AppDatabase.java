package com.localore.localore.model;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;


@Database(entities = {
        Exercise.class,
        GeoObject.class,
        Question.class,
        Quiz.class,
        QuizCategory.class,
        RunningQuiz.class,
        Session.class,
        User.class},
        version = 1)
@TypeConverters({AppDatabaseConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract ExerciseDao exerciseDao();
    public abstract GeoObjectDao geoDao();
    public abstract QuestionDao questionDao();
    public abstract QuizDao quizDao();
    public abstract QuizCategoryDao quizCategoryDao();
    public abstract RunningQuizDao runningQuizDao();
    public abstract SessionDao sessionDao();
    public abstract UserDao userDao();

    private static AppDatabase INSTANCE;
    private static String database_name = "localore-db";

    private static AppDatabase TEMP_INSTANCE;
    private static String temp_database_name = "temp-db";

    // allow main thread queries for now..

    /**
     * @param context
     * @return Instance for access to main db.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            //todo: don't allow main thread queries..
            INSTANCE = Room.databaseBuilder(context, AppDatabase.class, database_name).allowMainThreadQueries().build();
        }
        return INSTANCE;
    }

    /**
     * Temp-database useful during creation of exercises.
     * @param context
     * @return Instance for access to temp db.
     */
    public static AppDatabase getTempInstance(Context context) {
        if (TEMP_INSTANCE == null) {
            TEMP_INSTANCE = Room.databaseBuilder(context, AppDatabase.class, temp_database_name).build();
        }
        return TEMP_INSTANCE;
    }


    /**
     * Close the main-db.
     */
    public static void closeMain() {
        if (INSTANCE != null) INSTANCE.close();
        INSTANCE = null;
    }

    /**
     * Close the temp-db.
     */
    public static void closeTemp() {
        if (TEMP_INSTANCE != null) TEMP_INSTANCE.close();
        TEMP_INSTANCE = null;
    }
}
