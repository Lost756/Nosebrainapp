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
    public String gender;
    public String birthDate;
    public String microchipNumber;
    public String pedigreeNumber;
    public String qualificationBookNumber;
    public String instructorName;

    // Пустой конструктор (обязателен для Room)
    public Participant() {
    }

    // Конструктор только с именем
    @Ignore
    public Participant(String name) {
        this.name = name;
    }

    // Полный конструктор
    @Ignore
    public Participant(String name, String nickname, String breed, String gender,
                       String birthDate, String microchipNumber, String pedigreeNumber,
                       String qualificationBookNumber, String instructorName) {
        this.name = name;
        this.nickname = nickname;
        this.breed = breed;
        this.gender = gender;
        this.birthDate = birthDate;
        this.microchipNumber = microchipNumber;
        this.pedigreeNumber = pedigreeNumber;
        this.qualificationBookNumber = qualificationBookNumber;
        this.instructorName = instructorName;
    }

    @Override
    public String toString() {
        if (nickname != null && !nickname.isEmpty()) {
            return name + " (" + nickname + ")";
        }
        return name;
    }
}