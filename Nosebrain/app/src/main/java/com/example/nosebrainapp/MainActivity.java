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
import com.example.nosebrainapp.sync.SyncDownloadManager;
import com.example.nosebrainapp.utils.Constants;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import android.app.ProgressDialog;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CompetitionRepository repository;
    private Spinner spinnerCompetition;
    private Button btnStartJudging, btnViewResults, btnCreateCompetition, btnSync, btnDownloadFromServer;
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

        btnSync.setOnClickListener(v -> {
            if (selectedCompetitionId != -1) {
                Toast.makeText(this, "Начинаем полную синхронизацию...", Toast.LENGTH_SHORT).show();

                SyncManager syncManager = new SyncManager(this);
                syncManager.syncAllData(selectedCompetitionId, new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "✓ " + message, Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "✗ " + error, Toast.LENGTH_LONG).show());
                    }
                });
            } else {
                Toast.makeText(this, "Сначала выберите соревнование", Toast.LENGTH_SHORT).show();
            }
        });

        btnDownloadFromServer.setOnClickListener(v -> showDownloadDialog());
    }

    private void initViews() {
        spinnerCompetition = findViewById(R.id.spinnerCompetition);
        btnStartJudging = findViewById(R.id.btnStartJudging);
        btnViewResults = findViewById(R.id.btnViewResults);
        btnCreateCompetition = findViewById(R.id.btnCreateCompetition);
        btnSync = findViewById(R.id.btnSync);
        btnDownloadFromServer = findViewById(R.id.btnDownloadFromServer);
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

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        final TextInputEditText inputName = new TextInputEditText(this);
        inputName.setHint("Название соревнования *");
        layout.addView(inputName);

        final TextInputEditText inputDescription = new TextInputEditText(this);
        inputDescription.setHint("Описание");
        layout.addView(inputDescription);

        final TextInputEditText inputStartDate = new TextInputEditText(this);
        inputStartDate.setHint("Дата начала (ГГГГ-ММ-ДД)");
        layout.addView(inputStartDate);

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

    private void showDownloadDialog() {
        ProgressDialog loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Загрузка списка данных...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        SyncDownloadManager downloadManager = new SyncDownloadManager(this);
        downloadManager.fetchAllData(new SyncDownloadManager.DownloadCallback() {
            @Override
            public void onSuccess(SyncDownloadManager.ServerData data) {
                // Уже в UI потоке, так как mainHandler.post используется
                loadingDialog.dismiss();
                if (data.competitions.isEmpty() && data.categories.isEmpty() && data.participants.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Нет данных на сервере", Toast.LENGTH_LONG).show();
                } else {
                    showSelectionDialog(data);
                }
            }

            @Override
            public void onError(String error) {
                // Уже в UI потоке
                loadingDialog.dismiss();
                Toast.makeText(MainActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Download error: " + error);
            }
        });
    }

    private void showSelectionDialog(SyncDownloadManager.ServerData data) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sync_download, null);

        LinearLayout competitionsContainer = dialogView.findViewById(R.id.competitionsContainer);
        LinearLayout categoriesContainer = dialogView.findViewById(R.id.categoriesContainer);
        LinearLayout participantsContainer = dialogView.findViewById(R.id.participantsContainer);

        Button btnSelectAll = dialogView.findViewById(R.id.btnSelectAll);
        Button btnDeselectAll = dialogView.findViewById(R.id.btnDeselectAll);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnDownload = dialogView.findViewById(R.id.btnDownload);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView progressText = dialogView.findViewById(R.id.progressText);

        List<Integer> selectedCompetitions = new ArrayList<>();
        List<Integer> selectedCategories = new ArrayList<>();
        List<Integer> selectedParticipants = new ArrayList<>();

        List<CheckBox> competitionCheckboxes = new ArrayList<>();
        List<CheckBox> categoryCheckboxes = new ArrayList<>();
        List<CheckBox> participantCheckboxes = new ArrayList<>();

        // Заполняем соревнования
        for (int i = 0; i < data.competitions.size(); i++) {
            SyncDownloadManager.CompetitionData cd = data.competitions.get(i);
            CheckBox cb = new CheckBox(this);
            String status = cd.isNew ? "" : " (уже есть)";
            cb.setText(cd.name + status);
            if (cd.isNew) {
                cb.setChecked(true);
                selectedCompetitions.add(i);
            } else {
                cb.setEnabled(false);
            }
            final int index = i;
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedCompetitions.contains(index)) selectedCompetitions.add(index);
                } else {
                    selectedCompetitions.remove((Integer) index);
                }
            });
            competitionsContainer.addView(cb);
            competitionCheckboxes.add(cb);
        }

        // Заполняем категории
        for (int i = 0; i < data.categories.size(); i++) {
            SyncDownloadManager.CategoryData cd = data.categories.get(i);
            CheckBox cb = new CheckBox(this);
            String status = cd.isNew ? "" : " (уже есть)";
            cb.setText(cd.name + status);
            if (cd.isNew) {
                cb.setChecked(true);
                selectedCategories.add(i);
            } else {
                cb.setEnabled(false);
            }
            final int index = i;
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedCategories.contains(index)) selectedCategories.add(index);
                } else {
                    selectedCategories.remove((Integer) index);
                }
            });
            categoriesContainer.addView(cb);
            categoryCheckboxes.add(cb);
        }

        // Заполняем участников
        for (int i = 0; i < data.participants.size(); i++) {
            SyncDownloadManager.ParticipantData pd = data.participants.get(i);
            CheckBox cb = new CheckBox(this);
            String displayName = pd.name;
            if (pd.nickname != null && !pd.nickname.isEmpty()) {
                displayName += " (" + pd.nickname + ")";
            }
            String status = pd.isNew ? "" : " (уже есть)";
            cb.setText(displayName + status);
            if (pd.isNew) {
                cb.setChecked(true);
                selectedParticipants.add(i);
            } else {
                cb.setEnabled(false);
            }
            final int index = i;
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedParticipants.contains(index)) selectedParticipants.add(index);
                } else {
                    selectedParticipants.remove((Integer) index);
                }
            });
            participantsContainer.addView(cb);
            participantCheckboxes.add(cb);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnSelectAll.setOnClickListener(v -> {
            for (CheckBox cb : competitionCheckboxes) if (cb.isEnabled()) cb.setChecked(true);
            for (CheckBox cb : categoryCheckboxes) if (cb.isEnabled()) cb.setChecked(true);
            for (CheckBox cb : participantCheckboxes) if (cb.isEnabled()) cb.setChecked(true);
        });

        btnDeselectAll.setOnClickListener(v -> {
            for (CheckBox cb : competitionCheckboxes) cb.setChecked(false);
            for (CheckBox cb : categoryCheckboxes) cb.setChecked(false);
            for (CheckBox cb : participantCheckboxes) cb.setChecked(false);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDownload.setOnClickListener(v -> {
            if (selectedCompetitions.isEmpty() && selectedCategories.isEmpty() && selectedParticipants.isEmpty()) {
                Toast.makeText(MainActivity.this, "Выберите данные для загрузки", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            btnDownload.setEnabled(false);
            btnSelectAll.setEnabled(false);
            btnDeselectAll.setEnabled(false);

            SyncDownloadManager downloadManager = new SyncDownloadManager(MainActivity.this);
            downloadManager.importSelectedData(data, selectedCompetitions, selectedCategories, selectedParticipants,
                    new SyncDownloadManager.ImportCallback() {
                        @Override
                        public void onSuccess(String message) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                progressText.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                loadCompetitions();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                progressText.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                                btnDownload.setEnabled(true);
                                btnSelectAll.setEnabled(true);
                                btnDeselectAll.setEnabled(true);
                            });
                        }

                        @Override
                        public void onProgress(String message) {
                            runOnUiThread(() -> progressText.setText(message));
                        }
                    });
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompetitions();
    }
}