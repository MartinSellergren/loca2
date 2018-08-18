package com.localore.localore.modelManipulation;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.localore.localore.LocaUtils;
import com.localore.localore.R;
import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.GeoObjInstructionsIter;
import com.localore.localore.model.GeoObject;
import com.localore.localore.model.GeoObjectDao;
import com.localore.localore.model.NodeShape;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.QuizCategory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Static class for exercise related operations (manipulate the database).

 */
public class ExerciseControl {

    /**
     * An area containing less objects than this is not allowed for an exercise.
     */
    public static final int MIN_NO_GEO_OBJECTS_IN_AN_EXERCISE = 15;

    /**
     * Max allowed distance in meters between two geo-objects for a merge.
     */
    private static final double MERGE_LIMIT = 200;

    /**
     * Length based rank boost for geo-objects, by multiplying rank with [1, this].
     */
    private static final double RANK_BOOST_FACTOR = 0.1;

    /**
     * Max number of geo-objects in a level. If more: split into two levels.
     */
    public static final int MAX_NO_GEO_OBJECTS_IN_A_LEVEL = 5;

    //region create

    /**
     * Creates and inserts a new exercise-object into db.
     *
     * @param userId
     * @param exerciseName
     * @param workingArea
     * @param db
     *
     * @pre exerciseName unique
     */
    public static long newExercise(long userId, String exerciseName, NodeShape workingArea, AppDatabase db) {
        Exercise exercise = new Exercise(userId, exerciseName, workingArea);
        incrementExerciseDisplayIndexes(userId, db);
        long exerciseId = db.exerciseDao().insert(exercise);
        return exerciseId;
    }

    /**
     * Add 1 to display-indexes of exercises of user.
     * @param db
     */
    private static void incrementExerciseDisplayIndexes(long userId, AppDatabase db) {
        List<Exercise> exercises = db.exerciseDao().loadWithUser(userId);
        for (Exercise exercise : exercises)
            exercise.setDisplayIndex( exercise.getDisplayIndex() + 1 );

        db.exerciseDao().update(exercises);
    }

    //region acquire


    /**
     * If true: stops acquiring and exits with exception.
     */
    public static volatile boolean interruptAcquisition;


     /**
     * Fetches geo-objects in the working-area of exercise. Processes raw OSM.
     * Updates database with geo-objects. They all have quizId -1.
     *
     * @param workingArea Area containing objects.
     * @param tempDb Where geo-objects are inserted.
     * @param context For reading conversion-table from file.
     */
    public static void acquireGeoObjects(NodeShape workingArea, AppDatabase tempDb, Context context)  throws IOException, LocaUtils.WorkInterruptedException {
        interruptAcquisition = false;
        tempDb.clearAllTables();
        JsonObject convTable = openConversionTable(context);
        GeoObjInstructionsIter iter = new GeoObjInstructionsIter(workingArea, context);
        iter.open();
        List<String> instr;

        while ((instr=iter.next()) != null) {
            if (interruptAcquisition == true) {
                throw new LocaUtils.WorkInterruptedException();
            }

            try {
                GeoObject go = new GeoObject(instr, convTable);
                tempDb.geoDao().insert(go);
            }
            catch (GeoObject.BuildException e) {
                Log.i("<ME>", "Can't build: " + e.toString());
            }
        }
    }

    /**
     * Open table for conversion from tags to category.
     */
    private static JsonObject openConversionTable(Context context) {
        String json = LocaUtils.readTextFile(R.raw.tag_categories, context);
        return new JsonParser().parse(json).getAsJsonObject();
    }

    //endregion

    //region post process

    /**
     * @pre Data downloaded, raw geo-objects created and placed in temp-database.
     * @post Processed geo-objects are placed in main database, along with all other
     * exercise-related stuff.
     *
     * @param exerciseId Parent of created quizzes.
     * @param tempDb
     * @param mainDb
     * @return N.o geo-objects in the new exercise.
     */
    public static int postProcessing(long exerciseId, AppDatabase tempDb, AppDatabase mainDb) {
        List<Long> insertedIds = dedupeAndInsertGeoObjects(tempDb, mainDb);
        double maxRank = boostGeoObjectRanksByLength(insertedIds, mainDb);
        Exercise exercise = mainDb.exerciseDao().load(exerciseId);
        exercise.setMaxRankOfGeoObject(maxRank);
        mainDb.exerciseDao().update(exercise);

        insertGeoObjectQuizzes(exerciseId, mainDb);
        return insertedIds.size();
    }

