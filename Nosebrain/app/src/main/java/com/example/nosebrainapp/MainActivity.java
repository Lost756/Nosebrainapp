package com.example.nosebrainapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.nosebrainapp.data.entity.Competition;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import com.example.nosebrainapp.sync.SyncManager;
import com.example.nosebrainapp.utils.Constants;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CompetitionRepository repository;
    private Spinner spinnerCompetition;
    private Button btnStartJudging, btnViewResults, btnCreateCompetition, btnSync;
    private TextView txtNoCompetitions;

    private List<Competition> competitions;
    private int selectedCompetitionId = -1;
    private String selectedCompetitionName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new CompetitionRepository(this);

        initViews();
        loadCompetitions();

        btnStartJudging.setOnClickListener(v -> {
            if (selectedCompetitionId != -1) {
                Intent intent = new Intent(MainActivity.this, SelectorsActivity.class);
                intent.putExtra(Constants.EXTRA_COMPETITION_ID, selectedCompetitionId);
                intent.putExtra(Constants.EXTRA_COMPETITION_NAME, selectedCompetitionName);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Выберите соревнование", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewResults.setOnClickListener(v -> {
            if (selectedCompetitionId != -1) {
                Intent intent = new Intent(MainActivity.this, ResultsListActivity.class);
                intent.putExtra(Constants.EXTRA_COMPETITION_ID, selectedCompetitionId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Выберите соревнование", Toast.LENGTH_SHORT).show();
            }
        });

        btnCreateCompetition.setOnClickListener(v -> showCreateCompetitionDialog());

        // В MainActivity.java обработчик кнопки синхронизации
        btnSync.setOnClickListener(v -> {
            if (selectedCompetitionId != -1) {
                // Показываем прогресс
                Toast.makeText(this, "Начинаем полную синхронизацию...", Toast.LENGTH_SHORT).show();

                SyncManager syncManager = new SyncManager(this);
                syncManager.syncAllData(selectedCompetitionId, new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "✓ " + message, Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "✗ " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Сначала выберите соревнование", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        spinnerCompetition = findViewById(R.id.spinnerCompetition);
        btnStartJudging = findViewById(R.id.btnStartJudging);
        btnViewResults = findViewById(R.id.btnViewResults);
        btnCreateCompetition = findViewById(R.id.btnCreateCompetition);
        btnSync = findViewById(R.id.btnSync);
        txtNoCompetitions = findViewById(R.id.txtNoCompetitions);

        btnStartJudging.setEnabled(false);
        btnViewResults.setEnabled(false);
    }

    private void loadCompetitions() {
        competitions = repository.getCompetitions();

        if (competitions == null || competitions.isEmpty()) {
            spinnerCompetition.setVisibility(View.GONE);
            txtNoCompetitions.setVisibility(View.VISIBLE);
            btnStartJudging.setEnabled(false);
            btnViewResults.setEnabled(false);
        } else {
            spinnerCompetition.setVisibility(View.VISIBLE);
            txtNoCompetitions.setVisibility(View.GONE);

            ArrayAdapter<Competition> adapter = new ArrayAdapter<Competition>(this,
                    android.R.layout.simple_spinner_item, competitions) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    view.setText(competitions.get(position).name);
                    return view;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                    view.setText(competitions.get(position).name);
                    return view;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCompetition.setAdapter(adapter);

            spinnerCompetition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < competitions.size()) {
                        selectedCompetitionId = competitions.get(position).id;
                        selectedCompetitionName = competitions.get(position).name;
                        btnStartJudging.setEnabled(true);
                        btnViewResults.setEnabled(true);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    btnStartJudging.setEnabled(false);
                    btnViewResults.setEnabled(false);
                }
            });
        }
    }

    private void showCreateCompetitionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создание соревнования");

        // Создаём кастомный layout для диалога
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        // Название соревнования (обязательное поле)
        final TextInputEditText inputName = new TextInputEditText(this);
        inputName.setHint("Название соревнования *");
        layout.addView(inputName);

        // Описание
        final TextInputEditText inputDescription = new TextInputEditText(this);
        inputDescription.setHint("Описание");
        layout.addView(inputDescription);

        // Дата начала
        final TextInputEditText inputStartDate = new TextInputEditText(this);
        inputStartDate.setHint("Дата начала (ГГГГ-ММ-ДД)");
        layout.addView(inputStartDate);

        // Дата окончания
        final TextInputEditText inputEndDate = new TextInputEditText(this);
        inputEndDate.setHint("Дата окончания (ГГГГ-ММ-ДД)");
        layout.addView(inputEndDate);

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            if (!name.isEmpty()) {
                createCompetition(
                        name,
                        inputDescription.getText().toString().trim(),
                        inputStartDate.getText().toString().trim(),
                        inputEndDate.getText().toString().trim()
                );
            } else {
                Toast.makeText(MainActivity.this, "Введите название соревнования", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createCompetition(String name, String description, String startDate, String endDate) {
        new Thread(() -> {
            try {
                Competition competition = new Competition();
                competition.name = name;
                competition.description = description.isEmpty() ? null : description;
                competition.startDate = startDate.isEmpty() ? null : startDate;
                competition.endDate = endDate.isEmpty() ? null : endDate;
                competition.isActive = true;

                long id = repository.insertCompetition(competition);

                runOnUiThread(() -> {
                    if (id > 0) {
                        Toast.makeText(MainActivity.this, "Соревнование \"" + name + "\" создано", Toast.LENGTH_SHORT).show();
                        loadCompetitions();
                    } else {
                        Toast.makeText(MainActivity.this, "Ошибка при создании соревнования", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error creating competition: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompetitions();
    }
}