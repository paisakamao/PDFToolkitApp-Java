package com.pdf.toolkit;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Document");
        }

        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView fileManagerCard = findViewById(R.id.card_file_manager); // This is now the "Recent Files" button
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);

        pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html"));
        uniToolsCard.setOnClickListener(v -> launchWebViewActivity("unitools.html"));

        allFilesCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });

        scannerCard.setOnClickListener(v -> {
            // This assumes you will create a ScannerActivity.java file
            Intent intent = new Intent(HomeActivity.this, ScannerActivity.class);
            startActivity(intent);
        });
        
        // --- START: THIS IS THE UPDATED LISTENER ---
        // The "File Manager" card now opens the "Recent Files" screen.
        fileManagerCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, RecentFilesActivity.class);
            startActivity(intent);
        });
        // --- END: THIS IS THE UPDATED LISTENER ---
    }

    private void launchWebViewActivity(String fileName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName);
        startActivity(intent);
    }
}
