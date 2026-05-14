package com.example.nosebrainapp.utils;

import com.example.nosebrainapp.data.entity.PenaltyRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PenaltyCalculator {

    public static int calculatePenaltyScore(List<PenaltyRule> rules, Map<Integer, Integer> counts) {
        int total = 0;
        for (PenaltyRule rule : rules) {
            int count = counts.getOrDefault(rule.id, 0);
            total += calculateRuleScore(rule, count);
        }
        return total;
    }

    private static int calculateRuleScore(PenaltyRule rule, int count) {
        if (count <= 0) return 0;

        List<Double> points = rule.getPoints();

        if (rule.type.equals("flat")) {
            // Для flat штрафов count - это сумма значений
            return count;
        } else {
            // Для progressive - суммируем значения по порядку
            int score = 0;
            for (int i = 0; i < count && i < points.size(); i++) {
                score += points.get(i).intValue();
            }
            return score;
        }
    }

    public static int calculateTotalScore(int maxScore, int foundItems, int hidesCount, int penaltyScore) {
        if (foundItems < hidesCount) return 0;
        return Math.max(0, maxScore - penaltyScore);
    }
}