package com.localore.localore.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

@Dao
public interface SessionDao {

    @Insert
    public void insert(Session session);

    @Update
    public void update(Session session);


    @Query("SELECT * FROM session")
    public Session load();
}
