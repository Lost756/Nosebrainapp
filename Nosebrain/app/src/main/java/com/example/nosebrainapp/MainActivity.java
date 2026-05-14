package com.example.nosebrainapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nosebrainapp.data.DatabaseSeeder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Заполняем базу тестовыми данными при первом запуске
        DatabaseSeeder.seedIfEmpty(this);

        Button btnStartJudging = findViewById(R.id.btnStartJudging);
        Button btnViewResults = findViewById(R.id.btnViewResults);

        btnStartJudging.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectorsActivity.class);
            startActivity(intent);
        });

        btnViewResults.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ResultsListActivity.class);
            startActivity(intent);
        });
    }
}