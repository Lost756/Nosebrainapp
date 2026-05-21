package com.example.nosebrainapp.data.repository;

import android.content.Context;
import com.example.nosebrainapp.data.AppDatabase;
import com.example.nosebrainapp.data.entity.*;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
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
    // ==================== ПОЛЬЗОВАТЕЛИ (USER) ====================

    public List<User> getAllUsers() {
        return db.userDao().getAll();
    }

    public User getUserById(int userId) {
        return db.userDao().getById(userId);
    }

    public void updateUser(User user) {
        db.userDao().update(user);
    }

    // Проверка существования соревнования по имени
    public boolean competitionExists(String name) {
        List<Competition> all = db.competitionDao().getAll();
        for (Competition c : all) {
            if (c.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    // Проверка существования участника по имени
    public boolean participantExists(String name) {
        List<Participant> all = db.participantDao().getAll();
        for (Participant p : all) {
            if (p.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    // Проверка существования категории по имени
    public boolean categoryExists(String name, int competitionId) {
        List<Category> categories = db.categoryDao().getByCompetition(competitionId);
        for (Category c : categories) {
            if (c.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    // Импорт соревнования с сервера
    public long importCompetition(JSONObject compData) throws Exception {
        Competition competition = new Competition();
        competition.name = compData.getString("name");
        competition.description = compData.optString("description", "");
        competition.startDate = compData.optString("start_date", null);
        competition.endDate = compData.optString("end_date", null);
        competition.isActive = true;

        return insertCompetition(competition);
    }

    // Импорт участника с сервера
    public long importParticipant(JSONObject partData) throws Exception {
        Participant participant = new Participant();
        participant.name = partData.getString("name");
        participant.nickname = partData.optString("nickname", null);
        participant.breed = partData.optString("breed", null);
        participant.gender = partData.optString("gender", null);
        participant.birthDate = partData.optString("birth_date", null);
        participant.microchipNumber = partData.optString("microchip_number", null);
        participant.pedigreeNumber = partData.optString("pedigree_number", null);
        participant.qualificationBookNumber = partData.optString("qualification_book_number", null);
        participant.instructorName = partData.optString("instructor_name", null);

        return insertParticipant(participant);
    }

    // Импорт категории с сервера
    public long importCategory(JSONObject catData, int competitionId) throws Exception {
        Category category = new Category();
        category.competitionId = competitionId;
        category.name = catData.getString("name");
        category.timeLimit = catData.getDouble("time_limit");
        category.hidesCount = catData.getInt("hides_count");
        category.maxScore = catData.getInt("max_score");
        category.sortOrder = 0;

        long categoryId = insertCategory(category);

        // Импорт правил штрафов
        JSONArray rulesArray = catData.optJSONArray("penalty_rules");
        if (rulesArray != null) {
            for (int i = 0; i < rulesArray.length(); i++) {
                JSONObject ruleData = rulesArray.getJSONObject(i);
                PenaltyRule rule = new PenaltyRule();
                rule.categoryId = (int) categoryId;
                rule.name = ruleData.getString("name");
                rule.type = ruleData.getString("type");

                JSONArray pointsArray = ruleData.getJSONArray("points");
                List<Double> points = new ArrayList<>();
                for (int j = 0; j < pointsArray.length(); j++) {
                    points.add(pointsArray.getDouble(j));
                }
                rule.pointsJson = new Gson().toJson(points);
                rule.sequenceIndex = i + 1;
                insertPenaltyRule(rule);
            }
        }

        return categoryId;
    }
}