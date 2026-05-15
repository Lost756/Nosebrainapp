package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.Participant;
import java.util.List;

@Dao
public interface ParticipantDao {

    // Получить всех участников
    @Query("SELECT * FROM participants ORDER BY name ASC")
    List<Participant> getAll();

    // Получить участника по ID
    @Query("SELECT * FROM participants WHERE id = :id")
    Participant getById(int id);

    // Получить участников, доступных для соревнования (ещё не прошедших попытку в любой категории этого соревнования)
    @Query("SELECT p.* FROM participants p " +
            "WHERE p.id NOT IN (SELECT DISTINCT r.participantId FROM results r " +
            "INNER JOIN categories c ON r.categoryId = c.id " +
            "WHERE c.competitionId = :competitionId) " +
            "ORDER BY p.name ASC")
    List<Participant> getAvailableForCompetition(int competitionId);

    // Получить участников, привязанных к соревнованию
    @Query("SELECT p.* FROM participants p " +
            "INNER JOIN competition_participants cp ON cp.participantId = p.id " +
            "WHERE cp.competitionId = :competitionId " +
            "ORDER BY cp.sortOrder ASC, p.name ASC")
    List<Participant> getByCompetition(int competitionId);

    // Вставка участника
    @Insert
    long insert(Participant participant);

    // Обновление участника
    @Update
    void update(Participant participant);

    // Удаление участника
    @Delete
    void delete(Participant participant);

    // Удаление участника по ID
    @Query("DELETE FROM participants WHERE id = :participantId")
    void deleteById(int participantId);
}