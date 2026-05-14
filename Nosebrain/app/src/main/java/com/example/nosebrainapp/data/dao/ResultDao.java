package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.Result;
import java.util.List;

@Dao
public interface ResultDao {
    @Query("SELECT * FROM results WHERE categoryId = :categoryId ORDER BY totalScore DESC, time ASC")
    List<Result> getByCategory(int categoryId);

    @Query("SELECT * FROM results WHERE participantId = :participantId AND categoryId = :categoryId LIMIT 1")
    Result getByParticipantAndCategory(int participantId, int categoryId);

    @Insert
    long insert(Result result);

    @Delete
    void delete(Result result);
}