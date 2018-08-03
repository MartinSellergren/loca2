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
public interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insert(Question question);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public List<Long> insert(List<Question> questions);

    @Delete
    public void delete(Question question);

    @Delete
    public void delete(List<Question> questions);

    @Update
    public void update(Question question);

    @Query("SELECT * FROM question WHERE runningQuizId = :runningQuizId AND `index` = :index")
    public Question loadWithRunningQuizAndIndex(long runningQuizId, int index);

    @Transaction @Query("SELECT * FROM question WHERE runningQuizId = :runningQuizId")
    public List<Question> loadWithRunningQuiz(long runningQuizId);

    @Transaction @Query("SELECT * FROM question WHERE runningQuizId = :runningQuizId ORDER BY `index`")
    public List<Question> loadWithRunningQuizOrderedByIndex(long runningQuizId);

//    @Transaction @Query("SELECT DISTINCT geoObjectId FROM question WHERE answeredCorrectly = 0 AND runningQuizId = :runningQuizId")
//    public List<Long> loadGeoObjectsIdIncorrectlyAnsweredWithRunningQuiz(long runningQuizId);

    @Transaction @Query("SELECT * FROM question WHERE answeredCorrectly = 0 AND runningQuizId = :runningQuizId ORDER BY `index`")
    public List<Question> loadIncorrectWithRunningQuizOrderedByIndex(long runningQuizId);

    @Transaction @Query("SELECT count(*) FROM question WHERE runningQuizId = :runningQuizId")
    public int countWithRunningQuiz(long runningQuizId);


    //for testing
    @Transaction @Query("SELECT * FROM question")
    public List<Question> loadAll();
}