    //region dedupe

    /**
     * Dedupe geo-objects in src-db and insert into dest-db.
     * Src-db is emptied and closed.
     * Dedupe:
     * Attempt to merge object-pieces into one object.
     * Same (similar) name + close proximity -> merge.
     *
     * @param src
     * @param dest
     * @pre Raw geo-objects in src
     * @return IDs of the geo-objects inserted into dest.
     */
    public static List<Long> dedupeAndInsertGeoObjects(AppDatabase src, AppDatabase dest) {
        List<Long> insertedIds = new ArrayList<>();

        while (src.geoDao().count() > 0) {
            Log.d("<ME>", "count: " + src.geoDao().count());

            GeoObject go = src.geoDao().loadOne();
            src.geoDao().delete(go);

            List<GeoObject> sameNames = deleteAllGeoObjectsWithSimilarName(go.getName(), src);
            sameNames.add(go);
            List<GeoObject> merged = mergeMergables(sameNames);

            List<Long> ids = dest.geoDao().insert(merged);
            insertedIds.addAll(ids);
        }

        src.clearAllTables();
        AppDatabase.closeTemp();
        return insertedIds;
    }

    /**
     * Remove all geo-objects with specified (or similar) name.
     * @param name
     * @param db
     * @return Deleted geo-objects.
     */
    private static List<GeoObject> deleteAllGeoObjectsWithSimilarName(String name, AppDatabase db) {
        List<GeoObject> rmvs = db.geoDao().loadWithSimilarName(name);
        db.geoDao().delete(rmvs);
        return rmvs;
    }

    /**
     * @param sameNames All have same (or similar) name.
     * @return List with merged (if possible) geo-objects.
     */
    private static List<GeoObject> mergeMergables(List<GeoObject> sameNames) {
        List<GeoObject> merged = new ArrayList<GeoObject>();

        while (sameNames.size() > 0) {
            GeoObject accum = sameNames.remove(0);
            GeoObject temp;

            while ((temp=mergeFirst(accum, sameNames)) != null) {
                accum = temp;
            }
            merged.add(accum);
        }
        return merged;
    }

    /**
     * Merges geo-object with one in list (first mergable one).
     * Also removes the merged object from the list.
     *
     * @param sameNames Geo-objects with same (or similar) names
     * as go.
     * @return Merge between go and first mergable from sameNames,
     * or NULL if no merge possible.
     * @pre All geo-objects has same (or similar names).
     */
    private static GeoObject mergeFirst(GeoObject go, List<GeoObject> sameNames) {
        for (GeoObject go2 : sameNames) {
            GeoObject goMerge = merge(go, go2);
            if (goMerge != null) {
                sameNames.remove(go2);
                return goMerge;
            }
        }
        return null;
    }

    /**
     * @return Merged object, or NULL if merge not possible.
     * @pre g1, g2 has same (or similar) name.
     */
    private static GeoObject merge(GeoObject g1, GeoObject g2) {
        if (!g1.getSupercat().equals(g2.getSupercat()) ||
                !g1.getSubcat().equals(g2.getSubcat()) ||
                g1.isNode() || g2.isNode())
            return null;

        double d = approximateDistance(g1, g2);
        if (d > MERGE_LIMIT)
            return null;

        if (g1.getRank() > g2.getRank()) {
            g1.addShapes(g2.getShapes());
            return g1;
        }
        else {
            g2.addShapes(g1.getShapes());
            return g2;
        }
    }


    /**
     * @return Distance in meters between g1 and g2's closest nodes.
     */
    private static double minDistance(GeoObject g1, GeoObject g2) {
        double min = Double.POSITIVE_INFINITY;
        for (double[] n1 : g1.getNodes()) {
            for (double[] n2 : g2.getNodes()) {
                double d = LocaUtils.distance(n1, n2);
                if (d < min) min = d;
            }
        }
        return min;
    }

    /**
     * @return Approximate distance in meters between g1 and g2.
     */
    private static double approximateDistance(GeoObject g1, GeoObject g2) {
        double min = Double.POSITIVE_INFINITY;

        double[] bs1 = g1.getBounds();
        List<double[]> ns1 = new ArrayList<>();
        ns1.add(new double[]{bs1[0], bs1[1]});
        ns1.add(new double[]{bs1[0], bs1[3]});
        ns1.add(new double[]{bs1[2], bs1[1]});
        ns1.add(new double[]{bs1[2], bs1[3]});
        ns1.add(g1.getCenter());

        double[] bs2 = g2.getBounds();
        List<double[]> ns2 = new ArrayList<>();
        ns2.add(new double[]{bs2[0], bs2[1]});
        ns2.add(new double[]{bs2[0], bs2[3]});
        ns2.add(new double[]{bs2[2], bs2[1]});
        ns2.add(new double[]{bs2[2], bs2[3]});
        ns2.add(g2.getCenter());

        for (double[] n1 : ns1) {
            for (double[] n2 : ns2) {
                double d = LocaUtils.distance(n1, n2);
                if (d < min) min = d;
            }
        }
        return min;
    }

