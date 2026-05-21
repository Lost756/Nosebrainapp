package com.example.nosebrainapp.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class SyncDownloadManager {
    private static final String TAG = "SyncDownloadManager";
    private static final String BASE_URL = "http://192.168.0.177/nosework/api/get_data.php";

    private OkHttpClient client;
    private CompetitionRepository repository;
    private Handler mainHandler;

    public SyncDownloadManager(Context context) {
        this.repository = new CompetitionRepository(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface DownloadCallback {
        void onSuccess(ServerData data);
        void onError(String error);
    }

    public interface ImportCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(String message);
    }

    public static class ServerData {
        public List<CompetitionData> competitions = new ArrayList<>();
        public List<CategoryData> categories = new ArrayList<>();
        public List<ParticipantData> participants = new ArrayList<>();
    }

    public static class CompetitionData {
        public int id;
        public String name;
        public String description;
        public String startDate;
        public String endDate;
        public boolean isNew;
    }

    public static class CategoryData {
        public int id;
        public int competitionId;
        public String name;
        public double timeLimit;
        public int hidesCount;
        public int maxScore;
        public boolean isNew;
    }

    public static class ParticipantData {
        public int id;
        public int competitionId;
        public String name;
        public String nickname;
        public String breed;
        public boolean isNew;
    }

    public void fetchAllData(DownloadCallback callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "?action=get_all";
                Log.d(TAG, "Fetching data from: " + url);

                Request request = new Request.Builder().url(url).get().build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d(TAG, "Response: " + responseBody);

                JSONObject result = new JSONObject(responseBody);
                boolean success = result.optBoolean("success", false);

                if (!success) {
                    String errorMsg = result.optString("message", "Неизвестная ошибка");
                    mainHandler.post(() -> callback.onError(errorMsg));
                    return;
                }

                JSONObject data = result.getJSONObject("data");
                ServerData serverData = new ServerData();

                // Парсим соревнования
                if (data.has("competitions") && !data.isNull("competitions")) {
                    JSONArray competitions = data.getJSONArray("competitions");
                    for (int i = 0; i < competitions.length(); i++) {
                        try {
                            JSONObject comp = competitions.getJSONObject(i);
                            CompetitionData cd = new CompetitionData();
                            cd.id = comp.optInt("id", 0);
                            cd.name = comp.optString("name", "Без названия");
                            cd.description = comp.optString("description", "");
                            cd.startDate = comp.optString("start_date", "");
                            cd.endDate = comp.optString("end_date", "");
                            cd.isNew = !repository.competitionExists(cd.name);
                            serverData.competitions.add(cd);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing competition: " + e.getMessage());
                        }
                    }
                }

                // Парсим категории
                if (data.has("categories") && !data.isNull("categories")) {
                    JSONArray categories = data.getJSONArray("categories");
                    for (int i = 0; i < categories.length(); i++) {
                        try {
                            JSONObject cat = categories.getJSONObject(i);
                            CategoryData cd = new CategoryData();
                            cd.id = cat.optInt("id", 0);
                            cd.competitionId = cat.optInt("competition_id", 0);
                            cd.name = cat.optString("name", "Без названия");
                            cd.timeLimit = cat.optDouble("time_limit", 120.0);
                            cd.hidesCount = cat.optInt("hides_count", 5);
                            cd.maxScore = cat.optInt("max_score", 100);
                            cd.isNew = true;
                            serverData.categories.add(cd);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing category: " + e.getMessage());
                        }
                    }
                }

                // Парсим участников
                if (data.has("participants") && !data.isNull("participants")) {
                    JSONArray participants = data.getJSONArray("participants");
                    for (int i = 0; i < participants.length(); i++) {
                        try {
                            JSONObject part = participants.getJSONObject(i);
                            ParticipantData pd = new ParticipantData();
                            pd.id = part.optInt("id", 0);
                            pd.competitionId = part.optInt("competition_id", 0);
                            pd.name = part.optString("name", "Без имени");
                            pd.nickname = part.optString("nickname", "");
                            pd.breed = part.optString("breed", "");
                            pd.isNew = !repository.participantExists(pd.name);
                            serverData.participants.add(pd);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing participant: " + e.getMessage());
                        }
                    }
                }

                Log.d(TAG, "Parsed: " + serverData.competitions.size() + " competitions, " +
                        serverData.categories.size() + " categories, " +
                        serverData.participants.size() + " participants");

                final ServerData finalData = serverData;
                mainHandler.post(() -> callback.onSuccess(finalData));
            } catch (Exception e) {
                Log.e(TAG, "Fetch error", e);
                mainHandler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    public void importSelectedData(ServerData data, List<Integer> selectedCompetitions,
                                   List<Integer> selectedCategories, List<Integer> selectedParticipants,
                                   ImportCallback callback) {
        new Thread(() -> {
            try {
                int imported = 0;

                // Импортируем соревнования
                for (int idx : selectedCompetitions) {
                    if (idx >= 0 && idx < data.competitions.size()) {
                        CompetitionData cd = data.competitions.get(idx);
                        if (cd.isNew) {
                            mainHandler.post(() -> callback.onProgress("Импорт соревнования: " + cd.name));
                            try {
                                Competition competition = new Competition();
                                competition.name = cd.name;
                                competition.description = cd.description;
                                competition.startDate = cd.startDate.isEmpty() ? null : cd.startDate;
                                competition.endDate = cd.endDate.isEmpty() ? null : cd.endDate;
                                competition.isActive = true;
                                long id = repository.insertCompetition(competition);
                                if (id > 0) imported++;
                            } catch (Exception e) {
                                Log.e(TAG, "Error importing competition: " + e.getMessage());
                            }
                        }
                    }
                }

                // Импортируем участников
                for (int idx : selectedParticipants) {
                    if (idx >= 0 && idx < data.participants.size()) {
                        ParticipantData pd = data.participants.get(idx);
                        if (pd.isNew) {
                            mainHandler.post(() -> callback.onProgress("Импорт участника: " + pd.name));
                            try {
                                Participant participant = new Participant();
                                participant.name = pd.name;
                                participant.nickname = pd.nickname.isEmpty() ? null : pd.nickname;
                                participant.breed = pd.breed.isEmpty() ? null : pd.breed;
                                long id = repository.insertParticipant(participant);
                                if (id > 0) imported++;
                            } catch (Exception e) {
                                Log.e(TAG, "Error importing participant: " + e.getMessage());
                            }
                        }
                    }
                }

                // Импортируем категории
                for (int idx : selectedCategories) {
                    if (idx >= 0 && idx < data.categories.size()) {
                        CategoryData cd = data.categories.get(idx);
                        if (cd.isNew) {
                            mainHandler.post(() -> callback.onProgress("Импорт категории: " + cd.name));
                            try {
                                // Находим ID соревнования на устройстве
                                int localCompId = -1;
                                List<Competition> localComps = repository.getCompetitions();
                                for (Competition c : localComps) {
                                    for (CompetitionData sc : data.competitions) {
                                        if (sc.id == cd.competitionId && c.name.equals(sc.name)) {
                                            localCompId = c.id;
                                            break;
                                        }
                                    }
                                }

                                if (localCompId > 0) {
                                    Category category = new Category();
                                    category.competitionId = localCompId;
                                    category.name = cd.name;
                                    category.timeLimit = cd.timeLimit;
                                    category.hidesCount = cd.hidesCount;
                                    category.maxScore = cd.maxScore;
                                    category.sortOrder = 0;
                                    long id = repository.insertCategory(category);
                                    if (id > 0) imported++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error importing category: " + e.getMessage());
                            }
                        }
                    }
                }

                final int finalImported = imported;
                mainHandler.post(() -> callback.onSuccess("Импортировано " + finalImported + " элементов"));
            } catch (Exception e) {
                Log.e(TAG, "Import error", e);
                mainHandler.post(() -> callback.onError("Ошибка импорта: " + e.getMessage()));
            }
        }).start();
    }
}