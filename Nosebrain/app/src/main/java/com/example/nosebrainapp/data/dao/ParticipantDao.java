package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.Participant;
import java.util.List;

@Dao
public interface ParticipantDao {
    @Query("SELECT p.* FROM participants p " +
            "INNER JOIN competition_participants cp ON cp.participantId = p.id " +
            "WHERE cp.competitionId = :competitionId " +
            "AND p.id NOT IN (SELECT participantId FROM results WHERE categoryId IN " +
            "(SELECT id FROM categories WHERE competitionId = :competitionId)) " +
            "ORDER BY cp.sortOrder ASC, p.name ASC")
    List<Participant> getAvailableForCompetition(int competitionId);

    @Query("SELECT * FROM participants WHERE id = :id")
    Participant getById(int id);

    @Insert
    long insert(Participant participant);
}

// Также нужен SQL для таблицы competition_participants