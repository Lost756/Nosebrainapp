package com.example.nosebrainapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import com.example.nosebrainapp.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.*;

public class SelectorsActivity extends AppCompatActivity {

    private CompetitionRepository repository;
    private int competitionId;
    private String competitionName;

    // UI элементы для участника
    private Spinner spinnerParticipant;
    private Button btnStartAttempt;
    private TextView txtNoParticipants, txtCompetitionInfo;
    private List<Participant> participants;
    private int selectedParticipantId = -1;
    private String selectedParticipantName = "";

    // UI элементы для управления категориями
    private Spinner spinnerCategory;
    private Button btnAddCategory, btnSaveCategory, btnDeleteCategory;
    private LinearLayout categoryFormPanel;
    private LinearLayout penaltyRulesContainer;
    private Button btnAddPenaltyRule;

    // Поля формы категории
    private TextInputEditText etCategoryName;
    private TextInputEditText etTimeLimit;
    private TextInputEditText etHidesCount;
    private TextInputEditText etMaxScore;

    private List<Category> categories;
    private Category currentCategory;
    private List<PenaltyRuleInput> penaltyRules = new ArrayList<>();
    private int nextPenaltyIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectors);

        repository = new CompetitionRepository(this);

        competitionId = getIntent().getIntExtra(Constants.EXTRA_COMPETITION_ID, -1);
        competitionName = getIntent().getStringExtra(Constants.EXTRA_COMPETITION_NAME);

        if (competitionId == -1) {
            Toast.makeText(this, "Ошибка: соревнование не выбрано", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadParticipants();
        loadCategories();

        // Обработчики для участника
        btnStartAttempt.setOnClickListener(v -> startJudging());

        // Обработчики для категорий
        btnAddCategory.setOnClickListener(v -> showAddCategoryForm());
        btnSaveCategory.setOnClickListener(v -> saveCategory());
        btnDeleteCategory.setOnClickListener(v -> deleteCategory());
        btnAddPenaltyRule.setOnClickListener(v -> addPenaltyRuleRow(null));
    }

    private void initViews() {
        setTitle("Параметры - " + competitionName);

        // Участник
        spinnerParticipant = findViewById(R.id.spinnerParticipant);
        btnStartAttempt = findViewById(R.id.btnStartAttempt);
        txtNoParticipants = findViewById(R.id.txtNoParticipants);
        txtCompetitionInfo = findViewById(R.id.txtCompetitionInfo);

        txtCompetitionInfo.setText("Соревнование: " + competitionName);
        btnStartAttempt.setEnabled(false);

        // Категории
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnSaveCategory = findViewById(R.id.btnSaveCategory);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        categoryFormPanel = findViewById(R.id.categoryFormPanel);
        penaltyRulesContainer = findViewById(R.id.penaltyRulesContainer);
        btnAddPenaltyRule = findViewById(R.id.btnAddPenaltyRule);

        etCategoryName = findViewById(R.id.etCategoryName);
        etTimeLimit = findViewById(R.id.etTimeLimit);
        etHidesCount = findViewById(R.id.etHidesCount);
        etMaxScore = findViewById(R.id.etMaxScore);

        // Скрываем форму при загрузке
        categoryFormPanel.setVisibility(View.GONE);
        btnDeleteCategory.setEnabled(false);
    }

    // ==================== УПРАВЛЕНИЕ УЧАСТНИКАМИ ====================

    private void loadParticipants() {
        participants = repository.getAvailableParticipants(competitionId);

        if (participants == null || participants.isEmpty()) {
            spinnerParticipant.setVisibility(View.GONE);
            txtNoParticipants.setVisibility(View.VISIBLE);
            btnStartAttempt.setEnabled(false);
        } else {
            spinnerParticipant.setVisibility(View.VISIBLE);
            txtNoParticipants.setVisibility(View.GONE);

            ArrayAdapter<Participant> adapter = new ArrayAdapter<Participant>(this,
                    android.R.layout.simple_spinner_item, participants) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    Participant p = participants.get(position);
                    if (p.nickname != null && !p.nickname.isEmpty()) {
                        view.setText(p.name + " (" + p.nickname + ")");
                    } else {
                        view.setText(p.name);
                    }
                    return view;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                    Participant p = participants.get(position);
                    if (p.nickname != null && !p.nickname.isEmpty()) {
                        view.setText(p.name + " (" + p.nickname + ")");
                    } else {
                        view.setText(p.name);
                    }
                    return view;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerParticipant.setAdapter(adapter);

            spinnerParticipant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < participants.size()) {
                        selectedParticipantId = participants.get(position).id;
                        selectedParticipantName = participants.get(position).name;
                        btnStartAttempt.setEnabled(true);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    btnStartAttempt.setEnabled(false);
                }
            });
        }
    }

    private void startJudging() {
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "Сначала создайте категорию", Toast.LENGTH_SHORT).show();
            return;
        }

        // Показываем диалог выбора категории
        String[] categoryNames = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            categoryNames[i] = categories.get(i).name;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите категорию")
                .setItems(categoryNames, (dialog, which) -> {
                    Category selectedCategory = categories.get(which);
                    Intent intent = new Intent(SelectorsActivity.this, JudgeActivity.class);
                    intent.putExtra(Constants.EXTRA_CATEGORY_ID, selectedCategory.id);
                    intent.putExtra(Constants.EXTRA_PARTICIPANT_ID, selectedParticipantId);
                    intent.putExtra(Constants.EXTRA_PARTICIPANT_NAME, selectedParticipantName);
                    startActivity(intent);
                })
                .show();
    }

    // ==================== УПРАВЛЕНИЕ КАТЕГОРИЯМИ ====================

    private void loadCategories() {
        categories = repository.getCategoriesByCompetition(competitionId);

        if (categories == null || categories.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    Collections.singletonList("-- Нет категорий --"));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
            btnDeleteCategory.setEnabled(false);
        } else {
            ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(this,
                    android.R.layout.simple_spinner_item, categories) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    view.setText(categories.get(position).name);
                    return view;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                    view.setText(categories.get(position).name);
                    return view;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);

            spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < categories.size()) {
                        currentCategory = categories.get(position);
                        loadCategoryForEdit(currentCategory);
                        categoryFormPanel.setVisibility(View.VISIBLE);
                        btnDeleteCategory.setEnabled(true);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    categoryFormPanel.setVisibility(View.GONE);
                    btnDeleteCategory.setEnabled(false);
                }
            });

            if (!categories.isEmpty()) {
                currentCategory = categories.get(0);
                loadCategoryForEdit(currentCategory);
                categoryFormPanel.setVisibility(View.VISIBLE);
                btnDeleteCategory.setEnabled(true);
            }
        }
    }

    private void loadCategoryForEdit(Category category) {
        etCategoryName.setText(category.name);
        etTimeLimit.setText(String.valueOf(category.timeLimit));
        etHidesCount.setText(String.valueOf(category.hidesCount));
        etMaxScore.setText(String.valueOf(category.maxScore));

        List<PenaltyRule> rules = repository.getPenaltyRulesByCategory(category.id);
        penaltyRules.clear();
        penaltyRulesContainer.removeAllViews();

        for (PenaltyRule rule : rules) {
            List<Integer> points = new ArrayList<>();
            for (double p : rule.getPoints()) {
                points.add((int) p);
            }
            PenaltyRuleInput ruleInput = new PenaltyRuleInput(rule.name, rule.type, points);
            penaltyRules.add(ruleInput);
            addPenaltyRuleRow(ruleInput);
        }

        if (penaltyRules.isEmpty()) {
            addPenaltyRuleRow(null);
        }
    }

    private void showAddCategoryForm() {
        currentCategory = null;
        etCategoryName.setText("");
        etTimeLimit.setText("120");
        etHidesCount.setText("5");
        etMaxScore.setText("100");

        penaltyRules.clear();
        penaltyRulesContainer.removeAllViews();
        addPenaltyRuleRow(null);

        categoryFormPanel.setVisibility(View.VISIBLE);
        btnDeleteCategory.setEnabled(false);

        if (categories != null && !categories.isEmpty()) {
            spinnerCategory.setSelection(0);
        }
    }

    private void addPenaltyRuleRow(PenaltyRuleInput existingRule) {
        View row = getLayoutInflater().inflate(R.layout.item_penalty_rule_input, penaltyRulesContainer, false);

        TextInputEditText etRuleName = row.findViewById(R.id.etRuleName);
        Spinner spinnerRuleType = row.findViewById(R.id.spinnerRuleType);
        TextInputEditText etRulePoints = row.findViewById(R.id.etRulePoints);
        MaterialButton btnRemoveRule = row.findViewById(R.id.btnRemoveRule);

        final int position = penaltyRulesContainer.getChildCount();

        if (existingRule != null) {
            etRuleName.setText(existingRule.getName());
            etRulePoints.setText(pointsToString(existingRule.getPoints()));
            spinnerRuleType.setSelection("progressive".equals(existingRule.getType()) ? 1 : 0);
        }

        btnRemoveRule.setOnClickListener(v -> {
            penaltyRulesContainer.removeView(row);
            if (penaltyRulesContainer.getChildCount() == 0) {
                addPenaltyRuleRow(null);
            }
        });

        // Сохраняем данные при изменении
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                updatePenaltyRuleAtPosition(position, etRuleName.getText().toString(),
                        spinnerRuleType.getSelectedItemPosition(), etRulePoints.getText().toString());
            }
        };

        etRuleName.addTextChangedListener(watcher);
        etRulePoints.addTextChangedListener(watcher);

        spinnerRuleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                updatePenaltyRuleAtPosition(position, etRuleName.getText().toString(),
                        pos, etRulePoints.getText().toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        penaltyRulesContainer.addView(row);
    }

    private void updatePenaltyRuleAtPosition(int position, String name, int typePosition, String pointsStr) {
        while (penaltyRules.size() <= position) {
            penaltyRules.add(new PenaltyRuleInput());
        }

        PenaltyRuleInput rule = penaltyRules.get(position);
        rule.setName(name);
        rule.setType(typePosition == 0 ? "flat" : "progressive");

        List<Integer> points = new ArrayList<>();
        String[] parts = pointsStr.split(",");
        for (String part : parts) {
            try {
                points.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        rule.setPoints(points);
    }

    private String pointsToString(List<Integer> points) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(points.get(i));
        }
        return sb.toString();
    }

    private void saveCategory() {
        String name = etCategoryName.getText().toString().trim();
        String timeLimitStr = etTimeLimit.getText().toString().trim();
        String hidesCountStr = etHidesCount.getText().toString().trim();
        String maxScoreStr = etMaxScore.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название категории", Toast.LENGTH_SHORT).show();
            return;
        }

        double timeLimit;
        int hidesCount, maxScore;

        try {
            timeLimit = Double.parseDouble(timeLimitStr);
            hidesCount = Integer.parseInt(hidesCountStr);
            maxScore = Integer.parseInt(maxScoreStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Проверьте числовые поля", Toast.LENGTH_SHORT).show();
            return;
        }

        List<PenaltyRuleInput> validRules = new ArrayList<>();
        for (PenaltyRuleInput rule : penaltyRules) {
            if (rule.getName() != null && !rule.getName().isEmpty() &&
                    rule.getPoints() != null && !rule.getPoints().isEmpty()) {
                validRules.add(rule);
            }
        }

        if (validRules.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одно правило штрафа", Toast.LENGTH_SHORT).show();
            return;
        }

        // Конвертируем в формат для БД
        List<Map<String, Object>> rulesForServer = new ArrayList<>();
        for (PenaltyRuleInput rule : validRules) {
            Map<String, Object> ruleMap = new HashMap<>();
            ruleMap.put("name", rule.getName());
            ruleMap.put("type", rule.getType());
            List<Double> doublePoints = new ArrayList<>();
            for (int p : rule.getPoints()) {
                doublePoints.add((double) p);
            }
            ruleMap.put("points", doublePoints);
            rulesForServer.add(ruleMap);
        }

        new Thread(() -> {
            if (currentCategory == null) {
                // Создание новой категории
                Category newCategory = new Category();
                newCategory.competitionId = competitionId;
                newCategory.name = name;
                newCategory.timeLimit = timeLimit;
                newCategory.hidesCount = hidesCount;
                newCategory.maxScore = maxScore;
                newCategory.sortOrder = categories != null ? categories.size() : 0;

                long categoryId = repository.insertCategory(newCategory);

                for (int i = 0; i < rulesForServer.size(); i++) {
                    Map<String, Object> ruleData = rulesForServer.get(i);
                    PenaltyRule rule = new PenaltyRule();
                    rule.categoryId = (int) categoryId;
                    rule.name = (String) ruleData.get("name");
                    rule.type = (String) ruleData.get("type");
                    rule.pointsJson = new com.google.gson.Gson().toJson(ruleData.get("points"));
                    rule.sequenceIndex = i + 1;
                    repository.insertPenaltyRule(rule);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Категория создана", Toast.LENGTH_SHORT).show();
                    loadCategories();
                    categoryFormPanel.setVisibility(View.GONE);
                });
            } else {
                // Обновление существующей категории
                currentCategory.name = name;
                currentCategory.timeLimit = timeLimit;
                currentCategory.hidesCount = hidesCount;
                currentCategory.maxScore = maxScore;
                repository.updateCategory(currentCategory);

                repository.deletePenaltyRulesByCategory(currentCategory.id);
                for (int i = 0; i < rulesForServer.size(); i++) {
                    Map<String, Object> ruleData = rulesForServer.get(i);
                    PenaltyRule rule = new PenaltyRule();
                    rule.categoryId = currentCategory.id;
                    rule.name = (String) ruleData.get("name");
                    rule.type = (String) ruleData.get("type");
                    rule.pointsJson = new com.google.gson.Gson().toJson(ruleData.get("points"));
                    rule.sequenceIndex = i + 1;
                    repository.insertPenaltyRule(rule);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                    loadCategories();
                });
            }
        }).start();
    }

    private void deleteCategory() {
        if (currentCategory == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удаление категории")
                .setMessage("Вы уверены, что хотите удалить категорию \"" + currentCategory.name + "\"?\nВсе результаты в этой категории будут удалены.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    new Thread(() -> {
                        repository.deleteCategoryById(currentCategory.id);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Категория удалена", Toast.LENGTH_SHORT).show();
                            loadCategories();
                            categoryFormPanel.setVisibility(View.GONE);
                        });
                    }).start();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadParticipants();
        loadCategories();
    }

    // Внутренний класс для хранения данных штрафа
    private static class PenaltyRuleInput {
        private String name;
        private String type;
        private List<Integer> points;

        PenaltyRuleInput() {}

        PenaltyRuleInput(String name, String type, List<Integer> points) {
            this.name = name;
            this.type = type;
            this.points = points;
        }

        String getName() { return name; }
        void setName(String name) { this.name = name; }
        String getType() { return type; }
        void setType(String type) { this.type = type; }
        List<Integer> getPoints() { return points; }
        void setPoints(List<Integer> points) { this.points = points; }
    }
}