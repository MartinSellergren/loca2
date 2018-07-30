package com.localore.localore.modelManipulation;

import android.content.Context;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Question;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Static class for running-exercise related operations (manipulate the database).
 */
public class RunningQuizControl {

    /**
     * Set running-quiz to a new level-quiz (construct it from predefined quiz-data).
     */
    public static Quiz newLevelQuiz(long exerciseId, int quizCategoryType, Context context) {
        deleteRunningQuiz(context);
        Quiz quizData = loadCurrentLevelQuiz(exerciseId, quizCategoryType, context);
        List<GeoObject> levelGeoObjects =
                AppDatabase.getInstance(context).geoDao()
                        .loadWithQuiz(quizData.getId());

        long runningQuizId = AppDatabase.getInstance(context).runningQuizDao().loadOne().getId();
        newQuestions(levelGeoObjects, runningQuizId, context);

        return quizData;
    }

    /**
     * Deleted running-quiz from database (including underlying questions).
     * @param context
     */
    private static void deleteRunningQuiz(Context context) {
        RunningQuiz runningQuiz =
                AppDatabase.getInstance(context).runningQuizDao()
                        .loadOne();
        List<Question> questions =
                AppDatabase.getInstance(context).questionDao()
                        .loadWithRunningQuiz(runningQuiz.getId());

        for (Question question : questions)
            AppDatabase.getInstance(context).questionDao().delete(question);

        AppDatabase.getInstance(context).runningQuizDao().delete(runningQuiz);
    }

    /**
     * Load current level of specified quiz-category in specified exercise.
     * @param exerciseId
     * @param quizCategoryType
     * @param context
     */
    private static Quiz loadCurrentLevelQuiz(long exerciseId, int quizCategoryType, Context context) {
        int level = currentLevelInQuizCateogry(exerciseId, quizCategoryType);

        QuizCategory quizCategory =
                AppDatabase.getInstance(context).quizCategoryDao()
                        .loadQuizCategoryWithExerciseAndType(exerciseId, quizCategoryType);
        Quiz quiz =
                AppDatabase.getInstance(context).quizDao()
                        .loadWithQuizCategoryAndLevel(quizCategory.getId(), level);

        return quiz;
    }

    /**
     * Construct new questions about specified geo-objects (for specified running-quiz)
     * and insert into db.
     * Number of questions per geo-object specified in constant. Order semi-randomized.
     * Question difficulty increases.
     *
     * @param geoObjects
     * @param runningQuizId
     * @param context
     */
    private static void newQuestions(List<GeoObject> geoObjects, long runningQuizId, Context context) {
        int[] questionCounts = new int[geoObjects.size()];
        for (int i = 0; i < questionCounts.length; i++)
            questionCounts[i] = DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT;

        // extra questions for one geo-object
        int boostIndex = new Random().nextInt(questionCounts.length);
        questionCounts[boostIndex] += NO_EXTRA_QUESTIONS;

        int[] questionDifficulties = new int[geoObjects.size()];
        for (int i = 0; i < questionDifficulties.length; i++)
            questionDifficulties[i] = 0;

        int questionIndex = 0;

        while (allZero(questionCounts)) {
            int i = new Random().nextInt(questionCounts.length);
            if (questionCounts[i] != 0) {
                questionCounts[i] -= 1;

                int difficulty = questionDifficulties[i]++;
                int index = questionIndex++;
                GeoObject geoObject = geoObjects.get(i);

                Question question = new Question(runningQuizId, geoObject, index);
                AppDatabase.getInstance(context).questionDao().insert(question);
            }
        }
    }

}
