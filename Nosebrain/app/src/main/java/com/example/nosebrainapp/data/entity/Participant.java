package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "participants")
public class Participant {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String nickname;
    public String breed;
    public String instructorName;

    // Пустой конструктор (обязателен для Room)
    public Participant() {
    }

    // Конструктор только с именем
    @Ignore  // Игнорируем для Room, используем только в коде
    public Participant(String name) {
        this.name = name;
    }

    // Конструктор с именем и кличкой
    @Ignore  // Игнорируем для Room
    public Participant(String name, String nickname) {
        this.name = name;
        this.nickname = nickname;
    }
    @Override
    public String toString() {
        if (nickname != null && !nickname.isEmpty()) {
            return name + " (" + nickname + ")";  // "Анна Смирнова (Барон)"
        }
        return name;  // Только имя
    }
}