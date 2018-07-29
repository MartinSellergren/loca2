package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

@Dao
public interface RunningQuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(RunningQuiz runningQuiz);

    @Delete
    public void delete(RunningQuiz runningQuiz);

    @Update
    public void update(RunningQuiz runningQuiz);

    @Query("SELECT * FROM RunningQuiz LIMIT 1")
    public RunningQuiz loadOne();
}
