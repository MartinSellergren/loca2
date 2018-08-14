package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class GeoObjectDao {

    private static int BATCH_SIZE = 500;

    // refined

    @Transaction
    public void deleteWithIdIn(List<Long> ids) {
        List<List<Long>> batches = Lists.partition(ids, BATCH_SIZE);
        for (List<Long> batch : batches) deleteWithIdIn_(batch);
    }

    @Transaction
    public List<GeoObject> loadWithIdIn(List<Long> ids) {
        List<List<Long>> batches = Lists.partition(ids, BATCH_SIZE);
        List<GeoObject> geoObjects = new ArrayList<>();
        for (List<Long> batch : batches) geoObjects.addAll(loadWithIdIn_(batch));
        return geoObjects;
    }

    // raw

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insert(GeoObject go);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract List<Long> insert(List<GeoObject> gos);

    @Update
    public abstract void update(GeoObject geoObject);

    @Delete
    public abstract void delete(GeoObject go);

    @Delete
    public abstract void delete(List<GeoObject> gos);

    @Query("DELETE FROM GeoObject WHERE id IN (:ids)")
    public abstract void deleteWithIdIn_(List<Long> ids);

    @Query("SELECT * FROM GeoObject LIMIT 1")
    public abstract GeoObject loadOne();

    @Query("SELECT * FROM GeoObject WHERE id = :id")
    public abstract GeoObject load(long id);

    @Transaction @Query("SELECT * FROM geoobject WHERE id IN (:ids)")
    public abstract List<GeoObject> loadWithIdIn_(List<Long> ids);

    @Transaction @Query("SELECT * FROM GeoObject WHERE name = :name COLLATE NOCASE")
    public abstract List<GeoObject> loadWithSimilarName(String name);

    @Transaction @Query("SELECT * FROM geoobject WHERE quizId = :quizId")
    public abstract List<GeoObject> loadWithQuiz(long quizId);

    @Transaction @Query("SELECT * FROM geoobject WHERE quizId IN (:quizIds)")
    public abstract List<GeoObject> loadWithQuizIn(List<Long> quizIds);

    @Transaction @Query("SELECT * FROM geoobject ORDER BY RANDOM() LIMIT :preferredCount")
    public abstract List<GeoObject> loadRandoms(int preferredCount);

    @Transaction @Query("SELECT * FROM geoobject where quizId IN (:quizIds) ORDER BY RANDOM() LIMIT :preferredCount")
    public abstract List<GeoObject> loadRandomsWithQuizIn(List<Long> quizIds, int preferredCount);

    @Transaction @Query("SELECT id FROM GeoObject WHERE quizId = :quizId")
    public abstract List<Long> loadIdsWithQuiz(long quizId);

    @Transaction @Query("SELECT id FROM GeoObject WHERE quizId = :quizId ORDER BY rank DESC")
    public abstract List<Long> loadIdsWithQuizOrderedByRank(long quizId);

    @Transaction @Query("SELECT id FROM geoobject WHERE quizId = :quizId")
    public abstract List<Long> loadAllWithQuiz(long quizId);

    @Transaction @Query("SELECT id FROM geoobject WHERE quizId IN (:quizIds)")
    public abstract List<Long> loadIdsWithQuizIn(List<Long> quizIds);

    @Transaction @Query("SELECT id FROM GeoObject WHERE quizId = -1 AND supercat = :supercat ORDER BY rank DESC")
    public abstract List<Long> loadQuizlessIdsWithSupercatOrderdByRank(String supercat);

    @Query("SELECT count(*) FROM GeoObject")
    public abstract int count();

    @Query("SELECT count(*) FROM geoobject WHERE quizId = :quizId")
    public abstract int countInQuiz(long quizId);

    //for testing
    @Transaction @Query("SELECT id FROM geoobject")
    public abstract List<Long> loadAllIds();
}