package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "competition_participants")
public class CompetitionParticipant {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int competitionId;
    public int participantId;
    public int sortOrder;

    // Пустой конструктор (обязателен для Room)
    public CompetitionParticipant() {
        this.sortOrder = 0;
    }

    // Конструктор для создания связи
    @Ignore
    public CompetitionParticipant(int competitionId, int participantId, int sortOrder) {
        this.competitionId = competitionId;
        this.participantId = participantId;
        this.sortOrder = sortOrder;
    }
}