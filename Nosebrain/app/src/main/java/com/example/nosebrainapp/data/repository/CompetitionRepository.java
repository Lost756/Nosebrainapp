package com.example.nosebrainapp.data.repository;

import android.content.Context;
import com.example.nosebrainapp.data.AppDatabase;
import com.example.nosebrainapp.data.entity.*;
import java.util.List;
public class CompetitionRepository {

    private AppDatabase db;

    // Вставка категории
    public long insertCategory(Category category) {
        return db.categoryDao().insert(category);
    }
    // Обновление категории
    public void updateCategory(Category category) {
        db.categoryDao().update(category);
    }
    // Удаление категории по ID
    public void deleteCategoryById(int categoryId) {
        db.categoryDao().deleteById(categoryId);
    }
    // Вставка правила штрафа
    public long insertPenaltyRule(PenaltyRule rule) {
        return db.penaltyRuleDao().insert(rule);
    }
    // Удаление правил штрафа по категории
    public void deletePenaltyRulesByCategory(int categoryId) {
        db.penaltyRuleDao().deleteByCategoryId(categoryId);
    }
    public CompetitionRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public List<Competition> getCompetitions() {
        return db.competitionDao().getActive();
    }

    public List<Category> getCategoriesByCompetition(int competitionId) {
        return db.categoryDao().getByCompetition(competitionId);
    }

    public Category getCategoryById(int categoryId) {
        return db.categoryDao().getById(categoryId);
    }

    public List<Participant> getAvailableParticipants(int competitionId) {
        return db.participantDao().getAvailableForCompetition(competitionId);
    }

    public List<PenaltyRule> getPenaltyRulesByCategory(int categoryId) {
        return db.penaltyRuleDao().getByCategory(categoryId);
    }

    public void saveResult(Result result) {
        db.resultDao().insert(result);
    }

    public boolean hasResult(int participantId, int categoryId) {
        return db.resultDao().getByParticipantAndCategory(participantId, categoryId) != null;
    }

    public List<Result> getResultsByCategory(int categoryId) {
        return db.resultDao().getByCategory(categoryId);
    }
    // Вставка нового соревнования
    public long insertCompetition(Competition competition) {
        return db.competitionDao().insert(competition);
    }

}