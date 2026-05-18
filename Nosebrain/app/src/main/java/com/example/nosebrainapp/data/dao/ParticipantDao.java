package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.Participant;
import java.util.List;

@Dao
public interface ParticipantDao {

    @Query("SELECT * FROM participants ORDER BY name ASC")
    List<Participant> getAll();

    @Query("SELECT * FROM participants WHERE id = :id")
    Participant getById(int id);

    @Query("SELECT p.* FROM participants p " +
            "WHERE p.id NOT IN (SELECT DISTINCT r.participantId FROM results r " +
            "INNER JOIN categories c ON r.categoryId = c.id " +
            "WHERE c.competitionId = :competitionId) " +
            "ORDER BY p.name ASC")
    List<Participant> getAvailableForCompetition(int competitionId);

    @Query("SELECT p.* FROM participants p " +
            "INNER JOIN competition_participants cp ON cp.participantId = p.id " +
            "WHERE cp.competitionId = :competitionId " +
            "ORDER BY cp.sortOrder ASC, p.name ASC")
    List<Participant> getByCompetition(int competitionId);

    @Insert
    long insert(Participant participant);

    @Update
    void update(Participant participant);

    @Delete
    void delete(Participant participant);

    @Query("DELETE FROM participants WHERE id = :participantId")
    void deleteById(int participantId);
}