package com.localore.localore;

import android.content.Context;
import android.util.Log;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Question;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.model.Session;
import com.localore.localore.model.User;
import com.localore.localore.modelManipulation.SessionControl;

import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class LocaUtils {

    private static Random random = new Random();

    /**
     * @param lowerBound
     * @param upperBound
     * @return Random integer <- [lowerBound, upperBound)
     */
    public static int randi(int lowerBound, int upperBound) {
        if (upperBound < lowerBound || lowerBound < 0 || upperBound < 1) return 0;
        return lowerBound + random.nextInt(upperBound - lowerBound);
    }

    /**
     * @param upperBound
     * @return Random integer <- [0, upperBound)
     */
    public static int randi(int upperBound) {
        if (upperBound < 1) return 0;
        return random.nextInt(upperBound);
    }

    /**
     * @param res Resource id of text-file.
     * @return Whole content of file.
     */
    public static String readTextFile(int res, Context context) {
        InputStream stream = context.getResources().openRawResource(res);
        Scanner scanner = new Scanner(stream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * @param lon,lat
     * @return Distance in meters between points.
     */
    public static double distance(double[] lon, double[] lat) {
        double lng1 = lon[0]; double lat1 = lon[1];
        double lng2 = lat[0]; double lat2 = lat[1];

        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        return dist;
    }


    //region Logging

    /**
     * Log all objects in specified database.
     * @param db
     */
    public static void logGeoObjects(AppDatabase db) {
        List<Long> ids = db.geoDao().loadAllIds();
        Log.i("<ME>", "log geo objects, count: " + ids.size());

        for (long id : ids) {
            GeoObject go = db.geoDao().load(id);
            Log.i("<ME>", go.toString() + "\n");
        }

        Log.i("<ME>", "COUNT: " + db.geoDao().count());
    }


    /**
     * Log the main database.
     */
    public static void logDatabase(AppDatabase db) {
        Log.i("<DB>", "********** DB START **********");

        List<User> users = db.userDao().loadAll();
        for (User user : users) {
            Log.i("<DB>", "*USER: " + user.toString());
            logExercises(user.getId(), db);
        }

        logRunningQuiz(db);
        logSession(db);

        Log.i("<DB>", "*********** DB END ***********");
    }

    /**
     * Log exercises (and all underlying content) of a user.
     * @param userId
     * @param db
     */
    public static void logExercises(long userId, AppDatabase db) {
        List<Exercise> exercises =
                db.exerciseDao()
                        .loadWithUserOrderedByDisplayIndex(userId);

        for (Exercise exercise : exercises) {
            Log.i("<DB>", "**EXERCISE: " + exercise.toString());
            logQuizCategories(exercise.getId(), db);
        }
    }

    /**
     * Log quiz-categories (and all underlying content) of an exercise.
     * @param exerciseId
     * @param db
     */
    public static void logQuizCategories(long exerciseId, AppDatabase db) {
        List<QuizCategory> quizCategories = db.quizCategoryDao()
                .loadWithExerciseOrderedByType(exerciseId);

        for (QuizCategory quizCategory : quizCategories) {
            Log.i("<DB>", "***QUIZ-CATEGORY: " + quizCategory.toString());
            logQuizzes(quizCategory.getId(), db);
        }
    }

    /**
     * Log quizzes (and all underlying content) of a quiz-category.
     * @param quizCategoryId
     * @param db
     */
    public static void logQuizzes(long quizCategoryId, AppDatabase db) {
        List<Quiz> quizzes = db.quizDao()
                .loadWithQuizCategoryOrderedByLevel(quizCategoryId);

        for (Quiz quiz : quizzes) {
            Log.i("<DB>", "****QUIZ: " + quiz.toString());
            logGeoObjects(quiz.getId(), db);
        }
    }

    /**
     * Log geo-objects of a quiz.
     * @param quizId
     * @param db
     */
    public static void logGeoObjects(long quizId, AppDatabase db) {
        Log.i("<DB>", "*****NO GEO-OBJECTS: " + db.geoDao().countInQuiz(quizId));
//
//        List<Long> geoObjectIds = db.geoDao().loadIdsWithQuizOrderedByRank(quizId);
//
//        for (long geoObjectId : geoObjectIds) {
//            GeoObject geoObject = db.geoDao().load(geoObjectId);
//            Log.i("<DB>", "*****GEO-OBJECT: " + geoObject.toString());
//        }
    }

    /**
     * Log running-quiz, with questions.
     * @param db
     */
    public static void logRunningQuiz(AppDatabase db) {
        RunningQuiz runningQuiz = db.runningQuizDao().loadOne();
        if (runningQuiz == null) {
            Log.i("<DB>", "-No running quiz");
            return;
        }
        Log.i("<DB>", "-RUNNING-QUIZ: " + runningQuiz.toString());

        List<Question> questions = db.questionDao()
                .loadWithRunningQuizOrderedByIndex(runningQuiz.getId());
        for (Question question : questions) {
            Log.i("<DB>", "--QUESTION: " + question.toString());
        }
    }

    /**
     * Log session..
     * @param db
     */
    public static void logSession(AppDatabase db) {
        Session session = SessionControl.load(db);
        Log.i("<DB>", session.toString());
    }

    //endregion
}
