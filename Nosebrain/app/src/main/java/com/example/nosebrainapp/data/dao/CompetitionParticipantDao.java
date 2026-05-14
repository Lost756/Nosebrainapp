package com.example.nosebrainapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import com.example.nosebrainapp.data.entity.CompetitionParticipant;

@Dao
public interface CompetitionParticipantDao {
    @Insert
    long insert(CompetitionParticipant competitionParticipant);
}