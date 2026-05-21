package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "competitions")
public class Competition {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String description;
    public boolean isActive;
    public String startDate;
    public String endDate;

    public Competition() {
        this.isActive = true;
    }

    @Ignore
    public Competition(String name, String description) {
        this.name = name;
        this.description = description;
        this.isActive = true;
    }

    @Ignore
    public Competition(String name, String description, String startDate, String endDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
    }

    // Метод для отображения даты начала в формате ДД.ММ.ГГГГ
    public String getDisplayStartDate() {
        if (startDate == null || startDate.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(startDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return startDate;
        }
    }

    // Метод для отображения даты окончания в формате ДД.ММ.ГГГГ
    public String getDisplayEndDate() {
        if (endDate == null || endDate.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(endDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return endDate;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}