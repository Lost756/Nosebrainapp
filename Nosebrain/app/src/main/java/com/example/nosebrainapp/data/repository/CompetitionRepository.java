package com.example.nosebrainapp.data.repository;

import android.content.Context;
import com.example.nosebrainapp.data.AppDatabase;
import com.example.nosebrainapp.data.entity.*;
import java.util.List;

public class CompetitionRepository {

    private AppDatabase db;

    public CompetitionRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    // ==================== КОНСТРУКТОР ====================

    public CompetitionRepository(AppDatabase db) {
        this.db = db;
    }

    // ==================== СОРЕВНОВАНИЯ (COMPETITION) ====================

    public List<Competition> getCompetitions() {
        return db.competitionDao().getActive();
    }

    public List<Competition> getAllCompetitions() {
        return db.competitionDao().getAll();
    }

    public Competition getCompetitionById(int competitionId) {
        return db.competitionDao().getById(competitionId);
    }

    public long insertCompetition(Competition competition) {
        return db.competitionDao().insert(competition);
    }

    public void updateCompetition(Competition competition) {
        db.competitionDao().update(competition);
    }

    public void deleteCompetition(Competition competition) {
        db.competitionDao().delete(competition);
    }

    public void deleteCompetitionById(int competitionId) {
        Competition competition = db.competitionDao().getById(competitionId);
        if (competition != null) {
            db.competitionDao().delete(competition);
        }
    }

    // ==================== КАТЕГОРИИ (CATEGORY) ====================

    public List<Category> getCategoriesByCompetition(int competitionId) {
        return db.categoryDao().getByCompetition(competitionId);
    }

    public Category getCategoryById(int categoryId) {
        return db.categoryDao().getById(categoryId);
    }

    public long insertCategory(Category category) {
        return db.categoryDao().insert(category);
    }

    public void updateCategory(Category category) {
        db.categoryDao().update(category);
    }

    public void deleteCategoryById(int categoryId) {
        db.categoryDao().deleteById(categoryId);
    }

    // ==================== УЧАСТНИКИ (PARTICIPANT) ====================

    public List<Participant> getAllParticipants() {
        return db.participantDao().getAll();
    }

    public Participant getParticipantById(int participantId) {
        return db.participantDao().getById(participantId);
    }

    public long insertParticipant(Participant participant) {
        return db.participantDao().insert(participant);
    }

    public void updateParticipant(Participant participant) {
        db.participantDao().update(participant);
    }

    public void deleteParticipant(Participant participant) {
        db.participantDao().delete(participant);
    }

    public void deleteParticipantById(int participantId) {
        Participant participant = db.participantDao().getById(participantId);
        if (participant != null) {
            db.participantDao().delete(participant);
        }
    }

    // Получение участников, доступных для соревнования (ещё не прошедших попытку)
    public List<Participant> getAvailableParticipants(int competitionId) {
        return db.participantDao().getAvailableForCompetition(competitionId);
    }

    // ==================== СВЯЗЬ УЧАСТНИКОВ С СОРЕВНОВАНИЕМ (COMPETITION_PARTICIPANT) ====================

    public void addParticipantToCompetition(CompetitionParticipant link) {
        db.competitionParticipantDao().insert(link);
    }

    public void addParticipantToCompetition(int competitionId, int participantId, int sortOrder) {
        CompetitionParticipant link = new CompetitionParticipant();
        link.competitionId = competitionId;
        link.participantId = participantId;
        link.sortOrder = sortOrder;
        db.competitionParticipantDao().insert(link);
    }

    public void removeParticipantFromCompetition(int competitionId, int participantId) {
        db.competitionParticipantDao().deleteByCompetitionAndParticipant(competitionId, participantId);
    }

    public List<CompetitionParticipant> getCompetitionParticipants(int competitionId) {
        return db.competitionParticipantDao().getByCompetition(competitionId);
    }

    public void updateParticipantSortOrder(int competitionId, List<Integer> participantIds) {
        for (int i = 0; i < participantIds.size(); i++) {
            db.competitionParticipantDao().updateSortOrder(competitionId, participantIds.get(i), i + 1);
        }
    }

    // ==================== ПРАВИЛА ШТРАФОВ (PENALTY_RULE) ====================

    public List<PenaltyRule> getPenaltyRulesByCategory(int categoryId) {
        return db.penaltyRuleDao().getByCategory(categoryId);
    }

    public long insertPenaltyRule(PenaltyRule rule) {
        return db.penaltyRuleDao().insert(rule);
    }

    public void deletePenaltyRulesByCategory(int categoryId) {
        db.penaltyRuleDao().deleteByCategoryId(categoryId);
    }

    // ==================== РЕЗУЛЬТАТЫ (RESULT) ====================

    public List<Result> getResultsByCategory(int categoryId) {
        return db.resultDao().getByCategory(categoryId);
    }

    public Result getResultByParticipantAndCategory(int participantId, int categoryId) {
        return db.resultDao().getByParticipantAndCategory(participantId, categoryId);
    }

    public void saveResult(Result result) {
        db.resultDao().insert(result);
    }

    public boolean hasResult(int participantId, int categoryId) {
        return db.resultDao().getByParticipantAndCategory(participantId, categoryId) != null;
    }

    public void deleteResult(Result result) {
        db.resultDao().delete(result);
    }
}