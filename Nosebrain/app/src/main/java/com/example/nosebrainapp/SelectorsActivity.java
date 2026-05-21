package com.example.nosebrainapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import com.example.nosebrainapp.utils.Constants;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.*;

public class SelectorsActivity extends AppCompatActivity {

    private static final String TAG = "SelectorsActivity";
    private CompetitionRepository repository;
    private int competitionId;
    private String competitionName;

    // UI элементы для участника
    private Spinner spinnerParticipant;
    private Button btnStartAttempt, btnAddParticipant;
    private TextView txtNoParticipants, txtCompetitionInfo;
    private List<Participant> participants;
    private int selectedParticipantId = -1;
    private String selectedParticipantName = "";

    // UI элементы для управления категориями
    private Spinner spinnerCategory;
    private Button btnAddCategory, btnDeleteCategory;
    private TextView tvCategoryInfo;

    private List<Category> categories;
    private Category currentCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectors);

        repository = new CompetitionRepository(this);

        competitionId = getIntent().getIntExtra(Constants.EXTRA_COMPETITION_ID, -1);
        competitionName = getIntent().getStringExtra(Constants.EXTRA_COMPETITION_NAME);

        Log.d(TAG, "onCreate - competitionId: " + competitionId);
        Log.d(TAG, "onCreate - competitionName: " + competitionName);

        if (competitionId == -1) {
            Toast.makeText(this, "Ошибка: соревнование не выбрано", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadParticipants();
        loadCategories();

        btnStartAttempt.setOnClickListener(v -> {
            Log.d(TAG, "btnStartAttempt clicked, selectedParticipantId: " + selectedParticipantId);
            startJudging();
        });
        btnAddParticipant.setOnClickListener(v -> showParticipantBottomSheet());
        btnAddCategory.setOnClickListener(v -> showCategoryBottomSheet(null));
        btnDeleteCategory.setOnClickListener(v -> deleteCategory());
    }

    private void initViews() {
        setTitle("Параметры - " + competitionName);

        // Участник
        spinnerParticipant = findViewById(R.id.spinnerParticipant);
        btnStartAttempt = findViewById(R.id.btnStartAttempt);
        btnAddParticipant = findViewById(R.id.btnAddParticipant);
        txtNoParticipants = findViewById(R.id.txtNoParticipants);
        txtCompetitionInfo = findViewById(R.id.txtCompetitionInfo);

        txtCompetitionInfo.setText("Соревнование: " + competitionName);
        btnStartAttempt.setEnabled(false);

        // Категории
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        tvCategoryInfo = findViewById(R.id.tvCategoryInfo);

        btnDeleteCategory.setEnabled(false);
    }

    // ==================== УПРАВЛЕНИЕ УЧАСТНИКАМИ ====================

    private void loadParticipants() {
        participants = repository.getAllParticipants();
        Log.d(TAG, "loadParticipants - participants count: " + (participants != null ? participants.size() : 0));

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
                        Log.d(TAG, "Participant selected: ID=" + selectedParticipantId + ", Name=" + selectedParticipantName);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    btnStartAttempt.setEnabled(false);
                }
            });
        }
    }

    private void showParticipantBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_participant, null);
        bottomSheetDialog.setContentView(sheetView);

        TextInputEditText etName = sheetView.findViewById(R.id.etParticipantName);
        TextInputEditText etNickname = sheetView.findViewById(R.id.etParticipantNickname);
        TextInputEditText etBreed = sheetView.findViewById(R.id.etParticipantBreed);
        Spinner spinnerGender = sheetView.findViewById(R.id.spinnerGender);
        TextInputEditText etBirthDate = sheetView.findViewById(R.id.etParticipantBirthDate);
        TextInputEditText etMicrochip = sheetView.findViewById(R.id.etParticipantMicrochip);
        TextInputEditText etPedigree = sheetView.findViewById(R.id.etParticipantPedigree);
        TextInputEditText etQualification = sheetView.findViewById(R.id.etParticipantQualification);
        TextInputEditText etInstructor = sheetView.findViewById(R.id.etParticipantInstructor);
        Button btnCancel = sheetView.findViewById(R.id.btnCancelParticipant);
        Button btnSave = sheetView.findViewById(R.id.btnSaveParticipant);

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Введите имя участника", Toast.LENGTH_SHORT).show();
                return;
            }

            String gender = spinnerGender.getSelectedItemPosition() > 0 ? spinnerGender.getSelectedItem().toString() : null;

            addParticipant(
                    name,
                    etNickname.getText().toString().trim(),
                    etBreed.getText().toString().trim(),
                    gender,
                    etBirthDate.getText().toString().trim(),
                    etMicrochip.getText().toString().trim(),
                    etPedigree.getText().toString().trim(),
                    etQualification.getText().toString().trim(),
                    etInstructor.getText().toString().trim(),
                    bottomSheetDialog
            );
        });

        bottomSheetDialog.show();
    }

    private void addParticipant(String name, String nickname, String breed, String gender,
                                String birthDate, String microchipNumber, String pedigreeNumber,
                                String qualificationBookNumber, String instructorName,
                                BottomSheetDialog dialog) {
        new Thread(() -> {
            Participant participant = new Participant();
            participant.name = name;
            participant.nickname = nickname.isEmpty() ? null : nickname;
            participant.breed = breed.isEmpty() ? null : breed;
            participant.gender = gender;
            participant.birthDate = birthDate.isEmpty() ? null : birthDate;
            participant.microchipNumber = microchipNumber.isEmpty() ? null : microchipNumber;
            participant.pedigreeNumber = pedigreeNumber.isEmpty() ? null : pedigreeNumber;
            participant.qualificationBookNumber = qualificationBookNumber.isEmpty() ? null : qualificationBookNumber;
            participant.instructorName = instructorName.isEmpty() ? null : instructorName;

            long id = repository.insertParticipant(participant);

            if (id > 0) {
                List<Participant> currentParticipants = repository.getAllParticipants();
                CompetitionParticipant link = new CompetitionParticipant();
                link.competitionId = competitionId;
                link.participantId = (int) id;
                link.sortOrder = currentParticipants != null ? currentParticipants.size() : 0;
                repository.addParticipantToCompetition(link);
            }

            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(this, "Участник \"" + name + "\" добавлен", Toast.LENGTH_SHORT).show();
                    loadParticipants();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Ошибка при добавлении участника", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void startJudging() {
        Log.d(TAG, "startJudging called - selectedParticipantId: " + selectedParticipantId);

        if (selectedParticipantId == -1) {
            Toast.makeText(this, "Сначала выберите участника", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "Сначала создайте категорию", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] categoryNames = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            categoryNames[i] = categories.get(i).name;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите категорию")
                .setItems(categoryNames, (dialog, which) -> {
                    Category selectedCategory = categories.get(which);
                    Log.d(TAG, "Category selected: ID=" + selectedCategory.id + ", Name=" + selectedCategory.name);

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
        Log.d(TAG, "loadCategories - categories count: " + (categories != null ? categories.size() : 0));

        if (categories == null || categories.isEmpty()) {
            spinnerCategory.setVisibility(View.GONE);
            btnDeleteCategory.setEnabled(false);
            tvCategoryInfo.setText("Нет категорий. Нажмите кнопку \"Создать категорию\"");
        } else {
            spinnerCategory.setVisibility(View.VISIBLE);
            tvCategoryInfo.setText("Выберите категорию для просмотра:");

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
                        displayCategoryInfo(currentCategory);
                        btnDeleteCategory.setEnabled(true);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    btnDeleteCategory.setEnabled(false);
                }
            });

            if (!categories.isEmpty()) {
                currentCategory = categories.get(0);
                displayCategoryInfo(currentCategory);
                btnDeleteCategory.setEnabled(true);
            }
        }
    }

    private void displayCategoryInfo(Category category) {
        StringBuilder info = new StringBuilder();
        info.append("Название: ").append(category.name).append("\n");
        info.append("Лимит времени: ").append(category.timeLimit).append(" сек\n");
        info.append("Закладок: ").append(category.hidesCount).append("\n");
        info.append("Макс. балл: ").append(category.maxScore).append("\n\n");
        info.append("Штрафы:\n");

        List<PenaltyRule> rules = repository.getPenaltyRulesByCategory(category.id);
        if (rules.isEmpty()) {
            info.append("  - Нет штрафов");
        } else {
            for (PenaltyRule rule : rules) {
                info.append("  • ").append(rule.name).append(" (")
                        .append(rule.type.equals("flat") ? "фиксированный" : "прогрессивный")
                        .append("): ");
                List<Double> points = rule.getPoints();
                for (int i = 0; i < points.size(); i++) {
                    if (i > 0) info.append(", ");
                    info.append(points.get(i).intValue());
                }
                info.append("\n");
            }
        }

        tvCategoryInfo.setText(info.toString());
    }

    private void showCategoryBottomSheet(Category categoryToEdit) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_category, null);
        bottomSheetDialog.setContentView(sheetView);

        TextInputEditText etName = sheetView.findViewById(R.id.etCategoryName);
        TextInputEditText etTimeLimit = sheetView.findViewById(R.id.etTimeLimit);
        TextInputEditText etHidesCount = sheetView.findViewById(R.id.etHidesCount);
        TextInputEditText etMaxScore = sheetView.findViewById(R.id.etMaxScore);
        LinearLayout penaltiesContainer = sheetView.findViewById(R.id.penaltyRulesContainer);
        Button btnAddPenaltyRule = sheetView.findViewById(R.id.btnAddPenaltyRule);
        Button btnCancel = sheetView.findViewById(R.id.btnCancelCategory);
        Button btnSave = sheetView.findViewById(R.id.btnSaveCategory);

        List<PenaltyRuleInput> penaltyRulesList = new ArrayList<>();

        if (categoryToEdit != null) {
            etName.setText(categoryToEdit.name);
            etTimeLimit.setText(String.valueOf(categoryToEdit.timeLimit));
            etHidesCount.setText(String.valueOf(categoryToEdit.hidesCount));
            etMaxScore.setText(String.valueOf(categoryToEdit.maxScore));

            List<PenaltyRule> rules = repository.getPenaltyRulesByCategory(categoryToEdit.id);
            for (PenaltyRule rule : rules) {
                List<Integer> points = new ArrayList<>();
                for (double p : rule.getPoints()) {
                    points.add((int) p);
                }
                PenaltyRuleInput input = new PenaltyRuleInput(rule.name, rule.type, points);
                penaltyRulesList.add(input);
                addPenaltyRuleRow(penaltiesContainer, penaltyRulesList, input);
            }
        }

        if (penaltyRulesList.isEmpty()) {
            addPenaltyRuleRow(penaltiesContainer, penaltyRulesList, null);
        }

        btnAddPenaltyRule.setOnClickListener(v ->
                addPenaltyRuleRow(penaltiesContainer, penaltyRulesList, null));

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
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
            for (PenaltyRuleInput rule : penaltyRulesList) {
                if (rule.getName() != null && !rule.getName().isEmpty() &&
                        rule.getPoints() != null && !rule.getPoints().isEmpty()) {
                    validRules.add(rule);
                }
            }

            saveCategory(categoryToEdit, name, timeLimit, hidesCount, maxScore, validRules, bottomSheetDialog);
        });

        bottomSheetDialog.show();
    }

    private void addPenaltyRuleRow(LinearLayout container, List<PenaltyRuleInput> rulesList, PenaltyRuleInput existingRule) {
        View row = getLayoutInflater().inflate(R.layout.item_penalty_rule_input, container, false);

        TextInputEditText etRuleName = row.findViewById(R.id.etRuleName);
        Spinner spinnerRuleType = row.findViewById(R.id.spinnerRuleType);
        TextInputEditText etRulePoints = row.findViewById(R.id.etRulePoints);
        MaterialButton btnRemoveRule = row.findViewById(R.id.btnRemoveRule);

        final int position = container.getChildCount();

        if (existingRule != null) {
            etRuleName.setText(existingRule.getName());
            etRulePoints.setText(pointsToString(existingRule.getPoints()));
            spinnerRuleType.setSelection("progressive".equals(existingRule.getType()) ? 1 : 0);
        }

        btnRemoveRule.setOnClickListener(v -> {
            container.removeView(row);
            if (position < rulesList.size()) {
                rulesList.remove(position);
            }
        });

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                updatePenaltyRuleInList(rulesList, position, etRuleName.getText().toString(),
                        spinnerRuleType.getSelectedItemPosition(), etRulePoints.getText().toString());
            }
        };

        etRuleName.addTextChangedListener(watcher);
        etRulePoints.addTextChangedListener(watcher);

        spinnerRuleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                updatePenaltyRuleInList(rulesList, position, etRuleName.getText().toString(),
                        pos, etRulePoints.getText().toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        container.addView(row);
    }

    private void updatePenaltyRuleInList(List<PenaltyRuleInput> rulesList, int position, String name, int typePosition, String pointsStr) {
        while (rulesList.size() <= position) {
            rulesList.add(new PenaltyRuleInput());
        }

        PenaltyRuleInput rule = rulesList.get(position);
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

    private void saveCategory(Category existingCategory, String name, double timeLimit,
                              int hidesCount, int maxScore, List<PenaltyRuleInput> validRules,
                              BottomSheetDialog dialog) {
        new Thread(() -> {
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

            if (existingCategory == null) {
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
                    dialog.dismiss();
                });
            } else {
                existingCategory.name = name;
                existingCategory.timeLimit = timeLimit;
                existingCategory.hidesCount = hidesCount;
                existingCategory.maxScore = maxScore;
                repository.updateCategory(existingCategory);

                repository.deletePenaltyRulesByCategory(existingCategory.id);
                for (int i = 0; i < rulesForServer.size(); i++) {
                    Map<String, Object> ruleData = rulesForServer.get(i);
                    PenaltyRule rule = new PenaltyRule();
                    rule.categoryId = existingCategory.id;
                    rule.name = (String) ruleData.get("name");
                    rule.type = (String) ruleData.get("type");
                    rule.pointsJson = new com.google.gson.Gson().toJson(ruleData.get("points"));
                    rule.sequenceIndex = i + 1;
                    repository.insertPenaltyRule(rule);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                    loadCategories();
                    dialog.dismiss();
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