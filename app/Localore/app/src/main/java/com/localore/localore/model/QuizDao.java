package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(Quiz quiz);

    @Delete
    public void delete(Quiz quiz);

    @Update
    public void update(Quiz quiz);

    @Query("SELECT * FROM Quiz WHERE quizCategoryId = :quizCategoryId")
    public List<Quiz> loadWithQuizCategory(long quizCategoryId);

    @Query("SELECT * FROM Quiz WHERE quizCategoryId = :quizCategoryId ORDER BY level")
    public List<Quiz> loadWithQuizCategoryOrderedByLevel(long quizCategoryId);
}
