package com.example.nosebrainapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import com.example.nosebrainapp.utils.*;
import java.util.*;

public class JudgeActivity extends AppCompatActivity {

    // UI Elements
    private TextView txtParticipantInfo, txtCategoryInfo;
    private TextView timerText, timeLimitText;
    private Button btnStart, btnFound, btnStop, btnSaveResult;
    private TextView foundCountText, hidesCountText, totalPenaltyText;
    private LinearLayout penaltiesContainer, foundTimesContainer;
    private MaterialCardView summaryCard;
    private TextView summaryTime, summaryFound, summaryPenalty, summaryTotal;
    private EditText judgeComment;

    // Data
    private CompetitionRepository repository;
    private Category currentCategory;
    private int categoryId, participantId;
    private String participantName;
    private List<PenaltyRule> penaltyRules;
    private Map<Integer, Integer> penaltyCounts = new HashMap<>();
    private Map<Integer, Integer> penaltyScores = new HashMap<>();

    // Timer
    private TimerHelper timerHelper;
    private double currentTime = 0;
    private boolean attemptCompleted = false;
    private boolean resultSaved = false;

    // Attempt tracking
    private int foundItems = 0;
    private int hidesCount;
    private int maxScore;
    private List<Double> foundTimes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_judge);

        // Get data from intent
        categoryId = getIntent().getIntExtra(Constants.EXTRA_CATEGORY_ID, -1);
        participantId = getIntent().getIntExtra(Constants.EXTRA_PARTICIPANT_ID, -1);
        participantName = getIntent().getStringExtra(Constants.EXTRA_PARTICIPANT_NAME);

        repository = new CompetitionRepository(this);

        initViews();
        loadCategoryData();

        timerHelper = new TimerHelper(new TimerHelper.OnTickListener() {
            @Override
            public void onTick(double seconds) {
                currentTime = seconds;
                timerText.setText(formatTime(seconds));
            }

            @Override
            public void onTimeLimitReached() {
                stopAttempt("time");
            }
        });

        setupButtonListeners();
    }

    private void initViews() {
        txtParticipantInfo = findViewById(R.id.txtParticipantInfo);
        txtCategoryInfo = findViewById(R.id.txtCategoryInfo);
        timerText = findViewById(R.id.timerText);
        timeLimitText = findViewById(R.id.timeLimitText);
        btnStart = findViewById(R.id.btnStart);
        btnFound = findViewById(R.id.btnFound);
        btnStop = findViewById(R.id.btnStop);
        btnSaveResult = findViewById(R.id.btnSaveResult);
        foundCountText = findViewById(R.id.foundCountText);
        hidesCountText = findViewById(R.id.hidesCountText);
        totalPenaltyText = findViewById(R.id.totalPenaltyText);
        penaltiesContainer = findViewById(R.id.penaltiesContainer);
        foundTimesContainer = findViewById(R.id.foundTimesContainer);
        summaryCard = findViewById(R.id.summaryCard);
        summaryTime = findViewById(R.id.summaryTime);
        summaryFound = findViewById(R.id.summaryFound);
        summaryPenalty = findViewById(R.id.summaryPenalty);
        summaryTotal = findViewById(R.id.summaryTotal);
        judgeComment = findViewById(R.id.judgeComment);

        txtParticipantInfo.setText("Участник: " + participantName);
    }

    private void loadCategoryData() {
        currentCategory = repository.getCategoryById(categoryId);
        if (currentCategory != null) {
            txtCategoryInfo.setText("Категория: " + currentCategory.name);
            hidesCount = currentCategory.hidesCount;
            maxScore = currentCategory.maxScore;
            hidesCountText.setText(String.valueOf(hidesCount));
            timeLimitText.setText("Лимит: " + currentCategory.timeLimit + " сек");

            penaltyRules = repository.getPenaltyRulesByCategory(categoryId);
            buildPenaltyCards();
        }
    }

    private void buildPenaltyCards() {
        penaltiesContainer.removeAllViews();
        penaltyCounts.clear();

        for (PenaltyRule rule : penaltyRules) {
            View cardView = getLayoutInflater().inflate(R.layout.item_penalty_rule, penaltiesContainer, false);

            TextView ruleName = cardView.findViewById(R.id.ruleName);
            TextView ruleType = cardView.findViewById(R.id.ruleType);
            LinearLayout flatActions = cardView.findViewById(R.id.flatActionsContainer);
            Button progressiveButton = cardView.findViewById(R.id.progressiveButton);
            TextView countText = cardView.findViewById(R.id.countText);
            TextView amountText = cardView.findViewById(R.id.amountText);

            ruleName.setText(rule.name);
            ruleType.setText(rule.type.equals("flat") ? "Фиксированный" : "Прогрессивный");

            penaltyCounts.put(rule.id, 0);
            penaltyScores.put(rule.id, 0);

            if (rule.type.equals("flat")) {
                flatActions.setVisibility(View.VISIBLE);
                List<Double> points = rule.getPoints();
                for (double point : points) {
                    Button actionBtn = new Button(this);
                    actionBtn.setText(String.valueOf((int) point));
                    actionBtn.setOnClickListener(v -> addFlatPenalty(rule.id, (int) point, countText, amountText));
                    flatActions.addView(actionBtn);
                }
            } else {
                progressiveButton.setVisibility(View.VISIBLE);
                List<Double> points = rule.getPoints();
                progressiveButton.setText("+" + points.get(0).intValue());
                progressiveButton.setOnClickListener(v -> addProgressivePenalty(rule.id, points, progressiveButton, countText, amountText));
            }

            penaltiesContainer.addView(cardView);
        }
    }

    private void addFlatPenalty(int ruleId, int value, TextView countText, TextView amountText) {
        if (attemptCompleted || timerHelper.isRunning() == false) return;

        int currentCount = penaltyCounts.getOrDefault(ruleId, 0);
        int currentAmount = penaltyScores.getOrDefault(ruleId, 0);

        penaltyCounts.put(ruleId, currentCount + value);
        penaltyScores.put(ruleId, currentAmount + value);

        countText.setText(String.valueOf(penaltyCounts.get(ruleId)));
        amountText.setText(String.valueOf(penaltyScores.get(ruleId)));

        updateTotalPenalty();

        if (getTotalPenalty() >= maxScore) {
            stopAttempt("penalty");
        }
    }

    private void addProgressivePenalty(int ruleId, List<Double> points, Button button, TextView countText, TextView amountText) {
        if (attemptCompleted || timerHelper.isRunning() == false) return;

        int currentCount = penaltyCounts.getOrDefault(ruleId, 0);
        if (currentCount >= points.size()) return;

        int addValue = points.get(currentCount).intValue();
        int currentAmount = penaltyScores.getOrDefault(ruleId, 0);

        penaltyCounts.put(ruleId, currentCount + 1);
        penaltyScores.put(ruleId, currentAmount + addValue);

        countText.setText(String.valueOf(penaltyCounts.get(ruleId)));
        amountText.setText(String.valueOf(penaltyScores.get(ruleId)));

        if (currentCount + 1 < points.size()) {
            button.setText("+" + points.get(currentCount + 1).intValue());
        } else {
            button.setEnabled(false);
            button.setText("MAX");
        }

        updateTotalPenalty();

        if (getTotalPenalty() >= maxScore) {
            stopAttempt("penalty");
        }
    }

    private int getTotalPenalty() {
        int total = 0;
        for (int score : penaltyScores.values()) {
            total += score;
        }
        return total;
    }

    private void updateTotalPenalty() {
        totalPenaltyText.setText(String.valueOf(getTotalPenalty()));
    }

    private void setupButtonListeners() {
        btnStart.setOnClickListener(v -> startAttempt());
        btnFound.setOnClickListener(v -> addFoundItem());
        btnStop.setOnClickListener(v -> stopAttempt("stop"));
        btnSaveResult.setOnClickListener(v -> saveResult());
    }

    private void startAttempt() {
        timerHelper.start(currentCategory.timeLimit);
        btnStart.setEnabled(false);
        btnFound.setEnabled(true);
        btnStop.setEnabled(true);
        btnSaveResult.setEnabled(false);
        attemptCompleted = false;
        summaryCard.setVisibility(View.GONE);
    }

    private void addFoundItem() {
        if (attemptCompleted || timerHelper.isRunning() == false) return;

        if (foundItems < hidesCount) {
            foundItems++;
            foundCountText.setText(foundItems + " / " + hidesCount);
            foundTimes.add(currentTime);

            // Add to UI
            TextView timeView = new TextView(this);
            timeView.setText(foundItems + ". " + formatTime(currentTime));
            foundTimesContainer.addView(timeView);
            foundTimesContainer.setVisibility(View.VISIBLE);
            findViewById(R.id.foundTimesLabel).setVisibility(View.VISIBLE);

            if (foundItems == hidesCount) {
                stopAttempt("found");
            }
        }
    }

    private void stopAttempt(String reason) {
        if (timerHelper.isRunning()) {
            timerHelper.stop();
        }

        attemptCompleted = true;
        btnFound.setEnabled(false);
        btnStop.setEnabled(false);
        btnSaveResult.setEnabled(true);

        // Update summary
        summaryCard.setVisibility(View.VISIBLE);
        summaryTime.setText(formatTime(currentTime));
        summaryFound.setText(foundItems + " / " + hidesCount);
        summaryPenalty.setText(String.valueOf(getTotalPenalty()));

        int totalScore = PenaltyCalculator.calculateTotalScore(maxScore, foundItems, hidesCount, getTotalPenalty());
        summaryTotal.setText(String.valueOf(totalScore));

        String reasonText = "";
        switch (reason) {
            case "found": reasonText = "✓ Все закладки найдены!"; break;
            case "time": reasonText = "⏰ Время истекло"; break;
            case "penalty": reasonText = "⚠ Превышен лимит штрафов"; break;
            default: reasonText = "⏹ Попытка остановлена";
        }

        Toast.makeText(this, reasonText, Toast.LENGTH_SHORT).show();
    }

    private void saveResult() {
        if (resultSaved) return;

        int totalScore = PenaltyCalculator.calculateTotalScore(maxScore, foundItems, hidesCount, getTotalPenalty());

        Result result = new Result(
                categoryId,
                participantId,
                participantName,
                currentTime,
                foundItems,
                penaltyCounts,
                getTotalPenalty(),
                totalScore,
                judgeComment.getText().toString()
        );

        repository.saveResult(result);
        resultSaved = true;

        Toast.makeText(this, "Результат сохранён!", Toast.LENGTH_LONG).show();

        // Return to selectors after 2 seconds
        btnSaveResult.postDelayed(() -> finish(), 2000);
    }

    private String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        double secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%05.2f", minutes, secs);
    }
}