    //endregion

    //region boost ranks

    /**
     * Increase rank of long/big geo-objects in db.
     * @param ids IDs of geo-objects to process.
     * @param db
     * @return maxRank
     */
    public static double boostGeoObjectRanksByLength(List<Long> ids, AppDatabase db) {
        double meanLength = meanGeoObjectLength(ids, db);
        if (meanLength == 0) return 0;

        double maxRank = -1;
        for (long id : ids) {
            GeoObject g = db.geoDao().load(id);

            double ratio = g.getLength() / meanLength * RANK_BOOST_FACTOR;
            double rank = g.getRank() * (1 + ratio);
            g.setRank(rank);
            db.geoDao().update(g);

            if (rank > maxRank) maxRank = rank;
        }

        return maxRank;
    }

    /**
     * @param ids IDs of concerned geo-objects.
     * @param db
     * @return Mean length of geo-objects in db.
     */
    private static double meanGeoObjectLength(List<Long> ids, AppDatabase db) {
        double sum = 0;

        for (long id : ids) {
            GeoObject g = db.geoDao().load(id);
            sum += g.getLength();
        }

        return ids.size() > 0 ? sum / ids.size() : 0;
    }

    //endregion

    //region insert quizzes

    /**
     * Creates quizzes with categories based on geo-objects without a quiz (-1) and inserts into db.
     * Also updates geo-object's quiz-references (none -1 afterwords).
     *
     * @param exerciseId Parent exercise.
     * @param db
     */
    private static void insertGeoObjectQuizzes(long exerciseId, AppDatabase db) {
        for (int quizCategoryType = 0; quizCategoryType < QuizCategory.TYPES.length; quizCategoryType++) {
            String supercat = QuizCategory.TYPES[quizCategoryType];
            List<Long> ids = db.geoDao().loadQuizlessIdsWithSupercatOrderdByRank(supercat);

            if (ids.size() == 0) continue;

            List<List<Long>> levelGroups = groupEquallySizedLevels(ids);
            QuizCategory quizCategory = new QuizCategory(exerciseId, quizCategoryType);
            long quizCategoryId = db.quizCategoryDao().insert(quizCategory);

            for (int level = 0; level < levelGroups.size(); level++) {
                Quiz quiz = new Quiz(quizCategoryId, level);
                long quizId = db.quizDao().insert(quiz);
                List<Long> goIds = levelGroups.get(level);
                setQuizIds(goIds, quizId, db.geoDao());
            }
        }
    }

    /**
     * Group geo-objects into levels. Attempt equally sized groups, obeying defined constraints.
     *
     * @param goIds Geo-object ids sorted by rank.
     * @return Geo-objects grouped into levels. Equally sized groups if possible.
     */
    public static List<List<Long>> groupEquallySizedLevels(List<Long> goIds) {
        if (goIds.size() == 0) return new ArrayList();

        int noGroups = goIds.size() / MAX_NO_GEO_OBJECTS_IN_A_LEVEL +
                (goIds.size() % MAX_NO_GEO_OBJECTS_IN_A_LEVEL == 0 ? 0 : 1);

        int groupSize = goIds.size() / noGroups;
        int[] groupSizes = new int[noGroups];
        for (int i = 0; i < noGroups; i++) groupSizes[i] = groupSize;

        int noGroupsWithOneExtra = goIds.size() % noGroups;
        while (noGroupsWithOneExtra > 0) {
            int i = LocaUtils.randi(noGroups);
            if (groupSizes[i] == groupSize) {
                groupSizes[i]++;
                noGroupsWithOneExtra--;
            }
        }

        List<List<Long>> groups = new ArrayList<>();
        int i0 = 0;

        for (int size : groupSizes) {
            groups.add(goIds.subList(i0, i0+size));
            i0 = i0 + size;
        }

        return groups;
    }

