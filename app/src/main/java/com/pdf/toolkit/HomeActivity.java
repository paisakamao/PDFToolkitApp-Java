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

        // Find the "Pdf Tool" button by its ID
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView fileManagerCard = findViewById(R.id.card_file_manager);

        // Set a click listener
        pdfToolCard.setOnClickListener(v -> {
            // Launch the WebView with "index.html"
            launchWebViewActivity("index.html");
        });

        // Set the click listener for the new Scanner button
        scannerCard.setOnClickListener(v -> {
            // This is a placeholder for now, you can create scanner.html later
            launchWebViewActivity("scanner.html");
        });
        
        // Set the click listener for the "All Files" button
        allFilesCard.setOnClickListener(v -> {
            // Launch our new native AllFilesActivity
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });
        
        // Set the click listener for the "File Manager" button
        fileManagerCard.setOnClickListener(v -> {
            // Launch the phone's built-in file manager
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivity(intent);
        });
    }
    }
}
