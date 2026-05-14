package com.example.nosebrainapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import com.example.nosebrainapp.utils.Constants;
import java.util.List;

public class SelectorsActivity extends AppCompatActivity {

    private CompetitionRepository repository;
    private Spinner spinnerCompetition, spinnerCategory, spinnerParticipant;
    private Button btnStartAttempt;
    private TextView txtNoParticipants;

    private List<Competition> competitions;
    private List<Category> categories;
    private List<Participant> participants;

    private int selectedCompetitionId = -1;
    private int selectedCategoryId = -1;
    private int selectedParticipantId = -1;
    private String selectedParticipantName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectors);

        repository = new CompetitionRepository(this);

        spinnerCompetition = findViewById(R.id.spinnerCompetition);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerParticipant = findViewById(R.id.spinnerParticipant);
        btnStartAttempt = findViewById(R.id.btnStartAttempt);
        txtNoParticipants = findViewById(R.id.txtNoParticipants);

        loadCompetitions();

        // Исправленный OnItemSelectedListener для Competition
        spinnerCompetition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && competitions != null && position < competitions.size()) {
                    selectedCompetitionId = competitions.get(position).id;
                    loadCategories(selectedCompetitionId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });

        // Исправленный OnItemSelectedListener для Category
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && categories != null && position < categories.size()) {
                    selectedCategoryId = categories.get(position).id;
                    loadParticipants(selectedCompetitionId, selectedCategoryId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });

        // Исправленный OnItemSelectedListener для Participant
        spinnerParticipant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && participants != null && position < participants.size()) {
                    selectedParticipantId = participants.get(position).id;
                    selectedParticipantName = participants.get(position).name;
                    btnStartAttempt.setEnabled(true);
                } else {
                    btnStartAttempt.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                btnStartAttempt.setEnabled(false);
            }
        });

        btnStartAttempt.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(SelectorsActivity.this, JudgeActivity.class);
            intent.putExtra(Constants.EXTRA_CATEGORY_ID, selectedCategoryId);
            intent.putExtra(Constants.EXTRA_PARTICIPANT_ID, selectedParticipantId);
            intent.putExtra(Constants.EXTRA_PARTICIPANT_NAME, selectedParticipantName);
            startActivity(intent);
        });
    }

    private void loadCompetitions() {
        competitions = repository.getCompetitions();
        if (competitions != null && !competitions.isEmpty()) {
            ArrayAdapter<Competition> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, competitions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCompetition.setAdapter(adapter);
        }
    }

    private void loadCategories(int competitionId) {
        categories = repository.getCategoriesByCompetition(competitionId);
        if (categories != null && !categories.isEmpty()) {
            ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
        }
    }

    private void loadParticipants(int competitionId, int categoryId) {
        participants = repository.getAvailableParticipants(competitionId);

        if (participants == null || participants.isEmpty()) {
            spinnerParticipant.setVisibility(View.GONE);
            txtNoParticipants.setVisibility(View.VISIBLE);
            btnStartAttempt.setEnabled(false);
        } else {
            spinnerParticipant.setVisibility(View.VISIBLE);
            txtNoParticipants.setVisibility(View.GONE);
            ArrayAdapter<Participant> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, participants);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerParticipant.setAdapter(adapter);
        }
    }
}