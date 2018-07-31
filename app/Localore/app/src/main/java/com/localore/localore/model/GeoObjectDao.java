package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface GeoObjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(GeoObject go);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public List<Long> insert(List<GeoObject> gos);

    @Update
    public void update(GeoObject geoObject);

    @Delete
    public void delete(GeoObject go);

    @Delete
    public void delete(List<GeoObject> gos);

    @Query("DELETE FROM GeoObject WHERE id IN (:ids)")
    public void deleteWithIds(List<Long> ids);

    @Query("SELECT * FROM GeoObject LIMIT 1")
    public GeoObject loadOne();

    @Query("SELECT * FROM GeoObject WHERE id = :id")
    public GeoObject load(long id);

    @Transaction @Query("SELECT * FROM geoobject WHERE id IN (:ids)")
    public List<GeoObject> load(List<Long> ids);

    @Transaction @Query("SELECT * FROM GeoObject WHERE name = :name COLLATE NOCASE")
    public List<GeoObject> loadWithSimilarName(String name);

    @Transaction @Query("SELECT * FROM geoobject WHERE quizId = :quizId")
    public List<GeoObject> loadWithQuiz(long quizId);

    @Transaction @Query("SELECT * FROM geoobject ORDER BY RANDOM() LIMIT :preferredCount")
    public List<GeoObject> loadRandoms(int preferredCount);

    @Transaction @Query("SELECT id FROM GeoObject WHERE quizId = :quizId")
    public List<Long> loadIdsWithQuiz(long quizId);

    @Transaction @Query("SELECT id FROM GeoObject WHERE quizId = :quizId ORDER BY rank")
    public List<Long> loadIdsWithQuizOrderedByRank(long quizId);

    @Transaction @Query("SELECT id FROM GeoObject WHERE quizId = -1 AND supercat = :supercat ORDER BY rank")
    public List<Long> loadQuizlessIdsWithSupercatOrderdByRank(String supercat);

    @Transaction @Query("SELECT id FROM geoobject WHERE quizId IN (:quizIds)")
    public List<Long> loadIdsWithQuizIn(List<Long> quizIds);

    @Query("SELECT count(*) FROM GeoObject")
    public int size();

    //for testing
    @Transaction @Query("SELECT id FROM geoobject")
    public List<Long> loadAllIds();
}