package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import com.google.gson.Gson;
import java.util.List;

@Entity(tableName = "penalty_rules")
public class PenaltyRule {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int categoryId;
    public String name;
    public String type; // "flat" или "progressive"
    public String pointsJson;
    public int sequenceIndex;

    // Пустой конструктор (обязателен для Room)
    public PenaltyRule() {
    }

    // Конструктор для создания нового правила
    @Ignore
    public PenaltyRule(int categoryId, String name, String type, List<Double> points, int sequenceIndex) {
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
        this.pointsJson = new Gson().toJson(points);
        this.sequenceIndex = sequenceIndex;
    }

    public List<Double> getPoints() {
        Gson gson = new Gson();
        return gson.fromJson(pointsJson, new com.google.gson.reflect.TypeToken<List<Double>>(){}.getType());
    }
}