    /**
     * Set and update quiz-id of specified geo-objects.
     * @param goIds
     * @param quizId
     * @param geoDao
     */
    private static void setQuizIds(List<Long> goIds, long quizId, GeoObjectDao geoDao) {
        for (long goId : goIds) {
            GeoObject go = geoDao.load(goId);
            go.setQuizId(quizId);
            geoDao.update(go);
        }
    }

    //endregion

    //endregion

    //region wipe construction

    /**
     * Undo db-changes made by acquisition-process:
     * - Exercise under construction (and everything below).
     * - Construction-junk.
     *
     * @param exerciseId
     * @param context
     */
    public static void wipeConstruction(long exerciseId, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        Exercise exercise = db.exerciseDao().load(exerciseId);
        if (exercise != null)
            ExerciseControl.deleteExercise(exercise, db);

        wipeConstructionJunk(context);
    }

    /**
     * Wipe temp-construction-data:
     *  - Everything in the temp-database.
     *  - Geo-objects with -1 quiz-reference (which new geo-objects have at a point during post-processing).
     *
     * @param context
     */
    public static void wipeConstructionJunk(Context context) {
        AppDatabase.getTempInstance(context).clearAllTables();

        AppDatabase db = AppDatabase.getInstance(context);
        List<Long> junkGeoObjects = db.geoDao().loadAllWithQuiz(-1);
        db.geoDao().deleteWithIdIn(junkGeoObjects);
    }

    //endregion

    //endregion

    //region delete

    /**
     * @param exercise Exercise to be deleted from database (including underlying content).
     * @param db
     */
    public static void deleteExercise(Exercise exercise, AppDatabase db) {
        List<QuizCategory> quizCategories = db.quizCategoryDao()
                .loadWithExercise(exercise.getId());

        for (QuizCategory quizCategory : quizCategories)
            deleteQuizCategory(quizCategory, db);

        db.exerciseDao().delete(exercise);
        decrementExerciseDisplayIndexesAbove(exercise.getDisplayIndex(), exercise.getUserId(), db);
    }

    /**
     * Subtract 1 from display-indexes of exercises with current display-index strictly
     * above specified value, of specified user.
     *
     * @param displayIndex Update display-indexes strictly below this.
     * @param db
     */
    private static void decrementExerciseDisplayIndexesAbove(int displayIndex, long userId, AppDatabase db) {
        List<Exercise> exercises = db.exerciseDao().loadWithUser(userId);
        for (Exercise exercise : exercises) {
            if (exercise.getDisplayIndex() > displayIndex) {
                exercise.setDisplayIndex(exercise.getDisplayIndex() - 1);
            }
        }
        db.exerciseDao().update(exercises);
    }

    /**
     * @param quizCategory Quiz-category to be deleted from database (including underlying content).
     * @param db
     */
    public static void deleteQuizCategory(QuizCategory quizCategory, AppDatabase db) {
        List<Quiz> quizzes = db.quizDao()
                .loadWithQuizCategory(quizCategory.getId());

        for (Quiz quiz : quizzes) deleteQuiz(quiz, db);

        db.quizCategoryDao().delete(quizCategory);
    }


    /**
     * @param quiz Quiz to be deleted from database (including underlying content).
     * @param db
     */
    public static void deleteQuiz(Quiz quiz, AppDatabase db) {
        List<Long> delGeoObjectIds = db.geoDao().loadIdsWithQuiz(quiz.getId());
        db.geoDao().deleteWithIdIn(delGeoObjectIds);
        db.quizDao().delete(quiz);
    }

    //endregion

    //region reorder

    public static void swapOrder(Exercise e1, Exercise e2, AppDatabase db) {
        int temp = e1.getDisplayIndex();
        e1.setDisplayIndex(e2.getDisplayIndex());
        e2.setDisplayIndex(temp);
        db.exerciseDao().update(e1);
        db.exerciseDao().update(e2);
    }

    //endregion

    //region tapping

    /**
     * Load and return geo-objects for a tapping-session.
     *
     * @param exerciseId
     * @param quizCategoryType
     * @param loadNextLevel Geo-objects in next level, vs. past levels.
     * @param db
     * @return Tapping-session geo-objects.
     */
    public static List<GeoObject> loadGeoObjectsForTapping(long exerciseId, int quizCategoryType, boolean loadNextLevel, AppDatabase db) {
        Quiz nextQuiz = loadNextLevelQuiz(exerciseId, quizCategoryType, db);

        if (loadNextLevel) {
            return db.geoDao().loadWithQuiz(nextQuiz.getId());
        }
        else {
            QuizCategory quizCategory = db.quizCategoryDao()
                    .loadWithExerciseAndType(exerciseId, quizCategoryType);
            List<Long> idsOfPastLevels = db.quizDao()
                    .loadIdsWithLevelBelowAndQuizCategory(nextQuiz.getLevel(), quizCategory.getId());
            return db.geoDao().loadWithQuizIn(idsOfPastLevels);
        }
    }

