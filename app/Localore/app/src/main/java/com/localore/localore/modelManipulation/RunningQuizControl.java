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
 * Assumes there's no more than one running-quiz in the database.
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


    //region shortcuts

    public static RunningQuiz load(Context context) {
        return AppDatabase.getInstance(context).runningQuizDao().loadOne();
    }

    public static GeoObject loadGeoObjectFromQuestion(Question question, Context context) {
        return AppDatabase.getInstance(context).geoDao().load(question.getGeoObjectId());
    }

    /**
     * @param context
     * @return Underlying quiz-data of running quiz.
     */
    public static Quiz loadQuizFromRunningQuiz(RunningQuiz runningQuiz, Context context) {
        Question question =
                AppDatabase.getInstance(context).questionDao()
                        .loadWithRunningQuiz(runningQuiz.getId()).get(0);
        GeoObject geoObject = loadGeoObjectFromQuestion(question, context);
        return AppDatabase.getInstance(context).quizDao()
                .load(geoObject.getQuizId());
    }

    public static QuizCategory loadQuizCategoryFromRunningQuiz(RunningQuiz runningQuiz, Context context) {
        Quiz quiz = loadQuizFromRunningQuiz(runningQuiz, context);
        return AppDatabase.getInstance(context).quizCategoryDao().load(quiz.getQuizCategoryId());
    }

    //endregion

    //region new running-quiz

    /**
     * Set running-quiz to a new level-quiz (constructed from db quiz-data of exercise).
     * @param exerciseId
     * @param quizCategoryType
     * @param context
     */
    public static void newLevelQuiz(long exerciseId, int quizCategoryType, Context context) {
        int runningQuizType = 0;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        Quiz quizData = loadCurrentLevelQuiz(exerciseId, quizCategoryType, context);
        List<GeoObject> levelGeoObjects =
                AppDatabase.getInstance(context).geoDao()
                        .loadWithQuiz(quizData.getId());

        newQuestions(levelGeoObjects, runningQuizId, context);
    }

    /**
     * Set running-quiz to a new follow-up-quiz (constructed from db running-quiz).
     * @param context
     * @pre A running-quiz in db.
     */
    public static void newFollowUpQuiz(Context context) {
        long runningQuizId = load(context).getId();
        List<Question> incorrectQuestions =
                AppDatabase.getInstance(context)
                        .questionDao().loadIncorrectWithRunningQuizOrderedByIndex(runningQuizId);

        int runningQuizType = 1;
        runningQuizId = newRunningQuiz(runningQuizType, context);

        for (int i = 0; i < incorrectQuestions.size(); i++) {
            Question question = incorrectQuestions.get(i);
            question.setRunningQuizId(runningQuizId);
            question.setIndex(i);
            AppDatabase.getInstance(context).questionDao().insert(question);
        }
    }

    /**
     * Set running-quiz to a new level-reminder-quiz (generated semi-randomly
     * from geo-object-stats of exercise).
     *
     * @param exerciseId
     * @param quizCategoryType
     * @param context
     */
    public static void newLevelReminder(long exerciseId, int quizCategoryType, Context context) {
        int runningQuizType = 2;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        QuizCategory quizCategory =
                AppDatabase.getInstance(context).quizCategoryDao()
                        .loadWithExerciseAndType(exerciseId, quizCategoryType);
        List<Long> quizIds =
                AppDatabase.getInstance(context).quizDao()
                        .loadPassedIdsWithQuizCategory(quizCategory.getId());
        List<Long> geoObjectCandidateIds =
                AppDatabase.getInstance(context).geoDao()
                        .loadIdsWithQuizIn(quizIds);
        List<GeoObject> geoObjects = pickReminderQuizGeoObjects(geoObjectCandidateIds, context);

        newQuestions(geoObjects, runningQuizId, context);
    }

    /**
     * Set running-quiz to a new exercise-reminder-quiz (generated semi-randomly
     * from geo-object-stats of exercise).
     *
     * @param exerciseId
     * @param context
     */
    public static void newExerciseReminder(long exerciseId, Context context) {
        int runningQuizType = 3;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        List<Long> quizCategoryIds =
                AppDatabase.getInstance(context).quizCategoryDao()
                        .loadIdsWithExercise(exerciseId);
        List<Long> quizIds =
                AppDatabase.getInstance(context).quizDao()
                        .loadPassedIdsWithQuizCategoryIn(quizCategoryIds);
        List<Long> geoObjectCandidateIds =
                AppDatabase.getInstance(context).geoDao()
                        .loadIdsWithQuizIn(quizIds);
        List<GeoObject> geoObjects = pickReminderQuizGeoObjects(geoObjectCandidateIds, context);

        newQuestions(geoObjects, runningQuizId, context);
    }

    /**
     * Pick geo-objects for a reminder-quiz. Semi-random selection. Favour old and
     * problematic (bad success-rate) geo-objects.
     *
     * @param geoObjectCandidateIds
     * @return Selected geo-objects for reminder.
     */
    private static List<GeoObject> pickReminderQuizGeoObjects(List<Long> geoObjectCandidateIds, Context context) {
        int noQuestions = ExerciseControl.MAX_NO_GEO_OBJECTS_IN_A_LEVEL * DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT;

        //todo: select relevant objects

        List<GeoObject> selected = new ArrayList<>();

        for (int i = 0; i < noQuestions; i++) {
            long id = geoObjectCandidateIds.get(i);
            GeoObject geoObject = AppDatabase.getInstance(context).geoDao().load(id);
            selected.add(geoObject);
        }
        return selected;
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
     * Deleted running-quiz from database (including underlying questions), if one exists.
     * @param context
     */
    private static void deleteRunningQuiz(Context context) {
        RunningQuiz runningQuiz = load(context);
        if (runningQuiz == null) return;

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
        if (geoObjects.size() == 0) return;

        int[] questionCounts = new int[geoObjects.size()];
        for (int i = 0; i < questionCounts.length; i++)
            questionCounts[i] = DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT;

        int extraIndex = new Random().nextInt(questionCounts.length);
        questionCounts[extraIndex] += NO_EXTRA_QUESTIONS;

        int[] questionDifficulties = new int[geoObjects.size()];
        for (int i = 0; i < questionDifficulties.length; i++)
            questionDifficulties[i] = 0;

        int questionIndex = 0;

        while (!allIsZero(questionCounts)) {
            int i = new Random().nextInt(geoObjects.size());
            if (questionCounts[i] > 0) {
                questionCounts[i] -= 1;

                int index = questionIndex++;
                int difficulty = questionDifficulties[i]++;
                GeoObject geoObject = geoObjects.get(i);

                Question question = new Question(runningQuizId, geoObject, index, difficulty, context);
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

    //region question-operations

    /**
     * Report result of question in running quiz. Depends on type of running-quiz.
     * For a Pair-it question: call this multiple times.
     * Db-updates: Question in running-quiz, and Geo-object (defining question) stats.
     *
     * @param question
     * @param correct
     * @param context
     */
    public static void reportQuestionResult(Question question, boolean correct, Context context) {
        if (correct) question.setAnsweredCorrectly(true);
        else question.setAnsweredCorrectly(false);
        AppDatabase.getInstance(context).questionDao().update(question);

        RunningQuiz runningQuiz = load(context);
        double askWeight = 1;
        if (question.getType() == Question.PAIR_IT) askWeight *= 0.5;
        if (runningQuiz.getType() == RunningQuiz.FOLLOW_UP_QUIZ) askWeight *= 0.5;

        GeoObject geoObject = loadGeoObjectFromQuestion(question, context);
        geoObject.setTimesAsked( geoObject.getTimesAsked() + askWeight );
        if (correct) {
            geoObject.setNoCorrectAnswers(geoObject.getNoCorrectAnswers() + askWeight);
            geoObject.setTimeOfPreviousCorrectAnswer(System.currentTimeMillis());
        }

        AppDatabase.getInstance(context).geoDao().update(geoObject);
    }

    /**
     * Call to start quiz, and to progress to next question.
     *
     * - Returns next question in a quiz-run (NULL if no more).
     * - Db-update: current question in running-quiz.
     *
     * @param context
     * @return Next question, or NULL if quiz is done.
     */
    public static Question nextQuestion(Context context) {
        RunningQuiz runningQuiz = load(context);
        int nextQuestionIndex = runningQuiz.getCurrentQuestionIndex() + 1;
        runningQuiz.setCurrentQuestionIndex(nextQuestionIndex);
        AppDatabase.getInstance(context).runningQuizDao().update(runningQuiz);

        return AppDatabase.getInstance(context).questionDao()
                .loadWithRunningQuizAndIndex(runningQuiz.getId(), nextQuestionIndex);
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
     * @param exercise
     * @param context
     * @return Brief feedback.
     */
    public static String onFinishedRunningQuiz(Exercise exercise, Context context) {
        RunningQuiz runningQuiz = load(context);
        Quiz quiz = loadQuizFromRunningQuiz(runningQuiz, context);
        QuizCategory quizCategory = loadQuizCategoryFromRunningQuiz(runningQuiz, context);

        List<Question> questions =
                AppDatabase.getInstance(context).questionDao()
                        .loadWithRunningQuiz(runningQuiz.getId());
        double successRate = successRate(questions);

        if (runningQuiz.getType() == RunningQuiz.LEVEL_QUIZ) {
            if (successRate >= ACCEPTABLE_QUIZ_SUCCESS_RATE)
                reportLevelPassed(exercise, quiz, context);

            setRequiredReminderQuizzes(exercise, quizCategory, context);
        }
        else if (runningQuiz.getType() == RunningQuiz.QUIZ_CATEGORY_REMINDER) {
            quizCategory.setRequiredNoCategoryReminders( quizCategory.getRequiredNoCategoryReminders()-1 );
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
     * Sets quiz passed and increments passed-levels-since-global-reminder of exercise.
     * @param exercise
     * @param context
     */
    private static void reportLevelPassed(Exercise exercise, Quiz quiz, Context context) {
        quiz.setPassed(true);
        AppDatabase.getInstance(context).quizDao().update(quiz);

        exercise.setPassedLevelsSinceGlobalReminder( exercise.getPassedLevelsSinceGlobalReminder() + 1 );
        AppDatabase.getInstance(context).exerciseDao().update(exercise);
    }

    /**
     * Set required reminder quizzes: exercise-reminders (update exercise) and
     * quiz-category-reminders (update quiz-category of finished quiz).
     *
     * @param exercise
     * @param quizCategory Quiz-category of finished quiz.
     * @param context
     */
    private static void setRequiredReminderQuizzes(Exercise exercise, QuizCategory quizCategory, Context context) {
        //todo: based on n.o problematic/ old words (not random)

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
        quizCategory.setRequiredNoCategoryReminders(noQuizCategoryReminders);
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
