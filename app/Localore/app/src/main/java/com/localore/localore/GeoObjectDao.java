package com.localore.localore;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface GeoObjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(GeoObject... go);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(List<GeoObject> gos);

    @Update
    public void update(GeoObject geoObject);

    @Delete
    public void delete(GeoObject go);

    @Delete
    public void delete(List<GeoObject> gos);

    @Query("DELETE FROM GeoObject WHERE id IN (:ids)")
    public void deleteWithIds(List<Long> ids);

    @Query("SELECT * FROM GeoObject LIMIT 1")
    public GeoObject loadOne();

    @Query("SELECT * FROM GeoObject")
    public List<GeoObject> loadAll();

    @Query("SELECT * FROM GeoObject WHERE id LIKE :id")
    public GeoObject load(long id);

    @Query("SELECT * FROM GeoObject WHERE name = :name COLLATE NOCASE")
    public List<GeoObject> loadWithSimilarName(String name);

    @Query("SELECT id FROM GeoObject where exerciseId LIKE :exerciseId")
    public List<Long> loadIds(long exerciseId);

    @Query("SELECT id FROM GeoObject")
    public List<Long> loadIds();

    @Query("SELECT id FROM GeoObject")
    public List<Long> loadAllIds();

    @Query("SELECT count(*) FROM GeoObject")
    public long size();
}
