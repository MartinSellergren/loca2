package com.localore.localore.modelManipulation;

import android.content.Context;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Question;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.model.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Static class for running-exercise related operations (manipulate the database).
 */
public class RunningQuizControl {

    /**
     * Every geo-object in quiz gets this number of questions in a running-quiz...
     */
    public static final int DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT = 4;

    /**
     * ..except one (selected randomly) which gets this many extra questions.
     */
    public static final int NO_EXTRA_QUESTIONS = 3;

    /**
     * Required min success-rate to pass a level-quiz.
     */
    public static final double ACCEPTABLE_QUIZ_SUCCESS_RATE = 0.9;

    public static final int PASSED_LEVELS_BEFORE_EXERCISE_REMINDER = 4;

    public static final int MIN_NO_EXERCISE_REMINDERS = 2;

    public static final int MAX_NO_EXERCISE_REMINDERS = 5;

    public static final int MIN_NO_QUIZ_CATEGORY_REMINDERS = 0;

    public static final int MAX_NO_QUIZ_CATEGORY_REMINDERS = 3;



    //region new running-quiz

    /**
     * Set running-quiz to a new level-quiz (constructed from db quiz-data of active exercise).
     * @param quizCategoryType
     * @pre Session exercise set.
     */
    public static void newLevelQuiz(int quizCategoryType, Context context) {
        int runningQuizType = 0;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        long exerciseId = SessionControl.load(context).getExerciseId();
        if (exerciseId == -1) throw new RuntimeException("No session-exercise");

        Quiz quizData = loadCurrentLevelQuiz(exerciseId, quizCategoryType, context);
        List<GeoObject> levelGeoObjects =
                AppDatabase.getInstance(context).geoDao()
                        .loadWithQuiz(quizData.getId());

        newQuestions(levelGeoObjects, runningQuizId, context);
    }

    /**
     * Set running-quiz to a new follow-up-quiz (constructed from db running-quiz).
     * @param context
     */
    public static void newFollowUpQuiz(Context context) {
        long runningQuizId =
                AppDatabase.getInstance(context).runningQuizDao()
                        .loadOne().getId();
        List<Long> incorrectGeoObjectIds =
                AppDatabase.getInstance(context).questionDao()
                        .loadIdsIncorrectlyAnsweredWithRunningQuiz(runningQuizId);
        List<GeoObject> incorrectGeoObjects =
                AppDatabase.getInstance(context).geoDao()
                        .load(incorrectGeoObjectIds);

        int runningQuizType = 1;
        runningQuizId = newRunningQuiz(runningQuizType, context);

        newQuestions(incorrectGeoObjects, runningQuizId, context);
    }

    /**
     * Set running-quiz to a new level-reminder-quiz (generated semi-randomly
     * from geo-object-stats of active exercise).
     *
     * @param quizCategoryType
     * @param context
     * @pre Session exercise set.
     */
    public static void newLevelReminder(int quizCategoryType, Context context) {
        int runningQuizType = 2;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        long exerciseId = SessionControl.loadExercise(context).getId();
        QuizCategory quizCategory =
                AppDatabase.getInstance(context).quizCategoryDao()
                        .loadWithExerciseAndType(exerciseId, quizCategoryType);
        List<Long> quizIds =
                AppDatabase.getInstance(context).quizDao()
                        .loadPassedIdsWithQuizCategory(quizCategory.getId());
        List<Long> geoObjectCandidateIds =
                AppDatabase.getInstance(context).geoDao()
                        .loadIdsWithQuizIn(quizIds);
        List<GeoObject> geoObjects = pickReminderQuizGeoObjects(geoObjectCandidateIds);

        newQuestions(geoObjects, runningQuizId, context);
    }

    /**
     * Set running-quiz to a new exercise-reminder-quiz (generated semi-randomly
     * from geo-object-stats of active exercise).
     * @param context
     * @pre Session exercise set.
     */
    public static void newExerciseReminder(Context context) {
        int runningQuizType = 3;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        long exerciseId = SessionControl.load(context).getExerciseId();
        List<Long> quizCategoryIds =
                AppDatabase.getInstance(context).quizCategoryDao()
                        .loadIdsWithExercise(exerciseId);
        List<Long> quizIds =
                AppDatabase.getInstance(context).quizDao()
                        .loadPassedIdsWithQuizCategoryIn(quizCategoryIds);
        List<Long> geoObjectCandidateIds =
                AppDatabase.getInstance(context).geoDao()
                        .loadIdsWithQuizIn(quizIds);
        List<GeoObject> geoObjects = pickReminderQuizGeoObjects(geoObjectCandidateIds);

        newQuestions(geoObjects, runningQuizId, context);
    }

