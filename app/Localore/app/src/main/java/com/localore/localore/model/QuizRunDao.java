package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Update;

@Dao
public interface QuizRunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(QuizRun quizRun);

    @Delete
    public void delete(QuizRun quizRun);

    @Update
    public void update(QuizRun quizRun);
}