    //endregion

    //region misc

    /**
     * @param exerciseId
     * @param db
     * @return Quizzes in exercise.
     */
    public static List<Quiz> loadQuizzesInExercise(long exerciseId, AppDatabase db) {
        List<Long> quizCategoryIds = db.quizCategoryDao().loadIdsWithExercise(exerciseId);
        return db.quizDao().loadWithQuizCategoryIn(quizCategoryIds);
    }

    /**
     * @param exerciseId
     * @param db
     * @return Quizzes in exercise.
     */
    public static List<Long> loadQuizIdsInExercise(long exerciseId, AppDatabase db) {
        List<Long> quizCategoryIds = db.quizCategoryDao().loadIdsWithExercise(exerciseId);
        return db.quizDao().loadIdsWithQuizCategoryIn(quizCategoryIds);
    }

    /**
     * @param exerciseId
     * @param db
     * @return Passed quizzes in exercise.
     */
    public static List<Quiz> loadPassedQuizzesInExercise(long exerciseId, AppDatabase db) {
        List<Long> quizCategoryIds = db.quizCategoryDao().loadIdsWithExercise(exerciseId);
        return db.quizDao().loadPassedWithQuizCategories(quizCategoryIds);
    }

    /**
     * @param userId
     * @param db
     * @return List of exercise-progresses of a user, ordered by display-index.
     */
    public static List<Integer> exerciseProgresses(long userId, AppDatabase db) {
        List<Integer> progresses = new ArrayList<>();

        List<Long> exerciseIds = db.exerciseDao()
                .loadIdsWithUserOrderedByDisplayIndex(userId);
        for (Long exerciseId : exerciseIds)
            progresses.add(progressOfExercise(exerciseId, db));

        return progresses;
    }

    /**
     * @param exerciseId
     * @param db
     * @return Progress <= [0, 100] of exercise.
     */
    public static int progressOfExercise(long exerciseId, AppDatabase db) {
        float progress =
                (float)loadPassedQuizzesInExercise(exerciseId, db).size() /
                        loadQuizzesInExercise(exerciseId, db).size();
        return Math.round(progress * 100);
    }

    /**
     * @param exerciseId
     * @param db
     * @return For each quiz-category in exercise (ordered by type):
     *         0. Type
     *         1. Number of levels
     *         2. number of passed levels
     *         3. number of required quiz-category-reminders]
     */
    public static List<int[]> loadQuizCategoriesData(long exerciseId, AppDatabase db) {
        List<int[]> data = new ArrayList<>();
        List<QuizCategory> quizCategories =
                db.quizCategoryDao()
                        .loadWithExerciseOrderedByType(exerciseId);

        for (QuizCategory quizCategory : quizCategories) {
            int type = quizCategory.getType();
            int noLevels = db.quizDao()
                    .countWithQuizCategory(quizCategory.getId());
            int noPassedLevels = db.quizDao()
                    .countPassedWithQuizCategory(quizCategory.getId());
            int noReminders = quizCategory.getNoRequiredReminders();

            data.add(new int[]{type, noLevels, noPassedLevels, noReminders});
        }

        return data;
    }

    /**
     * Load next level-quiz of specified quiz-category in specified exercise
     * (i.e quiz with lowest level not yet done).
     * @param exerciseId
     * @param quizCategoryType
     * @param db
     * @return Next level-quiz of quiz-category, or NULL.
     */
    public static Quiz loadNextLevelQuiz(long exerciseId, int quizCategoryType, AppDatabase db) {
        QuizCategory quizCategory = db.quizCategoryDao()
                .loadWithExerciseAndType(exerciseId, quizCategoryType);
        if (quizCategory == null) return null;

        return db.quizDao()
                .loadLowestLevelNotYetDoneInQuizCategory(quizCategory.getId());
    }

    /**
     * @param geoObject
     * @param context
     * @return Geo-object's quiz-category.
     */
    public static QuizCategory loadQuizCategoryOfGeoObject(GeoObject geoObject, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        Quiz quiz = db.quizDao().load(geoObject.getQuizId());
        return db.quizCategoryDao().load(quiz.getQuizCategoryId());
    }

    //endregion
}
