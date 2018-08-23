package com.localore.localore.modelManipulation;

import android.content.Context;

import com.localore.localore.LocaUtils;
import com.localore.localore.QuizResultActivity;
import com.localore.localore.R;
import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.Question;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Static class for running-exercise related operations (manipulate the database).
 * Assumes there's no more than one running-quiz in the database.
 */
public class RunningQuizControl {

    /**
     * Every geo-object in quiz gets this number of questions in a running-quiz...
     */
    public static final int DEFAULT_NO_QUESTIONS_PER_GEO_OBJECT = 3;

    /**
     * ..except one (selected randomly) which gets this many extra questions.
     */
    public static final int NO_EXTRA_QUESTIONS = 2;

    /**
     * Required min success-rate to pass a level-quiz.
     */
    public static final double ACCEPTABLE_QUIZ_SUCCESS_RATE = 0.8;

    public static final int NO_PASSED_LEVELS_BEFORE_EXERCISE_REMINDER = 4;

//    public static final int MIN_NO_EXERCISE_REMINDERS = 1;
//
//    public static final int MAX_NO_EXERCISE_REMINDERS = 3;
//
//    public static final int MIN_NO_QUIZ_CATEGORY_REMINDERS = 0;
//
//    public static final int MAX_NO_QUIZ_CATEGORY_REMINDERS = 2;

    public static final int MAX_NO_REMINDERS = 3;



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
        if (runningQuiz == null) return null;

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
     * Thrown if failed to create a quiz.
     */
    public static class QuizConstructionException extends RuntimeException {
        public QuizConstructionException() { super(); }
        public QuizConstructionException(String msg) { super(msg); }
    }

    /**
     * Set running-quiz to a new level-quiz (constructed from db quiz-data of exercise).
     * @param exerciseId
     * @param quizCategoryType
     * @param context
     * @return Level (in quiz-category) of new quiz.
     */
    public static int newLevelQuiz(long exerciseId, int quizCategoryType, Context context) throws QuizConstructionException {
        AppDatabase db = AppDatabase.getInstance(context);
        int runningQuizType = 0;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        Quiz quizData = ExerciseControl.loadNextLevelQuiz(exerciseId, quizCategoryType, db);
        if (quizData == null) throw new QuizConstructionException("Top level reached");
        List<GeoObject> levelGeoObjects = db.geoDao().loadWithQuiz(quizData.getId());

        newQuestions(levelGeoObjects, runningQuizId, db);

        return quizData.getLevel();
    }

    /**
     * Set running-quiz to a new follow-up-quiz (constructed from db running-quiz).
     * If no incorrectly answered questions exists in previous running-quiz, does nothing.
     * If previous running-quiz a follow-up, does nothing.
     *
     * @param context
     * @return True if new follow-up quiz started.
     * @pre A running-quiz in db.
     */
    public static void newFollowUpQuiz(Context context) throws QuizConstructionException {
        AppDatabase db = AppDatabase.getInstance(context);
        RunningQuiz runningQuiz = load(context);
        long runningQuizId = load(context).getId();
        List<Question> incorrectQuestions = db.questionDao()
                        .loadIncorrectNonPairItWithRunningQuizOrderedByIndex(runningQuizId);

        if (runningQuiz.getType() == RunningQuiz.FOLLOW_UP_QUIZ)
            throw new QuizConstructionException("Previous quiz a follow-up");
        if (incorrectQuestions.size() == 0)
            throw new QuizConstructionException("No incorrect answers in previous quiz");


        int runningQuizType = 1;
        runningQuizId = newRunningQuiz(runningQuizType, context);

        List<Question> newQuestions = new ArrayList<>();
        for (int i = 0; i < incorrectQuestions.size(); i++) {
            Question question = incorrectQuestions.get(i);
            question.setRunningQuizId(runningQuizId);
            question.setIndex(i);
            question.setAnsweredCorrectly(true);
            newQuestions.add(question);
        }
        db.questionDao().insert(newQuestions);
    }

    /**
     * Set running-quiz to a new level-reminder-quiz (generated semi-randomly
     * from geo-object-stats of exercise - geo-object from finished levels).
     *
     * @param exerciseId
     * @param quizCategoryType
     * @param context
     */
    public static boolean newLevelReminder(long exerciseId, int quizCategoryType, Context context) throws QuizConstructionException {
        AppDatabase db = AppDatabase.getInstance(context);
        int runningQuizType = 2;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        QuizCategory quizCategory = db.quizCategoryDao().loadWithExerciseAndType(exerciseId, quizCategoryType);
        List<Long> quizIds = db.quizDao().loadPassedIdsWithQuizCategory(quizCategory.getId());
        if (quizIds.size() == 0) throw new QuizConstructionException("No finished quizzes in quiz-category");

        List<Long> geoObjectCandidateIds = db.geoDao().loadIdsWithQuizIn(quizIds);
        List<GeoObject> geoObjects = pickReminderQuizGeoObjects(geoObjectCandidateIds, db);

        newQuestions(geoObjects, runningQuizId, db);
        return true;
    }

