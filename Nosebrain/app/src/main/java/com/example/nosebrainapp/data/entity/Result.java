package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import com.google.gson.Gson;
import java.util.Date;
import java.util.Map;

@Entity(tableName = "results")
public class Result {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int categoryId;
    public int participantId;
    public String participantName;
    public double time;
    public int foundItems;
    public String penaltyCountsJson;
    public int penaltyScore;
    public int totalScore;
    public String judgeComment;
    public String createdAt;

    // Пустой конструктор (обязателен для Room)
    public Result() {
    }

    // Конструктор для создания результата
    @Ignore
    public Result(int categoryId, int participantId, String participantName, double time,
                  int foundItems, Map<Integer, Integer> penaltyCounts, int penaltyScore,
                  int totalScore, String judgeComment) {
        this.categoryId = categoryId;
        this.participantId = participantId;
        this.participantName = participantName;
        this.time = time;
        this.foundItems = foundItems;
        this.penaltyCountsJson = new Gson().toJson(penaltyCounts);
        this.penaltyScore = penaltyScore;
        this.totalScore = totalScore;
        this.judgeComment = judgeComment;
        this.createdAt = new Date().toString();
    }
}