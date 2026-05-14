package com.example.nosebrainapp.data.dao;

import androidx.room.*;
import com.example.nosebrainapp.data.entity.PenaltyRule;
import java.util.List;

@Dao
public interface PenaltyRuleDao {
    @Query("SELECT * FROM penalty_rules WHERE categoryId = :categoryId ORDER BY sequenceIndex ASC")
    List<PenaltyRule> getByCategory(int categoryId);

    @Insert
    long insert(PenaltyRule rule);
}