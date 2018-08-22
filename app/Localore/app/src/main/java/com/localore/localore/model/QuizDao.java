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
public interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(Quiz quiz);

    @Delete
    public void delete(Quiz quiz);

    @Update
    public void update(Quiz quiz);

    @Query("SELECT * FROM quiz WHERE id = :id")
    public Quiz load(long id);

    @Query("SELECT * FROM quiz WHERE isPassed = 0 AND quizCategoryId = :quizCategoryId ORDER BY level LIMIT 1")
    public Quiz loadLowestLevelNotYetDoneInQuizCategory(long quizCategoryId);

    @Transaction @Query("SELECT * FROM quiz WHERE quizCategoryId = :quizCategoryId")
    public List<Quiz> loadWithQuizCategory(long quizCategoryId);

    @Transaction @Query("SELECT * FROM quiz WHERE quizCategoryId = :quizCategoryId ORDER BY level")
    public List<Quiz> loadWithQuizCategoryOrderedByLevel(long quizCategoryId);

    @Transaction @Query("SELECT * FROM quiz WHERE quizCategoryId IN (:quizCategoryIds)")
    public List<Quiz> loadWithQuizCategoryIn(List<Long> quizCategoryIds);

    @Transaction @Query("SELECT id FROM quiz WHERE quizCategoryId == :quizCategoryId")
    public List<Long> loadIdsWithQuizCategory(Long quizCategoryId);

    @Transaction @Query("SELECT id FROM quiz WHERE quizCategoryId IN (:quizCategoryIds)")
    public List<Long> loadIdsWithQuizCategoryIn(List<Long> quizCategoryIds);

    @Transaction @Query("SELECT * FROM quiz WHERE isPassed = 1 AND quizCategoryId IN (:quizCategoryIds)")
    public List<Quiz> loadPassedWithQuizCategories(List<Long> quizCategoryIds);

    @Transaction @Query("SELECT id FROM quiz WHERE isPassed = 1 AND quizCategoryId = :quizCategoryId")
    public List<Long> loadPassedIdsWithQuizCategory(long quizCategoryId);

    @Transaction @Query("SELECT id FROM quiz WHERE isPassed = 1 AND quizCategoryId IN (:quizCategoryIds)")
    public List<Long> loadPassedIdsWithQuizCategoryIn(List<Long> quizCategoryIds);

    @Transaction @Query("SELECT id FROM quiz WHERE level < :level AND quizCategoryId = :quizCategoryId")
    public List<Long> loadIdsWithLevelBelowAndQuizCategory(int level, long quizCategoryId);

    @Query("SELECT COUNT(*) FROM quiz WHERE quizCategoryId = :quizCategoryId")
    public int countWithQuizCategory(long quizCategoryId);

    @Query("SELECT COUNT(*) FROM quiz WHERE isPassed = 1 AND quizCategoryId = :quizCategoryId")
    public int countPassedWithQuizCategory(long quizCategoryId);
}
