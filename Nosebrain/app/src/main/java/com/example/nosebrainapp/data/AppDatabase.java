package com.example.nosebrainapp.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.dao.*;

@Database(entities = {Competition.class, Category.class, Participant.class,
        PenaltyRule.class, Result.class, CompetitionParticipant.class},
        version = 2, exportSchema = false)

public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract CompetitionDao competitionDao();
    public abstract CategoryDao categoryDao();
    public abstract ParticipantDao participantDao();
    public abstract PenaltyRuleDao penaltyRuleDao();
    public abstract ResultDao resultDao();
    public abstract CompetitionParticipantDao competitionParticipantDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "nosework.db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}