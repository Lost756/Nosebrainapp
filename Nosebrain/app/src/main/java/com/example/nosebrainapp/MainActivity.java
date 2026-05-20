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
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CompetitionRepository repository;
    private Spinner spinnerCompetition;
    private Button btnStartJudging, btnViewResults, btnCreateCompetition;
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

        btnCreateCompetition.setOnClickListener(v -> showCompetitionBottomSheet());
    }

    private void initViews() {
        spinnerCompetition = findViewById(R.id.spinnerCompetition);
        btnStartJudging = findViewById(R.id.btnStartJudging);
        btnViewResults = findViewById(R.id.btnViewResults);
        btnCreateCompetition = findViewById(R.id.btnCreateCompetition);
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

    private void showCompetitionBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_competition, null);
        bottomSheetDialog.setContentView(sheetView);

        TextInputEditText etName = sheetView.findViewById(R.id.etCompetitionName);
        TextInputEditText etDescription = sheetView.findViewById(R.id.etCompetitionDescription);
        TextInputEditText etStartDate = sheetView.findViewById(R.id.etStartDate);
        TextInputEditText etEndDate = sheetView.findViewById(R.id.etEndDate);
        Spinner spinnerJudge = sheetView.findViewById(R.id.spinnerJudge);
        Spinner spinnerSecretary = sheetView.findViewById(R.id.spinnerSecretary);
        Button btnCancel = sheetView.findViewById(R.id.btnCancelCompetition);
        Button btnSave = sheetView.findViewById(R.id.btnSaveCompetition);

        // Загружаем список пользователей для спиннеров
        List<com.example.nosebrainapp.data.entity.User> allUsers = repository.getAllUsers();

        // Фильтруем судей и секретарей
        List<com.example.nosebrainapp.data.entity.User> judges = new ArrayList<>();
        List<com.example.nosebrainapp.data.entity.User> secretaries = new ArrayList<>();

        for (com.example.nosebrainapp.data.entity.User user : allUsers) {
            if (user.role.equals("judge") || user.role.equals("admin")) {
                judges.add(user);
            }
            if (user.role.equals("secretary") || user.role.equals("admin")) {
                secretaries.add(user);
            }
        }

        // Создаем адаптеры для спиннеров
        ArrayAdapter<com.example.nosebrainapp.data.entity.User> judgeAdapter = new ArrayAdapter<com.example.nosebrainapp.data.entity.User>(
                this, android.R.layout.simple_spinner_item, judges) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                com.example.nosebrainapp.data.entity.User user = getItem(position);
                view.setText(user.username);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                com.example.nosebrainapp.data.entity.User user = getItem(position);
                view.setText(user.username);
                return view;
            }
        };
        judgeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerJudge.setAdapter(judgeAdapter);

        ArrayAdapter<com.example.nosebrainapp.data.entity.User> secretaryAdapter = new ArrayAdapter<com.example.nosebrainapp.data.entity.User>(
                this, android.R.layout.simple_spinner_item, secretaries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                com.example.nosebrainapp.data.entity.User user = getItem(position);
                view.setText(user.username);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                com.example.nosebrainapp.data.entity.User user = getItem(position);
                view.setText(user.username);
                return view;
            }
        };
        secretaryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSecretary.setAdapter(secretaryAdapter);

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название соревнования", Toast.LENGTH_SHORT).show();
                return;
            }

            String description = etDescription.getText().toString().trim();
            String startDate = etStartDate.getText().toString().trim();
            String endDate = etEndDate.getText().toString().trim();

            int judgeId = -1;
            int secretaryId = -1;

            if (spinnerJudge.getSelectedItem() != null) {
                judgeId = ((com.example.nosebrainapp.data.entity.User) spinnerJudge.getSelectedItem()).id;
            }
            if (spinnerSecretary.getSelectedItem() != null) {
                secretaryId = ((com.example.nosebrainapp.data.entity.User) spinnerSecretary.getSelectedItem()).id;
            }

            createCompetition(name, description, startDate, endDate, judgeId, secretaryId, bottomSheetDialog);
        });

        bottomSheetDialog.show();
    }

    private void createCompetition(String name, String description, String startDate, String endDate,
                                   int judgeId, int secretaryId, BottomSheetDialog dialog) {
        new Thread(() -> {
            try {
                Competition competition = new Competition();
                competition.name = name;
                competition.description = description.isEmpty() ? null : description;
                competition.startDate = startDate.isEmpty() ? null : startDate;
                competition.endDate = endDate.isEmpty() ? null : endDate;
                competition.isActive = true;

                long id = repository.insertCompetition(competition);

                // Если выбран судья, обновляем его
                if (judgeId != -1 && judgeId != 0) {
                    com.example.nosebrainapp.data.entity.User judge = repository.getUserById(judgeId);
                    if (judge != null) {
                        judge.competitionId = (int) id;
                        repository.updateUser(judge);
                    }
                }

                // Если выбран секретарь, обновляем его
                if (secretaryId != -1 && secretaryId != 0) {
                    com.example.nosebrainapp.data.entity.User secretary = repository.getUserById(secretaryId);
                    if (secretary != null) {
                        secretary.competitionId = (int) id;
                        repository.updateUser(secretary);
                    }
                }

                runOnUiThread(() -> {
                    if (id > 0) {
                        Toast.makeText(MainActivity.this, "Соревнование \"" + name + "\" создано", Toast.LENGTH_SHORT).show();
                        loadCompetitions();
                        dialog.dismiss();
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