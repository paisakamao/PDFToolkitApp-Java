package com.pdf.toolkit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Find all the buttons
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView fileManagerCard = findViewById(R.id.card_file_manager);
        // --- START: FIND THE NEW BUTTON ---
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);
        // --- END: FIND THE NEW BUTTON ---

        // Set the click listener for the PDF Tool button
        pdfToolCard.setOnClickListener(v -> {
            launchWebViewActivity("index.html");
        });

        // Set the click listener for the Scanner button
        scannerCard.setOnClickListener(v -> {
            launchWebViewActivity("scanner.html");
        });
        
        // Set the click listener for the "All Files" button
        allFilesCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });
        
        // Set the click listener for the "File Manager" button
        fileManagerCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            // This is a safer way to launch an external activity
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        // --- START: SET THE CLICK LISTENER FOR THE NEW BUTTON ---
        uniToolsCard.setOnClickListener(v -> {
            // Tell the app to launch the WebView with "unitools.html"
            launchWebViewActivity("unitools.html");
        });
        // --- END: SET THE CLICK LISTENER FOR THE NEW BUTTON ---
    }

    private void launchWebViewActivity(String fileName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName);
        startActivity(intent);
    }
}