    /**
     * Set running-quiz to a new exercise-reminder-quiz (generated semi-randomly
     * from geo-object-stats of exercise).
     *
     * @param exerciseId
     * @param context
     */
    public static void newExerciseReminder(long exerciseId, Context context) throws QuizConstructionException {
        AppDatabase db = AppDatabase.getInstance(context);
        int runningQuizType = 3;
        long runningQuizId = newRunningQuiz(runningQuizType, context);

        List<Long> geoObjectCandidateIds = loadIdOfGeoObjectsInPassedQuizzesInExercise(exerciseId, db);
        if (geoObjectCandidateIds.size() == 0) throw new QuizConstructionException("No geo objects for reminder (no passes quizzes)");

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
        int noGeoObjects = Math.min(ExerciseControl.MAX_NO_GEO_OBJECTS_IN_A_LEVEL, geoObjectCandidateIds.size());

        //todo: select relevant objects

        List<GeoObject> selected = new ArrayList<>();

        for (int i = 0; i < noGeoObjects; i++) {
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
     * Not two same (geo-object and question-type) questions after another.
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

        int extraIndex = LocaUtils.randi(questionCounts.length); //TODO: smart pick
        questionCounts[extraIndex] += NO_EXTRA_QUESTIONS;

        int[] questionDifficulties = new int[geoObjects.size()];
        for (int i = 0; i < questionDifficulties.length; i++)
            questionDifficulties[i] = 0;

        int questionIndex = 0;
        List<Question> newQuestions = new ArrayList<>();

        while (!allIsZero(questionCounts)) {
            int i = LocaUtils.randi(geoObjects.size());
            if (questionCounts[i] > 0) {
                GeoObject geoObject = geoObjects.get(i);
                Question question = new Question(runningQuizId, geoObject, questionIndex, questionDifficulties[i], db);

                if (sameTextInAlternatives(question)) continue;
                if (newQuestions.size() > 0 && sameQuestionContent(newQuestions.get( newQuestions.size()-1 ), question)) continue;

                newQuestions.add(question);
                questionCounts[i] -= 1;
                questionIndex++;
                questionDifficulties[i]++;
            }
        }

        db.questionDao().insert(newQuestions);
    }

    /**
     * @param q1
     * @param q2
     * @return True if q1, q2 has same defining object and type.
     */
    private static boolean sameQuestionContent(Question q1, Question q2) {
        return q1.getGeoObjectId() == q2.getGeoObjectId() &&
                q1.getType() == q2.getType();
    }

    /**
     * @param question
     * @return True if it's a name-it or pair-it question with identical alternatives (button texts).
     */
    private static boolean sameTextInAlternatives(Question question) {
        if (question.getType() == Question.PLACE_IT) return false;

        List<String> buttonTexts = new ArrayList<>();
        for (GeoObject go : question.getContent()) buttonTexts.add(go.getName());
        return anyEqual(buttonTexts);
    }

    /**
     * @param strings
     * @return True if any equal to another.
     */
    private static boolean anyEqual(List<String> strings) {
        while (strings.size() > 1) {
            String str = strings.remove(0);
            if (strings.contains(str)) return true;
        }
        return false;
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

//    /**
//     * Report result of question in running quiz. Does nothing if question already reported.
//     * Don't report pair-it questions.
//     * Db-updates: Question (answered/correct) in running-quiz, and Geo-object (defining question) stats.
//     *
//     * @param question
//     * @param correct
//     * @param context
//     */
//    public static void reportQuestionResult(Question question, boolean correct, Context context) {
//        AppDatabase db = AppDatabase.getInstance(context);
//        Question dbQuestion = db.questionDao().load(question.getId());
//        if (dbQuestion.isAnswered()) return;
//
//        question.setAnswered(true);
//        if (correct) question.setAnsweredCorrectly(true);
//        else question.setAnsweredCorrectly(false);
//        db.questionDao().update(question);
//
//        RunningQuiz runningQuiz = load(context);
//        double askWeight = 1;
//        if (runningQuiz.getType() == RunningQuiz.FOLLOW_UP_QUIZ) askWeight *= 0.5;
//
//        GeoObject geoObject = loadGeoObjectFromQuestion(question, context);
//        geoObject.setTimesAsked( geoObject.getTimesAsked() + askWeight );
//        if (correct) {
//            geoObject.setNoCorrectAnswers(geoObject.getNoCorrectAnswers() + askWeight);
//            geoObject.setTimeOfPreviousCorrectAnswer(System.currentTimeMillis());
//        }
//
//        db.geoDao().update(geoObject);
//    }

    /**
     * Call after an answer to a name-it or pair-it question.
     * - Updates question (is-answered-correctly - for followup-quiz).
     * - Updates geo-object stats (for reminder-selection).
     *
     * If is-answered-correctly is false: Incorrect answer already provided: does nothing.
     *
     * @param contentIndex Index of answered object in question-content. -1 means no answer given.
     * @param context
     * @return True if correct answer.
     */
    public static boolean reportNameItPlaceItAnswer(int contentIndex, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        Question question = loadCurrentQuestion(context);
        if (question.getType() != Question.NAME_IT && question.getType() != Question.PLACE_IT) {
            throw new RuntimeException("Illegal call");
        }

        if (contentIndex == -1) {
            question.setAnsweredCorrectly(false);
            db.questionDao().update(question);
            return false;
        }

        boolean correctAnswer = checkNameItPlaceItAnswer(question, contentIndex);
        if (!question.isAnsweredCorrectly()) return correctAnswer;

        question.setAnsweredCorrectly(correctAnswer);
        db.questionDao().update(question);

        //report geo-obj stats
        RunningQuiz runningQuiz = load(context);
        double askWeight = 1;
        if (runningQuiz.getType() == RunningQuiz.FOLLOW_UP_QUIZ) askWeight *= 0.5;

        GeoObject geoObject = loadGeoObjectFromQuestion(question, context);
        geoObject.setTimesAsked( geoObject.getTimesAsked() + askWeight );
        if (question.isAnsweredCorrectly()) {
            geoObject.setNoCorrectAnswers(geoObject.getNoCorrectAnswers() + askWeight);
            geoObject.setTimeOfPreviousCorrectAnswer(System.currentTimeMillis());
        }
        db.geoDao().update(geoObject);

        return correctAnswer;
    }

    /**
     * @param question
     * @param contentIndex Answer
     * @return True if correct answer.
     */
    private static boolean checkNameItPlaceItAnswer(Question question, int contentIndex) {
        long correctId = question.getGeoObjectId();
        long answerId = question.getContent().get(contentIndex).getId();
        return correctId == answerId;
    }


    /**
     * Call after an incorrect answer to a pair-it question (possibly multiple times per question).
     * Downgrades answered-correctly-state of question.
     * (The correct-state's init-value is true.)
     *
     * No object-stats reported for pair-it.
     *
     * @param context
     */
    public static void reportIncorrectPairItAnswer(Context context) {
        Question question = loadCurrentQuestion(context);
        if (question.getType() != Question.PAIR_IT) {
            throw new RuntimeException("Illegal call");
        }

        question.setAnsweredCorrectly(false);
        AppDatabase.getInstance(context).questionDao().update(question);

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
     *
     * Level-quiz: If satisfactory result: Set passed and increment level, and set required reminders.
     * Follow-up: Update nothing.
     * Reminders: Decrement number of required reminders.
     *
     * @param context
     */
    public static void reportRunningQuizFinished(Context context) {
        Exercise exercise = SessionControl.loadExercise(context);

        AppDatabase db = AppDatabase.getInstance(context);
        RunningQuiz runningQuiz = load(context);
        Quiz quiz = loadQuizFromRunningQuiz(runningQuiz, context);
        QuizCategory quizCategory = loadQuizCategoryFromRunningQuiz(runningQuiz, context);

        List<Question> questions =
                db.questionDao().loadWithRunningQuiz(runningQuiz.getId());
        double successRate = successRate(questions);

        if (runningQuiz.getType() == RunningQuiz.LEVEL_QUIZ) {
            if (successRate >= ACCEPTABLE_QUIZ_SUCCESS_RATE) {
                reportLevelPassed(exercise, quiz, db);
                setNoRequiredReminders(exercise, quizCategory, context);
            }
        }
        else if (runningQuiz.getType() == RunningQuiz.QUIZ_CATEGORY_REMINDER) {
            decrementNoRequiredQuizCategoryReminders(quizCategory, db);
        }
        else if (runningQuiz.getType() == RunningQuiz.EXERCISE_REMINDER) {
            decrementNoRequiredExerciseReminders(exercise, db);
        }
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
    public static double successRate(List<Question> questions) {
        double correctCount = 0;
        int totCount = 0;

        for (Question question : questions) {
            if (question.getType() == Question.PAIR_IT) continue;

            totCount += 1;
            if (question.isAnsweredCorrectly())
                correctCount++;
        }

        return correctCount / totCount;
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

    //endregion

    //region Number of reminders

    /**
     * Derive and set number of required reminder quizzes for a quiz-category and exercise.
     * Called after level-quiz.
     *
     * Only updates n.o exercise-reminders if enough quizzes passed since last time required.
     * If new exercise-require: set n.o passed levels since exercise-reminder to zero.
     *
     * No reminders if progress 100% (of exercise/ quiz-category).
     *
     * @param exercise
     * @param quizCategory
     * @param context
     */
    private static void setNoRequiredReminders(Exercise exercise, QuizCategory quizCategory, Context context) {
        //todo: based on n.o problematic/ old words (prev quiz-result)
        AppDatabase db = AppDatabase.getInstance(context);

        if (ExerciseControl.progressOfExercise(exercise.getId(), db) == 100) return;

        if (exercise.getNoPassedLevelsSinceExerciseReminder() >= NO_PASSED_LEVELS_BEFORE_EXERCISE_REMINDER) {
            //int noExerciseReminders = relevantNumberOfExerciseReminders(exercise, db);
            int noExerciseReminders = 2;
            exercise.setNoRequiredExerciseReminders(noExerciseReminders);
            exercise.setNoPassedLevelsSinceExerciseReminder(0);
            db.exerciseDao().update(exercise);
        }

        int noQuizCategoryLevels = db.quizDao().countWithQuizCategory(quizCategory.getId());
        int noPassedQuizCategoryLevels = db.quizDao().countPassedWithQuizCategory(quizCategory.getId());
        if (noPassedQuizCategoryLevels == noQuizCategoryLevels) return;

        //int noQuizCategoryReminders = relevantNumberOfQuizCategoryReminders(quizCategory, db);
        int noQuizCategoryReminders = relevantNumberOfReminders(context);
        quizCategory.setNoRequiredReminders(noQuizCategoryReminders);
        db.quizCategoryDao().update(quizCategory);
    }

    /**
     * @param context
     * @return Relevant number of reminders based on prev quiz result.
     */
    private static int relevantNumberOfReminders(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        RunningQuiz runningQuiz = RunningQuizControl.load(context);
        List<Question> questions = db.questionDao().loadWithRunningQuiz(runningQuiz.getId());
        double successRate = RunningQuizControl.successRate(questions);

        if (successRate > 0.85) return 0;
        else return 1;
    }

//    /**
//     * Number of exercise-reminders, based on n.o problematic words in exercise.
//     * @param exercise
//     * @param db
//     * @return N.o suggested exercise-reminders.
//     */
//    private static int relevantNumberOfExerciseReminders(Exercise exercise, AppDatabase db) {
//        List<Long> passedQuizIds = ExerciseControl.loadPassedQuizIdsInExercise(exercise.getId(), db);
//        return noSuggestedReminders(passedQuizIds, db);
//
//        //return LocaUtils.randi(MIN_NO_EXERCISE_REMINDERS, MAX_NO_QUIZ_EXERCISE_REMINDERS + 1);
//    }
//
//    /**
//     * Number of quiz-category-reminders, based on n.o problematic words in quiz-category.
//     * @param quizCategory
//     * @param db
//     * @return N.o suggested quiz-category-reminders.
//     */
//    private static int relevantNumberOfQuizCategoryReminders(QuizCategory quizCategory, AppDatabase db) {
//        List<Long> passedQuizIds = db.quizDao().loadPassedIdsWithQuizCategory(quizCategory.getId());
//        return noSuggestedReminders(passedQuizIds, db);
//
//        //return LocaUtils.randi(MIN_NO_QUIZ_CATEGORY_REMINDERS, MAX_NO_QUIZ_CATEGORY_REMINDERS + 1);
//    }
//
//    /**
//     * @param passedQuizIds
//     * @param db
//     * @return Suggested no reminders based on n.o problematic objects in quizzes.
//     */
//    private static int noSuggestedReminders(List<Long> passedQuizIds, AppDatabase db) {
//        int noProblematicObjects = noProblematicObjects(passedQuizIds, db);
//        int noPossibleReminders = noProblematicObjects / ExerciseControl.MAX_NO_GEO_OBJECTS_IN_A_LEVEL;
//        return Math.min(LocaUtils.randi(noPossibleReminders + 1), MAX_NO_REMINDERS);
//    }
//
//    /**
//     * @param quizIds
//     * @param db
//     * @return Suggested n.o problematic geo-objects in quizzes.
//     */
//    private static int noProblematicObjects(List<Long> quizIds, AppDatabase db) {
//        Map<Long, Double> problemRatingsMap = ExerciseControl.problemRatings(quizIds, db);
//        List<Double> problemRatings = new ArrayList<>(problemRatingsMap.values());
//        return noProblematicRatings(problemRatings);
//    }
//
//    /**
//     * @param ratings
//     * @return N.o problematic ratings of the provided ones. Hard determining line between problematic/ not problematic.
//     */
//    private static int noProblematicRatings(List<Double> ratings) {
//        int HARD_LINE = 1;
//
//        int count = 0;
//        for (double rating : ratings) {
//            if (rating > HARD_LINE) count++;
//        }
//        return count;
//    }

    //endregion
}
