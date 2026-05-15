package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.CompetitionParticipant;
import java.util.List;

@Dao
public interface CompetitionParticipantDao {

    // Вставка связи участника с соревнованием
    @Insert
    long insert(CompetitionParticipant competitionParticipant);

    // Получить все связи для соревнования
    @Query("SELECT * FROM competition_participants WHERE competitionId = :competitionId ORDER BY sortOrder ASC")
    List<CompetitionParticipant> getByCompetition(int competitionId);

    // Получить связь по ID участника и соревнования
    @Query("SELECT * FROM competition_participants WHERE competitionId = :competitionId AND participantId = :participantId LIMIT 1")
    CompetitionParticipant getByCompetitionAndParticipant(int competitionId, int participantId);

    // Проверить, существует ли связь
    @Query("SELECT EXISTS(SELECT 1 FROM competition_participants WHERE competitionId = :competitionId AND participantId = :participantId)")
    boolean exists(int competitionId, int participantId);

    // Удалить связь по ID участника и соревнования
    @Query("DELETE FROM competition_participants WHERE competitionId = :competitionId AND participantId = :participantId")
    void deleteByCompetitionAndParticipant(int competitionId, int participantId);

    // Удалить все связи для соревнования
    @Query("DELETE FROM competition_participants WHERE competitionId = :competitionId")
    void deleteByCompetition(int competitionId);

    // Удалить все связи для участника
    @Query("DELETE FROM competition_participants WHERE participantId = :participantId")
    void deleteByParticipant(int participantId);

    // Обновить порядок сортировки
    @Query("UPDATE competition_participants SET sortOrder = :sortOrder WHERE competitionId = :competitionId AND participantId = :participantId")
    void updateSortOrder(int competitionId, int participantId, int sortOrder);

    // Получить максимальный порядок сортировки для соревнования
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM competition_participants WHERE competitionId = :competitionId")
    int getMaxSortOrder(int competitionId);
}