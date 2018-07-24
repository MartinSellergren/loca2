package com.localore.localore;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(Exercise e);

    @Query("SELECT * FROM Exercise WHERE id LIKE :id")
    public Exercise loadExercise(long id);

    @Query ("SELECT * FROM Exercise")
    public List<Exercise> loadAllExercises();
}
