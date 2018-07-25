package com.localore.localore;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Database(entities = {GeoObject.class, Exercise.class}, version = 1)
@TypeConverters({GeoObjectConverter.class, ExerciseConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Max allowed distance in meters between two geo-objects for
     * a merge.
     * */
    public static final double MERGE_LIMIT = 100;

    /**
     * Length based rank boost for geo-objects, by multiplying rank with [1, this].
     * */
    public static final double RANK_BOOST_FACTOR = 0.1;



    private static AppDatabase INSTANCE;
    private static String database_name = "localore-db";

    // allow main thread queries for now..
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, AppDatabase.class, database_name).allowMainThreadQueries().build();
        }
        return INSTANCE;
    }

    public abstract GeoObjectDao geoDao();
    public abstract ExerciseDao exerciseDao();



    /**
     * Attempt to merge object-pices into one object.
     * Same (similar) name + close proximity -> merge.
     *
     * @param exerciseId Merge objects belonging to this exercise.
     */
    public static void dedupeGeoObjects(long exerciseId, Context context) {
        AppDatabase tempDb = Room.databaseBuilder(context, AppDatabase.class, "temp_db").build();
        tempDb.clearAllTables();
        moveGeoObjects(INSTANCE, tempDb, exerciseId);

        while (tempDb.geoDao().size() > 0) {
            Log.d("_ME_", "size: " + tempDb.geoDao().size());

            GeoObject go = tempDb.geoDao().loadOne();
            tempDb.geoDao().delete(go);

            List<GeoObject> sameNames = deleteAllGeoObjectsWithSimilarName(go.getName(), tempDb);
            sameNames.add(go);
            List<GeoObject> merged = mergeMergables(sameNames);

            INSTANCE.geoDao().insert(merged);
        }

        tempDb.clearAllTables();
        tempDb.close();
    }

    /**
     * Move all geo-objects (of specified exercise) in src to dest (remove from src).
     * @param src
     * @param dest
     * @param exerciseId
     */
    private static void moveGeoObjects(AppDatabase src, AppDatabase dest, long exerciseId) {
        List<Long> ids = src.geoDao().loadIds(exerciseId);
        for (long id : ids) {
            GeoObject go = src.geoDao().load(id);
            src.geoDao().delete(go);
            dest.geoDao().insert(go);
        }
    }

    /**
     * Remove, from db, all geo-objects (no matter exercise etc) with specified (or similar) name.
     * @param name
     * @param db
     * @return Deleted geo-objects.
     */
    private static List<GeoObject> deleteAllGeoObjectsWithSimilarName(String name, AppDatabase db) {
        List<GeoObject> rmvs = db.geoDao().loadWithSimilarName(name);
        db.geoDao().delete(rmvs);
        return rmvs;

//        List<GeoObject> rmvs = new ArrayList<>();
//        List<Long> ids = db.geoDao().loadIds();
//
//        for (long id : ids) {
//            GeoObject go = db.geoDao().load(id);
//            if (go.getName().equalsIgnoreCase(name)) {
//                db.geoDao().delete(go);
//                rmvs.add(go);
//            }
//        }
//        return rmvs;
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
     * @pre o1, o2 has same (or similar) name.
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


    /**
     * Increase rank of long(/big) objects.
     *
     * @param exerciseId Merge objects belonging to this exercise.
     */
    public static void boostGeoObjectRanksByLength(long exerciseId) {
        double meanLength = meanGeoObjectLength(exerciseId);
        if (meanLength == 0) return;

        for (long id : INSTANCE.geoDao().loadIds(exerciseId)) {
            GeoObject g = INSTANCE.geoDao().load(id);

            double ratio = g.getLength() / meanLength * RANK_BOOST_FACTOR;
            g.setRank( g.getRank() * (1+ratio) );
            INSTANCE.geoDao().update(g);
        }
    }

    /**
     * @param exerciseId
     * @return Mean length of geo-objects of specified exercise.
     */
    private static double meanGeoObjectLength(long exerciseId) {
        double sum = 0;
        List<Long> geoObjectIds = INSTANCE.geoDao().loadIds(exerciseId);

        for (long id : geoObjectIds) {
            GeoObject g = INSTANCE.geoDao().load(id);
            sum += g.getLength();
        }

        return geoObjectIds.size() > 0 ? sum / geoObjectIds.size() : 0;
    }

}
