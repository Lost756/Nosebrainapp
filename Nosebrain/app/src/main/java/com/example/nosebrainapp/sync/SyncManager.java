package com.example.nosebrainapp.sync;

import android.content.Context;
import android.util.Log;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class SyncManager {
    private static final String TAG = "SyncManager";
    // Для эмулятора: 10.0.2.2, для телефона: IP компьютера
    private static final String BASE_URL = "http://192.168.0.177/nosework/api/sync.php";

    private OkHttpClient client;
    private CompetitionRepository repository;

    public SyncManager(Context context) {
        this.repository = new CompetitionRepository(context);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // ==================== ПОЛНАЯ СИНХРОНИЗАЦИЯ ====================
    public void syncAllData(int competitionId, SyncCallback callback) {
        new Thread(() -> {
            try {
                Competition competition = repository.getCompetitionById(competitionId);
                if (competition == null) {
                    callback.onError("Соревнование не найдено");
                    return;
                }

                // 1. Синхронизируем соревнование
                int serverCompetitionId = syncCompetition(competition);
                if (serverCompetitionId == -1) {
                    callback.onError("Ошибка синхронизации соревнования");
                    return;
                }

                // 2. Синхронизируем категории
                List<Category> categories = repository.getCategoriesByCompetition(competitionId);
                for (Category category : categories) {
                    syncCategory(category, serverCompetitionId);
                }

                // 3. Синхронизируем участников
                List<Participant> participants = repository.getAllParticipants();
                for (Participant participant : participants) {
                    int serverParticipantId = syncParticipant(participant, serverCompetitionId);
                    if (serverParticipantId != -1) {
                        // Сохраняем соответствие ID для результатов
                        saveParticipantMapping(participant.id, serverParticipantId);
                    }
                }

                // 4. Синхронизируем результаты
                for (Category category : categories) {
                    List<Result> results = repository.getResultsByCategory(category.id);
                    for (Result result : results) {
                        syncResult(result, category.id, result.participantId);
                    }
                }

                callback.onSuccess("Полная синхронизация завершена!");
            } catch (Exception e) {
                Log.e(TAG, "Sync error", e);
                callback.onError("Ошибка: " + e.getMessage());
            }
        }).start();
    }

    // Синхронизация соревнования
    private int syncCompetition(Competition competition) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject compJson = new JSONObject();
        compJson.put("mobile_id", competition.id);
        compJson.put("name", competition.name);
        compJson.put("description", competition.description != null ? competition.description : "");
        compJson.put("start_date", competition.startDate != null ? competition.startDate : "");
        compJson.put("end_date", competition.endDate != null ? competition.endDate : "");
        json.put("competition", compJson);
        json.put("action", "sync_competition");

        String response = sendRequest(json);
        JSONObject result = new JSONObject(response);

        if (result.optBoolean("success", false)) {
            return result.optInt("server_id", -1);
        }
        return -1;
    }

    // Синхронизация участника
    private int syncParticipant(Participant participant, int competitionServerId) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject partJson = new JSONObject();
        partJson.put("mobile_id", participant.id);
        partJson.put("name", participant.name);
        partJson.put("nickname", participant.nickname != null ? participant.nickname : "");
        partJson.put("breed", participant.breed != null ? participant.breed : "");
        partJson.put("gender", participant.gender != null ? participant.gender : "");
        partJson.put("birth_date", participant.birthDate != null ? participant.birthDate : "");
        partJson.put("microchip_number", participant.microchipNumber != null ? participant.microchipNumber : "");
        partJson.put("pedigree_number", participant.pedigreeNumber != null ? participant.pedigreeNumber : "");
        partJson.put("qualification_book_number", participant.qualificationBookNumber != null ? participant.qualificationBookNumber : "");
        partJson.put("instructor_name", participant.instructorName != null ? participant.instructorName : "");
        json.put("participant", partJson);
        json.put("competition_server_id", competitionServerId);
        json.put("action", "sync_participant");

        String response = sendRequest(json);
        JSONObject result = new JSONObject(response);

        if (result.optBoolean("success", false)) {
            return result.optInt("server_id", -1);
        }
        return -1;
    }

    // Синхронизация категории
    private int syncCategory(Category category, int competitionServerId) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject catJson = new JSONObject();
        catJson.put("mobile_id", category.id);
        catJson.put("name", category.name);
        catJson.put("time_limit", category.timeLimit);
        catJson.put("hides_count", category.hidesCount);
        catJson.put("max_score", category.maxScore);

        // Правила штрафов
        List<PenaltyRule> rules = repository.getPenaltyRulesByCategory(category.id);
        JSONArray rulesArray = new JSONArray();
        for (PenaltyRule rule : rules) {
            JSONObject ruleJson = new JSONObject();
            ruleJson.put("name", rule.name);
            ruleJson.put("type", rule.type);
            JSONArray pointsArray = new JSONArray();
            for (double p : rule.getPoints()) {
                pointsArray.put(p);
            }
            ruleJson.put("points", pointsArray);
            rulesArray.put(ruleJson);
        }
        catJson.put("penalty_rules", rulesArray);

        json.put("category", catJson);
        json.put("competition_server_id", competitionServerId);
        json.put("action", "sync_category");

        String response = sendRequest(json);
        JSONObject result = new JSONObject(response);

        if (result.optBoolean("success", false)) {
            return result.optInt("server_id", -1);
        }
        return -1;
    }

    // Синхронизация результата
    private void syncResult(Result result, int categoryMobileId, int participantMobileId) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject resultJson = new JSONObject();
        resultJson.put("time", result.time);
        resultJson.put("found_items", result.foundItems);
        resultJson.put("penalty_counts", new JSONObject(result.penaltyCountsJson));
        resultJson.put("judge_comment", result.judgeComment != null ? result.judgeComment : "");
        json.put("result", resultJson);
        json.put("action", "sync_result");

        String response = sendRequest(json);
        JSONObject res = new JSONObject(response);

        if (!res.optBoolean("success", false)) {
            throw new Exception(res.optString("error", "Unknown error"));
        }
    }

    // Отправка запроса на сервер
    private String sendRequest(JSONObject json) throws Exception {
        Log.d(TAG, "Sending: " + json.toString());

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        Log.d(TAG, "Response: " + responseBody);

        if (!response.isSuccessful()) {
            throw new Exception("HTTP error: " + response.code());
        }

        return responseBody;
    }

    // Сохранение соответствия ID для результатов
    private void saveParticipantMapping(int mobileId, int serverId) {
    }
}