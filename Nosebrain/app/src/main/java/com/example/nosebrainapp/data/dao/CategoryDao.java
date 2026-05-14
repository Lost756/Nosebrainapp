package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.Category;
import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories WHERE competitionId = :competitionId ORDER BY sortOrder ASC, id ASC")
    List<Category> getByCompetition(int competitionId);

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getById(int id);

    @Insert
    long insert(Category category);

    @Update
    void update(Category category);
}