package com.iot.team_1.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface IngredientDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIngredients(List<IngredientEntity> ingredients);

    @Query("SELECT name FROM ingredients")
    List<String> getAllIngredientNames();
}
