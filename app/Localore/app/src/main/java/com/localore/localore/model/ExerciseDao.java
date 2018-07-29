package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(Exercise exercise);

    @Update
    public void update(Exercise exercise);

    @Delete
    public void delete(Exercise exercise);

    @Query("SELECT * FROM Exercise WHERE id = :id")
    public Exercise load(long id);

    @Query("SELECT * FROM Exercise WHERE displayIndex = :displayIndex")
    public Exercise loadWithDisplayIndex(long displayIndex);

    @Query("SELECT * FROM Exercise WHERE userId = :userId")
    public List<Exercise> loadWithUser(long userId);

    @Query("SELECT * FROM Exercise WHERE userId = :userId ORDER BY displayIndex")
    public List<Exercise> loadWithUserOrderdByDisplayIndex(long userId);

    @Query("SELECT * FROM Exercise")
    public List<Exercise> loadAll();

    @Query("SELECT count(*) FROM Exercise")
    public int size();
}
