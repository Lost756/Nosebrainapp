package com.example.nosebrainapp;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nosebrainapp.data.entity.*;
import com.example.nosebrainapp.data.repository.CompetitionRepository;
import com.example.nosebrainapp.utils.Constants;
import java.util.*;

public class ResultsListActivity extends AppCompatActivity {

    private CompetitionRepository repository;
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private TextView txtNoResults;
    private TextView txtCompetitionName;

    private List<Category> categories;
    private List<Result> currentResults = new ArrayList<>();
    private ResultsAdapter adapter;

    private int competitionId;
    private String competitionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        repository = new CompetitionRepository(this);

        // Получаем ID соревнования из Intent
        competitionId = getIntent().getIntExtra(Constants.EXTRA_COMPETITION_ID, -1);
        competitionName = getIntent().getStringExtra(Constants.EXTRA_COMPETITION_NAME);

        if (competitionId == -1) {
            Toast.makeText(this, "Ошибка: соревнование не выбрано", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setTitle("Результаты - " + competitionName);
        loadCategories();

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && categories != null && position < categories.size()) {
                    loadResults(categories.get(position).id);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinnerCategoryResults);
        recyclerView = findViewById(R.id.recyclerViewResults);
        txtNoResults = findViewById(R.id.txtNoResults);
        txtCompetitionName = findViewById(R.id.txtCompetitionName);

        if (txtCompetitionName != null) {
            txtCompetitionName.setText("Соревнование: " + competitionName);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultsAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadCategories() {
        // Загружаем категории для конкретного соревнования
        categories = repository.getCategoriesByCompetition(competitionId);

        if (categories == null || categories.isEmpty()) {
            spinnerCategory.setVisibility(View.GONE);
            txtNoResults.setVisibility(View.VISIBLE);
            txtNoResults.setText("Нет категорий в этом соревновании");
        } else {
            spinnerCategory.setVisibility(View.VISIBLE);

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

            // Автоматически загружаем результаты первой категории
            if (!categories.isEmpty()) {
                loadResults(categories.get(0).id);
            }
        }
    }

    private void loadResults(int categoryId) {
        currentResults = repository.getResultsByCategory(categoryId);
        if (currentResults == null || currentResults.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            txtNoResults.setVisibility(View.VISIBLE);
            txtNoResults.setText("Нет результатов в этой категории");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            txtNoResults.setVisibility(View.GONE);
            adapter.setResults(currentResults);
        }
    }

    private class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {
        private List<Result> results = new ArrayList<>();

        public void setResults(List<Result> results) {
            this.results = results;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Result result = results.get(position);
            holder.participantName.setText(result.participantName);
            holder.timeText.setText(formatTime(result.time));
            holder.foundText.setText(result.foundItems + " / " + getHidesCountForCategory(result.categoryId));
            holder.penaltyText.setText(String.valueOf(result.penaltyScore));
            holder.scoreText.setText(String.valueOf(result.totalScore));
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        private String formatTime(double seconds) {
            int minutes = (int) (seconds / 60);
            double secs = seconds % 60;
            return String.format(Locale.getDefault(), "%02d:%05.2f", minutes, secs);
        }

        private int getHidesCountForCategory(int categoryId) {
            Category cat = repository.getCategoryById(categoryId);
            return cat != null ? cat.hidesCount : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView participantName, timeText, foundText, penaltyText, scoreText;

            ViewHolder(View itemView) {
                super(itemView);
                participantName = itemView.findViewById(R.id.participantName);
                timeText = itemView.findViewById(R.id.timeText);
                foundText = itemView.findViewById(R.id.foundText);
                penaltyText = itemView.findViewById(R.id.penaltyText);
                scoreText = itemView.findViewById(R.id.scoreText);
            }
        }
    }
}