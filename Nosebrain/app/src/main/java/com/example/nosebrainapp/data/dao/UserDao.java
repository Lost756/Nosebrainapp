package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.User;
import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users ORDER BY username ASC")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE id = :id")
    User getById(int id);

    @Query("SELECT * FROM users WHERE role = :role")
    List<User> getByRole(String role);

    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);
}