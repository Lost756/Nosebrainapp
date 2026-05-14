package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.Competition;
import java.util.List;

@Dao
public interface CompetitionDao {
    @Query("SELECT * FROM competitions ORDER BY id DESC")
    List<Competition> getAll();

    @Query("SELECT * FROM competitions WHERE isActive = 1 ORDER BY id DESC")
    List<Competition> getActive();

    @Query("SELECT * FROM competitions WHERE id = :id")
    Competition getById(int id);

    @Insert
    long insert(Competition competition);

    @Update
    void update(Competition competition);

    @Delete
    void delete(Competition competition);
}