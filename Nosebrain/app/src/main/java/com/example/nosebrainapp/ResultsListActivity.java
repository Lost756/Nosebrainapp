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
import java.util.*;

public class ResultsListActivity extends AppCompatActivity {

    private CompetitionRepository repository;
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private TextView txtNoResults;

    private List<Category> categories;
    private List<Result> currentResults = new ArrayList<>();
    private ResultsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        repository = new CompetitionRepository(this);

        spinnerCategory = findViewById(R.id.spinnerCategoryResults);
        recyclerView = findViewById(R.id.recyclerViewResults);
        txtNoResults = findViewById(R.id.txtNoResults);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultsAdapter();
        recyclerView.setAdapter(adapter);

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

    private void loadCategories() {
        List<Competition> competitions = repository.getCompetitions();
        if (!competitions.isEmpty()) {
            categories = repository.getCategoriesByCompetition(competitions.get(0).id);
            ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
        }
    }

    private void loadResults(int categoryId) {
        currentResults = repository.getResultsByCategory(categoryId);
        if (currentResults.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            txtNoResults.setVisibility(View.VISIBLE);
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