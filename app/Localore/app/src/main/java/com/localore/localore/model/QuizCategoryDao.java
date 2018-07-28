package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Update;

@Dao
public interface QuizCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(QuizCategory quizCategory);

    @Delete
    public void delete(QuizCategory quizCategory);

    @Update
    public void update(QuizCategory quizCategory);
}
