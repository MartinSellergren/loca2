package com.localore.localore.modelManipulation;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.localore.localore.CreateExerciseService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Static class for exercise related operations (manipulate the database).

 */
public class ExerciseControl {

    /**
     * Max allowed distance in meters between two geo-objects for a merge.
     */
    private static final double MERGE_LIMIT = 5;

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
     * Creates and inserts a new exercise-object into the db.
     *
     * @param exerciseName
     * @param workingArea
     * @param context
     *
     * @pre Session has a logged in user.
     * @pre exerciseName unique
     */
    public static long newExercise(String exerciseName, NodeShape workingArea, Context context) {
        long userId = SessionControl.load(context).getUserId();
        Exercise exercise = new Exercise(userId, exerciseName, workingArea);
        incrementExerciseDisplayIndexes(userId, context);
        long exerciseId = AppDatabase.getInstance(context).exerciseDao().insert(exercise);
        return exerciseId;
    }

    /**
     * Add 1 to display-indexes of exercises of user.
     * @param userId
     */
    private static void incrementExerciseDisplayIndexes(long userId, Context context) {
        List<Exercise> exercises = AppDatabase.getInstance(context).exerciseDao().loadWithUser(userId);
        for (Exercise exercise : exercises) {
            exercise.setDisplayIndex( exercise.getDisplayIndex() + 1 );
            AppDatabase.getInstance(context).exerciseDao().update(exercise);
        }
    }

    //region acquire

