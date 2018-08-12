package com.localore.localore.modelManipulation;

import android.content.Context;

import com.localore.localore.LocaUtils;
import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Question;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;

import java.util.ArrayList;
import java.util.List;

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
    public static final double ACCEPTABLE_QUIZ_SUCCESS_RATE = 0.85;

    public static final int NO_PASSED_LEVELS_BEFORE_EXERCISE_REMINDER = 4;

    public static final int MIN_NO_EXERCISE_REMINDERS = 2;

    public static final int MAX_NO_EXERCISE_REMINDERS = 5;

    public static final int MIN_NO_QUIZ_CATEGORY_REMINDERS = 0;

    public static final int MAX_NO_QUIZ_CATEGORY_REMINDERS = 3;


    //region shortcuts

    /**
     * Loads a running-quiz.
     * @param context
     * @return Running-quiz or NULL.
     */
    public static RunningQuiz load(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        return db.runningQuizDao().loadOne();
    }

    /**
     *
     * @param context
     * @return True if a quiz is running (exists in db).
     */
    public static boolean isCurrentlyRunning(Context context) {
        return load(context) != null;
    }

    /**
     * @param question
     * @param context
     * @return Geo-object of question.
     */
    public static GeoObject loadGeoObjectFromQuestion(Question question, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        return db.geoDao().load(question.getGeoObjectId());
    }

    /**
     * @param runningQuiz
     * @param context
     * @return Underlying quiz-data of running quiz.
     */
    public static Quiz loadQuizFromRunningQuiz(RunningQuiz runningQuiz, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        Question question = db.questionDao().loadWithRunningQuiz(runningQuiz.getId()).get(0);
        GeoObject geoObject = loadGeoObjectFromQuestion(question, context);
        return db.quizDao().load(geoObject.getQuizId());
    }

    public static QuizCategory loadQuizCategoryFromRunningQuiz(RunningQuiz runningQuiz, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        Quiz quiz = loadQuizFromRunningQuiz(runningQuiz, context);
        return db.quizCategoryDao().load(quiz.getQuizCategoryId());
    }

    /**
     * @param exerciseId
     * @param db
     * @return Id of all geo-objects in passed quizzes in an exercise.
     */
    public static List<Long> loadIdOfGeoObjectsInPassedQuizzesInExercise(Long exerciseId, AppDatabase db) {
        List<Long> quizCategoryIds = db.quizCategoryDao().loadIdsWithExercise(exerciseId);
        List<Long> quizIds = db.quizDao().loadPassedIdsWithQuizCategoryIn(quizCategoryIds);
        return db.geoDao().loadIdsWithQuizIn(quizIds);
    }

    /**
     * @param context
     * @return Number of questions in current running-quiz.
     */
    public static int noQuestions(Context context) {
        RunningQuiz runningQuiz = load(context);
        if (runningQuiz == null) return 0;
        return AppDatabase.getInstance(context).questionDao().countWithRunningQuiz(runningQuiz.getId());
    }

    public static Question loadCurrentQuestion(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        RunningQuiz runningQuiz = RunningQuizControl.load(context);
        int currentQuestionIndex = runningQuiz.getCurrentQuestionIndex();
        return db.questionDao().loadWithRunningQuizAndIndex(runningQuiz.getId(), currentQuestionIndex);
    }

    public static GeoObject loadGeoObjectOfCurrentQuestion(Context context) {
        Question currentQuestion = loadCurrentQuestion(context);
        return AppDatabase.getInstance(context).geoDao()
                .load(currentQuestion.getGeoObjectId());
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
        AppDatabase db = AppDatabase.getInstance(context);
        int runningQuizType = 0;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        Quiz quizData = ExerciseControl.loadNextLevelQuiz(exerciseId, quizCategoryType, db);
        if (quizData == null) return;
        List<GeoObject> levelGeoObjects = db.geoDao().loadWithQuiz(quizData.getId());

        newQuestions(levelGeoObjects, runningQuizId, db);
    }

    /**
     * Set running-quiz to a new follow-up-quiz (constructed from db running-quiz).
     * @param context
     * @pre A running-quiz in db.
     */
    public static void newFollowUpQuiz(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        long runningQuizId = load(context).getId();
        List<Question> incorrectQuestions = db.questionDao()
                        .loadIncorrectWithRunningQuizOrderedByIndex(runningQuizId);

        int runningQuizType = 1;
        runningQuizId = newRunningQuiz(runningQuizType, context);

        List<Question> newQuestions = new ArrayList<>();
        for (int i = 0; i < incorrectQuestions.size(); i++) {
            Question question = incorrectQuestions.get(i);
            question.setRunningQuizId(runningQuizId);
            question.setIndex(i);
            newQuestions.add(question);
        }
        db.questionDao().insert(newQuestions);
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
        AppDatabase db = AppDatabase.getInstance(context);
        int runningQuizType = 2;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        QuizCategory quizCategory = db.quizCategoryDao().loadWithExerciseAndType(exerciseId, quizCategoryType);
        List<Long> quizIds = db.quizDao().loadPassedIdsWithQuizCategory(quizCategory.getId());
        List<Long> geoObjectCandidateIds = db.geoDao().loadIdsWithQuizIn(quizIds);
        List<GeoObject> geoObjects = pickReminderQuizGeoObjects(geoObjectCandidateIds, db);

        newQuestions(geoObjects, runningQuizId, db);
    }

    /**
     * Set running-quiz to a new exercise-reminder-quiz (generated semi-randomly
     * from geo-object-stats of exercise).
     *
     * @param exerciseId
     * @param context
     */
    public static void newExerciseReminder(long exerciseId, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        int runningQuizType = 3;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        List<Long> geoObjectCandidateIds = loadIdOfGeoObjectsInPassedQuizzesInExercise(exerciseId, db);
        List<GeoObject> geoObjects = pickReminderQuizGeoObjects(geoObjectCandidateIds, db);
        newQuestions(geoObjects, runningQuizId, db);
    }

    /**
     * Pick geo-objects for a reminder-quiz. Semi-random selection. Favour old and
     * problematic (bad success-rate) geo-objects.
     *
     * @param geoObjectCandidateIds
     * @return Selected geo-objects for reminder.
     */
    private static List<GeoObject> pickReminderQuizGeoObjects(List<Long> geoObjectCandidateIds, AppDatabase db) {
        int noQuestions = Math.min(
                ExerciseControl.MAX_NO_GEO_OBJECTS_IN_A_LEVEL * DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT,
                geoObjectCandidateIds.size());

        //todo: select relevant objects

        List<GeoObject> selected = new ArrayList<>();

        for (int i = 0; i < noQuestions; i++) {
            long id = geoObjectCandidateIds.get(i);
            GeoObject geoObject = db.geoDao().load(id);
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
        AppDatabase db = AppDatabase.getInstance(context);
        deleteRunningQuiz(context);
        RunningQuiz runningQuiz = new RunningQuiz(runningQuizType);
        long runningQuizId = db.runningQuizDao().insert(runningQuiz);

        return runningQuizId;
    }

    /**
     * Deleted running-quiz from database (including underlying questions), if one exists.
     * @param context
     */
    public static void deleteRunningQuiz(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        RunningQuiz runningQuiz = load(context);
        if (runningQuiz == null) return;

        List<Question> questions =
                db.questionDao().loadWithRunningQuiz(runningQuiz.getId());

        db.questionDao().delete(questions);
        db.runningQuizDao().delete(runningQuiz);
    }

    /**
     * Construct new questions about specified geo-objects (for specified running-quiz)
     * and insert into db.
     * Number of questions per geo-object specified in constant. Order semi-randomized.
     * Question difficulty increases.
     *
     * @param geoObjects
     * @param runningQuizId
     * @param db
     */
    private static void newQuestions(List<GeoObject> geoObjects, long runningQuizId, AppDatabase db) {
        if (geoObjects.size() == 0) return;

        int[] questionCounts = new int[geoObjects.size()];
        for (int i = 0; i < questionCounts.length; i++)
            questionCounts[i] = DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT;

        int extraIndex = LocaUtils.randi(questionCounts.length);
        questionCounts[extraIndex] += NO_EXTRA_QUESTIONS;

        int[] questionDifficulties = new int[geoObjects.size()];
        for (int i = 0; i < questionDifficulties.length; i++)
            questionDifficulties[i] = 0;

        int questionIndex = 0;
        List<Question> newQuestions = new ArrayList<>();

        while (!allIsZero(questionCounts)) {
            int i = LocaUtils.randi(geoObjects.size());
            if (questionCounts[i] > 0) {
                questionCounts[i] -= 1;

                int index = questionIndex++;
                int difficulty = questionDifficulties[i]++;
                GeoObject geoObject = geoObjects.get(i);

                Question question = new Question(runningQuizId, geoObject, index, difficulty, db);
                newQuestions.add(question);
            }
        }

        db.questionDao().insert(newQuestions);
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
     * Report result of question in running quiz. Does nothing if question already reported.
     * Don't report pair-it questions.
     * Db-updates: Question (answered/correct) in running-quiz, and Geo-object (defining question) stats.
     *
     * @param question
     * @param correct
     * @param context
     */
    public static void reportQuestionResult(Question question, boolean correct, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        Question dbQuestion = db.questionDao().load(question.getId());
        if (dbQuestion.isAnswered()) return;

        question.setAnswered(true);
        if (correct) question.setAnsweredCorrectly(true);
        else question.setAnsweredCorrectly(false);
        db.questionDao().update(question);

        RunningQuiz runningQuiz = load(context);
        double askWeight = 1;
        if (runningQuiz.getType() == RunningQuiz.FOLLOW_UP_QUIZ) askWeight *= 0.5;

        GeoObject geoObject = loadGeoObjectFromQuestion(question, context);
        geoObject.setTimesAsked( geoObject.getTimesAsked() + askWeight );
        if (correct) {
            geoObject.setNoCorrectAnswers(geoObject.getNoCorrectAnswers() + askWeight);
            geoObject.setTimeOfPreviousCorrectAnswer(System.currentTimeMillis());
        }

        db.geoDao().update(geoObject);
    }

    /**
     * Call to freshStart quiz, and to progress to next question.
     *
     * - Returns next question in a quiz-run (NULL if no more).
     * - Db-update: current question in running-quiz.
     *
     * @param context
     * @return Next question, or NULL if quiz is done.
     */
    public static Question nextQuestion(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        RunningQuiz runningQuiz = load(context);
        int nextQuestionIndex = runningQuiz.getCurrentQuestionIndex() + 1;
        runningQuiz.setCurrentQuestionIndex(nextQuestionIndex);
        db.runningQuizDao().update(runningQuiz);

        return db.questionDao()
                .loadWithRunningQuizAndIndex(runningQuiz.getId(), nextQuestionIndex);
    }

    //endregion

    //region report quiz-results

    /**
     * Call when a quiz is finished. Updates database based on quiz-type and result.
     * Removes running-quiz from db.
     *
     * Level-quiz: If satisfactory result: Set passed and increment level, and set required reminders.
     * Follow-up: Update nothing.
     * Reminders: Decrement number of required reminders.
     *
     * @param exercise
     * @param context
     * @return Brief feedback.
     */
    public static String onFinishedRunningQuiz(Exercise exercise, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        RunningQuiz runningQuiz = load(context);
        Quiz quiz = loadQuizFromRunningQuiz(runningQuiz, context);
        QuizCategory quizCategory = loadQuizCategoryFromRunningQuiz(runningQuiz, context);

        List<Question> questions =
                db.questionDao()
                        .loadWithRunningQuiz(runningQuiz.getId());
        double successRate = successRate(questions);

        if (runningQuiz.getType() == RunningQuiz.LEVEL_QUIZ) {
            if (successRate >= ACCEPTABLE_QUIZ_SUCCESS_RATE) {
                reportLevelPassed(exercise, quiz, db);
                setNoRequiredReminders(exercise, quizCategory, db);
            }
        }
        else if (runningQuiz.getType() == RunningQuiz.QUIZ_CATEGORY_REMINDER) {
            decrementNoRequiredQuizCategoryReminders(quizCategory, db);
        }
        else if (runningQuiz.getType() == RunningQuiz.EXERCISE_REMINDER) {
            decrementNoRequiredExerciseReminders(exercise, db);
        }

        deleteRunningQuiz(context);
        return feedback(successRate);
    }

    /**
     * Decrement n.o required quiz-category reminders (keep above zero).
     * @param quizCategory
     * @param db
     */
    private static void decrementNoRequiredQuizCategoryReminders(QuizCategory quizCategory, AppDatabase db) {
        int noReminders = Math.max(0, quizCategory.getNoRequiredReminders() - 1);
        quizCategory.setNoRequiredReminders(noReminders);
        db.quizCategoryDao().update(quizCategory);
    }

    /**
     * Decrement n.o required exercise reminders (keep above zero).
     * @param exercise
     * @param db
     */
    private static void decrementNoRequiredExerciseReminders(Exercise exercise, AppDatabase db) {
        int noReminders = Math.max(0, exercise.getNoRequiredExerciseReminders() - 1);
        exercise.setNoRequiredExerciseReminders(noReminders);
        db.exerciseDao().update(exercise);
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
     * @param db
     */
    private static void reportLevelPassed(Exercise exercise, Quiz quiz, AppDatabase db) {
        quiz.setPassed(true);
        db.quizDao().update(quiz);

        exercise.setNoPassedLevelsSinceExerciseReminder( exercise.getNoPassedLevelsSinceExerciseReminder() + 1 );
        db.exerciseDao().update(exercise);
    }

    /**
     * Derive and set number of required reminder quizzes for a quiz-category and exercise.
     *
     * Only updates n.o exercise-reminders if enough quizzes passed since last time required.
     * If new exercise-require: set n.o passed levels since exercise-reminder to zero.
     *
     * @param exercise
     * @param quizCategory
     * @param db
     */
    private static void setNoRequiredReminders(Exercise exercise, QuizCategory quizCategory, AppDatabase db) {
        //todo: based on n.o problematic/ old words (not random)

        if (exercise.getNoPassedLevelsSinceExerciseReminder() >= NO_PASSED_LEVELS_BEFORE_EXERCISE_REMINDER) {
            int noExerciseReminders = LocaUtils.randi(MIN_NO_EXERCISE_REMINDERS, MAX_NO_EXERCISE_REMINDERS + 1);
            exercise.setNoRequiredExerciseReminders(noExerciseReminders);
            exercise.setNoPassedLevelsSinceExerciseReminder(0);
            db.exerciseDao().update(exercise);
        }

        int noQuizCategoryReminders = LocaUtils.randi(MIN_NO_QUIZ_CATEGORY_REMINDERS, MAX_NO_QUIZ_CATEGORY_REMINDERS + 1);
        quizCategory.setNoRequiredReminders(noQuizCategoryReminders);
        db.quizCategoryDao().update(quizCategory);
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
