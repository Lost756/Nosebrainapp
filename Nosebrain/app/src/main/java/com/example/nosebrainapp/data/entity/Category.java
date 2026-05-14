package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int competitionId;
    public String name;
    public double timeLimit;
    public int hidesCount;
    public int maxScore;
    public int sortOrder;

    // Пустой конструктор (обязателен для Room)
    public Category() {
        this.sortOrder = 0;
    }

    // Конструктор для создания категории
    @Ignore
    public Category(int competitionId, String name, double timeLimit, int hidesCount, int maxScore) {
        this.competitionId = competitionId;
        this.name = name;
        this.timeLimit = timeLimit;
        this.hidesCount = hidesCount;
        this.maxScore = maxScore;
        this.sortOrder = 0;
    }
}