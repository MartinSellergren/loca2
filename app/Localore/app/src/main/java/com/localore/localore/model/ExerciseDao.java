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
public interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(Exercise exercise);

    @Update
    public void update(Exercise exercise);

    @Update
    public void update(List<Exercise> exercises);

    @Delete
    public void delete(Exercise exercise);

    @Query("SELECT * FROM Exercise WHERE id = :id")
    public Exercise load(long id);

    @Query("SELECT * FROM Exercise WHERE displayIndex = :displayIndex")
    public Exercise loadWithDisplayIndex(long displayIndex);

    @Transaction @Query("SELECT * FROM exercise WHERE userId = :userId")
    public List<Exercise> loadWithUser(long userId);

    @Transaction @Query("SELECT * FROM exercise WHERE userId = :userId ORDER BY displayIndex")
    public List<Exercise> loadWithUserOrderedByDisplayIndex(long userId);

    @Transaction @Query("SELECT * FROM exercise")
    public List<Exercise> loadAll();

    @Transaction @Query("SELECT name FROM exercise WHERE userId = :userId")
    public List<String> loadNamesWithUser(long userId);

    @Transaction @Query("SELECT id FROM exercise WHERE userId = :userId ORDER BY displayIndex")
    public List<Long> loadIdsWithUserOrderedByDisplayIndex(long userId);
}