    /**
     * Pick geo-objects for a reminder-quiz. Semi-random selection. Favour old and
     * problematic (bad success-rate) geo-objects.
     * @param geoObjectCandidateIds
     * @return
     */
    private static List<GeoObject> pickReminderQuizGeoObjects(List<Long> geoObjectCandidateIds) {
        int noQuestions = ExerciseControl.MAX_NO_GEO_OBJECTS_IN_A_LEVEL * DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT;

        return new ArrayList<>();
    }

    /**
     * Replace current running-quiz in database with new one.
     * @param runningQuizType
     * @param context
     * @return Id of new running-quiz.
     */
    private static long newRunningQuiz(int runningQuizType, Context context) {
        deleteRunningQuiz(context);
        RunningQuiz runningQuiz = new RunningQuiz(runningQuizType);
        long runningQuizId =
                AppDatabase.getInstance(context).runningQuizDao()
                        .insert(runningQuiz);

        return runningQuizId;
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
     * Load current level of specified quiz-category in specified exercise
     * (i.e quiz with lowest level not yet done).
     * @param exerciseId
     * @param quizCategoryType
     * @param context
     */
    private static Quiz loadCurrentLevelQuiz(long exerciseId, int quizCategoryType, Context context) {
        QuizCategory quizCategory =
                AppDatabase.getInstance(context).quizCategoryDao()
                        .loadWithExerciseAndType(exerciseId, quizCategoryType);

        List<Quiz> quizzes =
                AppDatabase.getInstance(context).quizDao()
                        .loadWithQuizCategoryOrderedByLevel(quizCategory.getId());

        return quizzes.get(0);
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

        int extraIndex = new Random().nextInt(questionCounts.length);
        questionCounts[extraIndex] += NO_EXTRA_QUESTIONS;

        int[] questionDifficulties = new int[geoObjects.size()];
        for (int i = 0; i < questionDifficulties.length; i++)
            questionDifficulties[i] = 0;

        int questionIndex = 0;

        while (allIsZero(questionCounts)) {
            int i = new Random().nextInt(geoObjects.size());
            if (questionCounts[i] != 0) {
                questionCounts[i] -= 1;

                int index = questionIndex++;
                int difficulty = questionDifficulties[i]++;
                GeoObject geoObject = geoObjects.get(i);

                Question question = new Question(runningQuizId, geoObject, index, difficulty);
                AppDatabase.getInstance(context).questionDao().insert(question);
            }
        }
    }

    /**
     * @param xs
     * @return True if all elements = 0.
     */
    private static boolean allIsZero(int[] xs) {
        for (int x : xs) {
            if (x != 0) return false;
        }
        return true;
    }

    //endregion

    //region report question-results

    /**
     * Report result to question in running quiz.
     * @param question
     * @param correct
     * @param context
     */
    public static void reportQuestionResult(Question question, boolean correct, Context context) {
        //todo
    }

    //endregion

    //region report quiz-results

    /**
     * Call when a quiz is finished. Updates database based on quiz-type and result.
     *
     * Level-quiz: Set passed if satisfactory result and required reminders.
     * Follow-up: Update nothing.
     * Reminders: Decrement number of required reminders.
     *
     * @param context
     * @return Brief feedback.
     */
    public static String onFinishedRunningQuiz(Context context) {
        RunningQuiz runningQuiz = AppDatabase.getInstance(context).runningQuizDao().loadOne();
        Quiz quiz = loadQuizFromRunningQuiz(runningQuiz, context);
        QuizCategory quizCategory = loadQuizCategoryFromRunningQuiz(runningQuiz, context);
        Exercise exercise = SessionControl.loadExercise(context);

        List<Question> questions =
                AppDatabase.getInstance(context).questionDao()
                        .loadWithRunningQuiz(runningQuiz.getId());
        double successRate = successRate(questions);

        if (runningQuiz.getType() == RunningQuiz.LEVEL_QUIZ) {
            if (successRate >= ACCEPTABLE_QUIZ_SUCCESS_RATE)
                reportLevelPassed(quiz, context);

            setRequiredReminderQuizzes(quizCategory, context);
        }
        else if (runningQuiz.getType() == RunningQuiz.QUIZ_CATEGORY_REMINDER) {
            quizCategory.setRequiredCategoryReminders( quizCategory.getRequiredCategoryReminders()-1 );
            AppDatabase.getInstance(context).quizCategoryDao().update(quizCategory);
        }
        else if (runningQuiz.getType() == RunningQuiz.EXERCISE_REMINDER) {
            exercise.setRequiredGlobalReminders( exercise.getPassedLevelsSinceGlobalReminder()-1 );
        }

        return feedback(successRate);
    }

    /**
     * @param questions
     * @return Success-rate of questions.
     */
    private static double successRate(List<Question> questions) {
        double correctCount = 0;

        for (Question question : questions) {
            if (question.isAnsweredCorrectly()) correctCount++;
        }

        return correctCount / questions.size();
    }

    /**
     * @param context
     * @return Underlying quiz-data of running quiz.
     */
    public static Quiz loadQuizFromRunningQuiz(RunningQuiz runningQuiz, Context context) {
        Question question =
                AppDatabase.getInstance(context).questionDao()
                        .loadWithRunningQuiz(runningQuiz.getId()).get(0);
        GeoObject geoObject =
                AppDatabase.getInstance(context).geoDao()
                        .load(question.getGeoObjectId());
        return
                AppDatabase.getInstance(context).quizDao()
                        .load(geoObject.getQuizId());
    }

    public static QuizCategory loadQuizCategoryFromRunningQuiz(RunningQuiz runningQuiz, Context context) {
        Quiz quiz = loadQuizFromRunningQuiz(runningQuiz, context);
        return AppDatabase.getInstance(context).quizCategoryDao().load(quiz.getQuizCategoryId());
    }

    /**
     * Sets quiz passed and increments exercise.passedLevelsSinceGlobalReminder.
     * @param context
     */
    private static void reportLevelPassed(Quiz quiz, Context context) {
        quiz.setPassed(true);
        AppDatabase.getInstance(context).quizDao().update(quiz);

        Exercise exercise = SessionControl.loadExercise(context);
        exercise.setPassedLevelsSinceGlobalReminder( exercise.getPassedLevelsSinceGlobalReminder() + 1 );
    }

    /**
     * Set required reminder quizzes: exercise-reminders (update active exercise) and
     * quiz-category-reminders (update quiz-category of finished quiz).
     *
     * @param quizCategory Quiz-category of finished quiz.
     * @param context
     */
    private static void setRequiredReminderQuizzes(QuizCategory quizCategory, Context context) {
        //todo: based on n.o problematic/ old words (not random)

        Exercise exercise = SessionControl.loadExercise(context);
        if (exercise.getPassedLevelsSinceGlobalReminder() >= PASSED_LEVELS_BEFORE_EXERCISE_REMINDER) {
            int noExerciseReminders =
                    MIN_NO_EXERCISE_REMINDERS +
                            new Random().nextInt(
                                    MAX_NO_EXERCISE_REMINDERS - MIN_NO_EXERCISE_REMINDERS + 1);
            exercise.setRequiredGlobalReminders(noExerciseReminders);
            exercise.setPassedLevelsSinceGlobalReminder(0);
            AppDatabase.getInstance(context).exerciseDao().update(exercise);
        }

        int noQuizCategoryReminders =
                MIN_NO_QUIZ_CATEGORY_REMINDERS +
                        new Random().nextInt(
                                MAX_NO_QUIZ_CATEGORY_REMINDERS - MIN_NO_QUIZ_CATEGORY_REMINDERS + 1);
        quizCategory.setRequiredCategoryReminders(noQuizCategoryReminders);
        AppDatabase.getInstance(context).quizCategoryDao().update(quizCategory);
    }

    /**
     * @param successRate
     * @return poor/decent/good/excellent. Excellent means level passed.
     */
    private static String feedback(double successRate) {
        if (successRate >= ACCEPTABLE_QUIZ_SUCCESS_RATE) return "Excellent";
        else return "Poor";
    }

    //endregion

}
