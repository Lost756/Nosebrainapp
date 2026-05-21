package com.example.nosebrainapp.sync;

import android.content.Context;
import android.util.Log;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class SyncManager {
    private static final String TAG = "SyncManager";
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

    // Конвертация даты из формата ДД.ММ.ГГГГ в ГГГГ-ММ-ДД для сервера
    private String convertDateToServerFormat(String date) {
        if (date == null || date.isEmpty()) return "";
        try {
            // Парсим дату в формате ДД.ММ.ГГГГ
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            // Преобразуем в формат ГГГГ-ММ-ДД для сервера
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return outputFormat.format(parsedDate);
        } catch (Exception e) {
            Log.e(TAG, "Date parsing error: " + e.getMessage());
            return date;
        }
    }

    // Конвертация даты из формата сервера в ДД.ММ.ГГГГ для отображения
    private String convertDateFromServerFormat(String date) {
        if (date == null || date.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return outputFormat.format(parsedDate);
        } catch (Exception e) {
            return date;
        }
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

                Log.d(TAG, "Начинаем синхронизацию соревнования: " + competition.name);

                int serverCompetitionId = syncCompetition(competition);
                if (serverCompetitionId == -1) {
                    callback.onError("Ошибка синхронизации соревнования");
                    return;
                }
                Log.d(TAG, "Соревнование синхронизировано: mobile_id=" + competition.id + " -> server_id=" + serverCompetitionId);

                List<Category> categories = repository.getCategoriesByCompetition(competitionId);
                for (Category category : categories) {
                    int serverCategoryId = syncCategory(category, serverCompetitionId);
                    Log.d(TAG, "Категория синхронизирована: mobile_id=" + category.id + " -> server_id=" + serverCategoryId);
                }

                List<Participant> participants = repository.getAllParticipants();
                for (Participant participant : participants) {
                    int serverParticipantId = syncParticipant(participant, serverCompetitionId);
                    Log.d(TAG, "Участник синхронизирован: mobile_id=" + participant.id + " -> server_id=" + serverParticipantId);
                }

                for (Category category : categories) {
                    List<Result> results = repository.getResultsByCategory(category.id);
                    Log.d(TAG, "Синхронизация результатов для категории ID=" + category.id + ": " + results.size() + " результатов");

                    for (Result result : results) {
                        boolean success = syncResult(result, category.id, result.participantId);
                        if (success) {
                            Log.d(TAG, "Результат синхронизирован: " + result.participantName);
                        } else {
                            Log.e(TAG, "Ошибка синхронизации результата: " + result.participantName);
                        }
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

        // Конвертируем даты из ДД.ММ.ГГГГ в ГГГГ-ММ-ДД для сервера
        String startDate = competition.startDate != null ? convertDateToServerFormat(competition.startDate) : "";
        String endDate = competition.endDate != null ? convertDateToServerFormat(competition.endDate) : "";
        compJson.put("start_date", startDate);
        compJson.put("end_date", endDate);

        json.put("competition", compJson);
        json.put("action", "sync_competition");

        Log.d(TAG, "Sending competition: " + json.toString());

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

        // Конвертируем дату рождения
        String birthDate = participant.birthDate != null ? convertDateToServerFormat(participant.birthDate) : "";
        partJson.put("birth_date", birthDate);

        partJson.put("microchip_number", participant.microchipNumber != null ? participant.microchipNumber : "");
        partJson.put("pedigree_number", participant.pedigreeNumber != null ? participant.pedigreeNumber : "");
        partJson.put("qualification_book_number", participant.qualificationBookNumber != null ? participant.qualificationBookNumber : "");
        partJson.put("instructor_name", participant.instructorName != null ? participant.instructorName : "");
        json.put("participant", partJson);
        json.put("competition_server_id", competitionServerId);
        json.put("action", "sync_participant");

        Log.d(TAG, "Sending participant: " + json.toString());

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

        Log.d(TAG, "Sending category: " + json.toString());

        String response = sendRequest(json);
        JSONObject result = new JSONObject(response);

        if (result.optBoolean("success", false)) {
            return result.optInt("server_id", -1);
        }
        return -1;
    }

    // Синхронизация результата
    private boolean syncResult(Result result, int categoryMobileId, int participantMobileId) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject resultJson = new JSONObject();
        resultJson.put("time", result.time);
        resultJson.put("found_items", result.foundItems);

        JSONObject penaltyObj = new JSONObject(result.penaltyCountsJson);
        resultJson.put("penalty_counts", penaltyObj);
        resultJson.put("judge_comment", result.judgeComment != null ? result.judgeComment : "");

        json.put("result", resultJson);
        json.put("category_id", categoryMobileId);
        json.put("participant_id", participantMobileId);
        json.put("action", "sync_result");

        Log.d(TAG, "Sending result: category_mobile_id=" + categoryMobileId + ", participant_mobile_id=" + participantMobileId);

        String response = sendRequest(json);
        JSONObject res = new JSONObject(response);

        return res.optBoolean("success", false);
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
}