     /**
     * Fetches geo-objects in the working-area of exercise. Process raw OSM.
     * Updates temp-database with geo-objects. They all have quizId -1.
     *
     * @param workingArea Area containing objects.
     * @return True if database updated as planned. False means network error (etc?).
     */
    public static boolean acquireGeoObjects(NodeShape workingArea, Context context) {
        AppDatabase.getTempInstance(context).clearAllTables();
        JsonObject convTable = openConversionTable(context);
        GeoObjInstructionsIter iter = new GeoObjInstructionsIter(workingArea, context);
        iter.open();
        List<String> instr;

        while ((instr=iter.next()) != null) {
            try {
                GeoObject go = new GeoObject(instr, convTable);
                AppDatabase.getTempInstance(context).geoDao().insert(go);
            }
            catch (GeoObject.BuildException e) {
                Log.i("_ME_", "Can't build: " + e.toString());
            }
        }

        return true;
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
     * @pre Raw geo-objects in temp-database.
     */
    public static void postProcessing(long exerciseId, Context context) {
        List<Long> insertedIds = dedupeAndInsertGeoObjects(context);
        boostGeoObjectRanksByLength(insertedIds, context);
        insertGeoObjectQuizzes(exerciseId, context);
    }

    //region dedupe

    /**
     * Dedupe geo-objects in temp-db and insert into the main db.
     * Temp-db is emptied and closed.
     * Dedupe:
     * Attempt to merge object-pices into one object.
     * Same (similar) name + close proximity -> merge.
     *
     * @param context
     * @pre Raw geo-objects in temp-database.
     * @return IDs of the geo-objects inserted into the main-db.
     */
    public static List<Long> dedupeAndInsertGeoObjects(Context context) {
        List<Long> insertedIds = new ArrayList<>();

        while (AppDatabase.getTempInstance(context).geoDao().size() > 0) {
            Log.d("_ME_", "size: " + AppDatabase.getTempInstance(context).geoDao().size());

            GeoObject go = AppDatabase.getTempInstance(context).geoDao().loadOne();
            AppDatabase.getTempInstance(context).geoDao().delete(go);

            List<GeoObject> sameNames = deleteAllGeoObjectsWithSimilarNameInTempDb(go.getName(), context);
            sameNames.add(go);
            List<GeoObject> merged = mergeMergables(sameNames);

            List<Long> ids = AppDatabase.getInstance(context).geoDao().insert(merged);
            insertedIds.addAll(ids);
        }

        AppDatabase.getTempInstance(context).clearAllTables();
        AppDatabase.closeTemp();
        return insertedIds;
    }

    /**
     * Remove, from temp-db, all geo-objects with specified (or similar) name.
     * @param name
     * @param context
     * @return Deleted geo-objects.
     */
    private static List<GeoObject> deleteAllGeoObjectsWithSimilarNameInTempDb(String name, Context context) {
        List<GeoObject> rmvs = AppDatabase.getTempInstance(context).geoDao().loadWithSimilarName(name);
        AppDatabase.getTempInstance(context).geoDao().delete(rmvs);
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

        double d = minDistance(g1, g2);
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
//
//    /**
//     * @return Approximate distance in meters between g1 and g2.
//     */
//    private static double approximateDistance(GeoObject g1, GeoObject g2) {
//        double min = Double.POSITIVE_INFINITY;
//
//        double[] bs1 = g1.getBounds();
//        List<double[]> ns1 = new ArrayList<>();
//        ns1.add(new double[]{bs1[0], bs1[1]});
//        ns1.add(new double[]{bs1[0], bs1[3]});
//        ns1.add(new double[]{bs1[2], bs1[1]});
//        ns1.add(new double[]{bs1[2], bs1[3]});
//        ns1.add(g1.getCenter());
//
//        double[] bs2 = g2.getBounds();
//        List<double[]> ns2 = new ArrayList<>();
//        ns2.add(new double[]{bs2[0], bs2[1]});
//        ns2.add(new double[]{bs2[0], bs2[3]});
//        ns2.add(new double[]{bs2[2], bs2[1]});
//        ns2.add(new double[]{bs2[2], bs2[3]});
//        ns2.add(g2.getCenter());
//
//        for (double[] n1 : ns1) {
//            for (double[] n2 : ns2) {
//                double d = LocaUtils.distance(n1, n2);
//                if (d < min) min = d;
//            }
//        }
//        return min;
//    }

    //endregion

    //region boost ranks

    /**
     * In the main-db, increase rank of long/big objects.
     * @param ids IDs of geo-objects to process.
     */
    public static void boostGeoObjectRanksByLength(List<Long> ids, Context context) {
        double meanLength = meanGeoObjectLength(ids, context);
        if (meanLength == 0) return;

        for (long id : ids) {
            GeoObject g = AppDatabase.getInstance(context).geoDao().load(id);

            double ratio = g.getLength() / meanLength * RANK_BOOST_FACTOR;
            g.setRank( g.getRank() * (1+ratio) );
            AppDatabase.getInstance(context).geoDao().update(g);
        }
    }

    /**
     * @param ids IDs of concerned geo-objects in main-db.
     * @return Mean length of geo-objects.
     */
    private static double meanGeoObjectLength(List<Long> ids, Context context) {
        double sum = 0;

        for (long id : ids) {
            GeoObject g = AppDatabase.getInstance(context).geoDao().load(id);
            sum += g.getLength();
        }

        return ids.size() > 0 ? sum / ids.size() : 0;
    }

    //endregion

    //region insert quizzes

    /**
     * Creates quizzes with categories based on geo-objects without a quiz (-1) and inserts into db.
     * Also updates geo-object's quiz-references (none -1 afterwords).
     * @param exerciseId Parent exercise.
     * @param context
     */
    private static void insertGeoObjectQuizzes(long exerciseId, Context context) {
        for (int quizCategoryType = 0; quizCategoryType < QuizCategory.types.length; quizCategoryType++) {
            String supercat = QuizCategory.types[quizCategoryType];
            List<Long> ids = AppDatabase.getInstance(context).geoDao().
                    loadQuizlessIdsWithSupercatOrderdByRank(supercat);

            if (ids.size() == 0) continue;

            List<List<Long>> levelGroups = groupEquallySizedLevels(ids);
            QuizCategory quizCategory = new QuizCategory(exerciseId, quizCategoryType);
            long quizCategoryId = AppDatabase.getInstance(context).quizCategoryDao().insert(quizCategory);

            for (int level = 0; level < levelGroups.size(); level++) {
                Quiz quiz = new Quiz(quizCategoryId, level);
                long quizId = AppDatabase.getInstance(context).quizDao().insert(quiz);
                List<Long> goIds = levelGroups.get(level);
                setQuizIds(goIds, quizId, AppDatabase.getInstance(context).geoDao());
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
            int i = new Random().nextInt(noGroups);
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

    //endregion

    //region delete

    /**
     * @param exercise Exercise to be deleted from database (including underlying content).
     * @param context
     */
    public static void deleteExercise(Exercise exercise, Context context) {
        List<QuizCategory> quizCategories =
                AppDatabase.getInstance(context).quizCategoryDao()
                .loadWithExercise(exercise.getId());

        for (QuizCategory quizCategory : quizCategories)
            deleteQuizCategory(quizCategory, context);

        AppDatabase.getInstance(context).exerciseDao().delete(exercise);
        decrementExerciseDisplayIndexesAbove(exercise.getDisplayIndex(), exercise.getUserId(), context);
    }

    /**
     * Subtract 1 from display-indexes of exercises with current display-index strictly
     * above specified value, of specified user.
     *
     * @param displayIndex Update display-indexes strictly below this.
     * @param context
     */
    private static void decrementExerciseDisplayIndexesAbove(int displayIndex, long userId, Context context) {
        List<Exercise> exercises = AppDatabase.getInstance(context).exerciseDao().loadWithUser(userId);
        for (Exercise exercise : exercises) {
            if (exercise.getDisplayIndex() > displayIndex) {
                exercise.setDisplayIndex(exercise.getDisplayIndex() - 1);
                AppDatabase.getInstance(context).exerciseDao().update(exercise);
            }
        }
    }

    /**
     * @param quizCategory Quiz-category to be deleted from database (including underlying content).
     * @param context
     */
    public static void deleteQuizCategory(QuizCategory quizCategory, Context context) {
        List<Quiz> quizzes =
                AppDatabase.getInstance(context).quizDao()
                .loadWithQuizCategory(quizCategory.getId());

        for (Quiz quiz : quizzes)
            deleteQuiz(quiz, context);

        AppDatabase.getInstance(context).quizCategoryDao().delete(quizCategory);
    }


    /**
     * @param quiz Quiz to be deleted from database (including underlying content).
     * @param context
     */
    public static void deleteQuiz(Quiz quiz, Context context) {
        List<Long> geoObjectIds =
                AppDatabase.getInstance(context).geoDao()
                        .loadIdsWithQuiz(quiz.getId());

        for (long geoObjectId : geoObjectIds) {
            GeoObject geoObject = AppDatabase.getInstance(context).geoDao().load(geoObjectId);
            deleteGeoObject(geoObject, context);
        }

        AppDatabase.getInstance(context).quizDao().delete(quiz);
    }

    /**
     * @param geoObject Geo-object to be deleted.
     * @param context
     */
    public static void deleteGeoObject(GeoObject geoObject, Context context) {
        AppDatabase.getInstance(context).geoDao().delete(geoObject);
    }

    //endregion

    //region reorder

    //todo

    //endregion

    //region tapping

    //todo

    //endregion


    //region misc

    /**
     * @param exerciseId
     * @param context
     * @return Quizzes in exercise.
     */
    public static List<Quiz> loadQuizzesInExercise(long exerciseId, Context context) {
        List<Long> quizCategoryIds = AppDatabase.getInstance(context).quizCategoryDao()
                .loadIdsWithExercise(exerciseId);
        return AppDatabase.getInstance(context).quizDao()
                .loadWithQuizCategories(quizCategoryIds);
    }

    /**
     * @param exerciseId
     * @param context
     * @return Passed quizzes in exercise.
     */
    public static List<Quiz> loadPassedQuizzesInExercise(long exerciseId, Context context) {
        List<Long> quizCategoryIds = AppDatabase.getInstance(context).quizCategoryDao()
                .loadIdsWithExercise(exerciseId);
        return AppDatabase.getInstance(context).quizDao()
                .loadPassedWithQuizCategories(quizCategoryIds);
    }

    /**
     * @param context
     * @return List of exercise-names of session-user, ordered by display-index.
     *
     * @pre Session-user set.
     */
    public static List<String> exerciseNames(Context context) {
        long userId = SessionControl.load(context).getUserId();
        return AppDatabase.getInstance(context).exerciseDao()
                .loadNamesWithUserOrderedByDisplayIndex(userId);
    }

    /**
     * @param context
     * @return List of exercise-progresses of session-user, ordered by display-index.
     *
     * @pre Session-user set.
     */
    public static List<Integer> exerciseProgresses(Context context) {
        long userId = SessionControl.load(context).getUserId();
        List<Integer> progresses = new ArrayList<>();

        List<Long> exerciseIds = AppDatabase.getInstance(context).exerciseDao()
                .loadIdsWithUserOrderedByDisplayIndex(userId);
        for (Long exerciseId : exerciseIds)
            progresses.add(progressOfExercise(exerciseId, context));

        return progresses;
    }

    /**
     * @param exerciseId
     * @param context
     * @return Progress <= [0, 100] of exercise.
     */
    public static int progressOfExercise(long exerciseId, Context context) {
        float progress =
                (float)loadPassedQuizzesInExercise(exerciseId, context).size() /
                        loadQuizzesInExercise(exerciseId, context).size();
        return Math.round(progress);
    }


    //endregion
}
