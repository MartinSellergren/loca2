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

    @Query("SELECT * FROM quiz WHERE id = :id")
    public Quiz load(long id);

    @Query("SELECT * FROM quiz WHERE quizCategoryId = :quizCategoryId")
    public List<Quiz> loadWithQuizCategory(long quizCategoryId);

    @Query("SELECT * FROM quiz WHERE quizCategoryId = :quizCategoryId ORDER BY level")
    public List<Quiz> loadWithQuizCategoryOrderedByLevel(long quizCategoryId);

    @Query("SELECT * FROM quiz WHERE quizCategoryId IN (:quizCategoryIds)")
    public List<Quiz> loadWithQuizCategories(List<Long> quizCategoryIds);

    @Query("SELECT * FROM quiz WHERE isPassed = 1 AND quizCategoryId IN (:quizCategoryIds)")
    public List<Quiz> loadPassedWithQuizCategories(List<Long> quizCategoryIds);

    @Query("SELECT id FROM quiz WHERE isPassed = 1 AND quizCategoryId = :quizCategoryId")
    public List<Long> loadPassedIdsWithQuizCategory(long quizCategoryId);

    @Query("SELECT id FROM quiz WHERE isPassed = 1 AND quizCategoryId IN (:quizCategoryIds)")
    public List<Long> loadPassedIdsWithQuizCategoryIn(List<Long> quizCategoryIds);

    @Query("SELECT COUNT(*) FROM quiz WHERE quizCategoryId = :quizCategoryId")
    public int countWithQuizCategory(long quizCategoryId);

    @Query("SELECT COUNT(*) FROM quiz WHERE isPassed = 1 AND quizCategoryId = :quizCategoryId")
    public int countPassedWithQuizCategory(long quizCategoryId);
}
