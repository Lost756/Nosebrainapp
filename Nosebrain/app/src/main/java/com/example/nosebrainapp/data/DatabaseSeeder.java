package com.example.nosebrainapp.data;

import android.content.Context;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.dao.CompetitionParticipantDao;
import java.util.Arrays;
import java.util.List;

public class DatabaseSeeder {

    public static void seedIfEmpty(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        // Проверяем, есть ли уже данные
        List<Competition> existing = db.competitionDao().getActive();
        if (existing != null && !existing.isEmpty()) {
            return;
        }

        // Создаём тестовое соревнование
        Competition competition = new Competition();
        competition.name = "Nosework Cup 2026";
        competition.description = "Открытый кубок по ноузворку";
        competition.isActive = true;
        long competitionId = db.competitionDao().insert(competition);

        // Создаём категорию
        Category category = new Category();
        category.competitionId = (int) competitionId;
        category.name = "Начальный уровень";
        category.timeLimit = 120.0;
        category.hidesCount = 5;
        category.maxScore = 100;
        category.sortOrder = 0;
        long categoryId = db.categoryDao().insert(category);

        // Создаём правила штрафов
        List<Double> flatPoints = Arrays.asList(2.0, 3.0, 5.0);
        PenaltyRule flatRule = new PenaltyRule();
        flatRule.categoryId = (int) categoryId;
        flatRule.name = "Общий штраф";
        flatRule.type = "flat";
        flatRule.pointsJson = new com.google.gson.Gson().toJson(flatPoints);
        flatRule.sequenceIndex = 1;
        db.penaltyRuleDao().insert(flatRule);

        List<Double> progressivePoints = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        PenaltyRule progressiveRule = new PenaltyRule();
        progressiveRule.categoryId = (int) categoryId;
        progressiveRule.name = "Прогрессивный штраф";
        progressiveRule.type = "progressive";
        progressiveRule.pointsJson = new com.google.gson.Gson().toJson(progressivePoints);
        progressiveRule.sequenceIndex = 2;
        db.penaltyRuleDao().insert(progressiveRule);

        // Создаём тестовых участников (используем пустой конструктор и заполняем поля)
        Participant participant1 = new Participant();
        participant1.name = "Анна Смирнова";

        Participant participant2 = new Participant();
        participant2.name = "Дмитрий Иванов";
        participant2.nickname = "Барон";

        Participant participant3 = new Participant();
        participant3.name = "Елена Петрова";

        long p1 = db.participantDao().insert(participant1);
        long p2 = db.participantDao().insert(participant2);
        long p3 = db.participantDao().insert(participant3);

        // Добавляем участников в соревнование
        CompetitionParticipant cp1 = new CompetitionParticipant();
        cp1.competitionId = (int) competitionId;
        cp1.participantId = (int) p1;
        cp1.sortOrder = 1;

        CompetitionParticipant cp2 = new CompetitionParticipant();
        cp2.competitionId = (int) competitionId;
        cp2.participantId = (int) p2;
        cp2.sortOrder = 2;

        CompetitionParticipant cp3 = new CompetitionParticipant();
        cp3.competitionId = (int) competitionId;
        cp3.participantId = (int) p3;
        cp3.sortOrder = 3;

        db.competitionParticipantDao().insert(cp1);
        db.competitionParticipantDao().insert(cp2);
        db.competitionParticipantDao().insert(cp3);
    }
}