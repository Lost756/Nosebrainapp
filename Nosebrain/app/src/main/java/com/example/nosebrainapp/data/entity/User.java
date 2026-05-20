package com.example.nosebrainapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String username;
    public String passwordHash;
    public String role; // admin, judge, secretary
    public Integer competitionId;
    public String displayName;

    // Пустой конструктор для Room
    public User() {
    }

    @Ignore
    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }
}