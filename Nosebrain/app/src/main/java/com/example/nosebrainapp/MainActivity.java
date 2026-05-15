package com.example.nosebrainapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.nosebrainapp.data.entity.Competition;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import com.example.nosebrainapp.utils.Constants;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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

        btnCreateCompetition.setOnClickListener(v -> showCreateCompetitionDialog());
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

    private void showCreateCompetitionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создание соревнования");

        // Создаём поле ввода
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Название соревнования");
        input.setPadding(50, 20, 50, 20);

        builder.setView(input);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                createCompetition(name);
            } else {
                Toast.makeText(MainActivity.this, "Введите название соревнования", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createCompetition(String name) {
        new Thread(() -> {
            Competition competition = new Competition();
            competition.name = name;
            competition.description = "";
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
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompetitions();
    }
}