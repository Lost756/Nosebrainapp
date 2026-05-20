package com.example.nosebrainapp.data;

import android.content.Context;
import com.example.nosebrainapp.data.entity.User;
import java.util.List;

public class DatabaseSeeder {

    public static void seedIfEmpty(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        // Создаём тестового администратора, если нет пользователей
        List<User> users = db.userDao().getAll();
        if (users == null || users.isEmpty()) {
            // Создаём администратора
            User admin = new User();
            admin.username = "admin";
            admin.passwordHash = "admin"; // В реальном проекте используйте хэш
            admin.role = "admin";
            db.userDao().insert(admin);

            // Создаём тестового судью
            User judge = new User();
            judge.username = "judge";
            judge.passwordHash = "judge";
            judge.role = "judge";
            db.userDao().insert(judge);

            // Создаём тестового секретаря
            User secretary = new User();
            secretary.username = "secretary";
            secretary.passwordHash = "secretary";
            secretary.role = "secretary";
            db.userDao().insert(secretary);
        }
    }
}