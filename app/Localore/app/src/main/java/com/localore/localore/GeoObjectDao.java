package com.localore.localore;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface GeoObjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(GeoObject... go);

    @Query("SELECT * FROM GeoObject")
    public List<GeoObject> loadAllGeoObjects();

    @Query("SELECT id FROM GeoObject")
    public List<Integer> loadAllGeoObjectIDs();

    @Query("SELECT * FROM GeoObject WHERE id LIKE :id")
    public GeoObject loadGeoObject(int id);
}
