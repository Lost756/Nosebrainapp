package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "competitions")
public class Competition {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String description;
    public boolean isActive;
    public String startDate;
    public String endDate;

    // Пустой конструктор (обязателен для Room)
    public Competition() {
        this.isActive = true;
    }

    // Конструктор для создания соревнования
    @Ignore
    public Competition(String name, String description) {
        this.name = name;
        this.description = description;
        this.isActive = true;
    }

    // Конструктор с датами
    @Ignore
    public Competition(String name, String description, String startDate, String endDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;

    }

    @Override
    public String toString() {
        return name;  // Показываем название соревнования
    }
}