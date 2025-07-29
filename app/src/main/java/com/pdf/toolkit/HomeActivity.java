package com.pdf.toolkit;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // This correctly sets the title in the system-provided title bar.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Document");
        }

        // Find all the buttons
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView fileManagerCard = findViewById(R.id.card_file_manager);
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);

        // --- All your listeners, now complete and correct ---

        pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html"));
        
        uniToolsCard.setOnClickListener(v -> launchWebViewActivity("unitools.html"));

        allFilesCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });

        // This preserves your custom logic for the Scanner button.
        // This assumes you have created or will create a "ScannerActivity.java" file.
        scannerCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ScannerActivity.class);
            startActivity(intent);
        });
        
        // --- START: THIS IS THE NEW, WORKING CODE FOR THE FILE MANAGER ---
        fileManagerCard.setOnClickListener(v -> {
            // Create an Intent to open a location.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            // Get the path to the primary "Downloads" folder.
            String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            Uri uri = Uri.parse(downloadsPath);
            
            // Tell the Intent to open the folder at that location.
            intent.setDataAndType(uri, "*/*"); // The "*/*" means "show all file types"

            // It's a best practice to wrap this in a try-catch block
            // in case the user's phone doesn't have a default file manager.
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No file manager app found.", Toast.LENGTH_SHORT).show();
            }
        });
        // --- END: THIS IS THE NEW, WORKING CODE ---
    }

    private void launchWebViewActivity(String fileName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        // This is the corrected, safer way to pass the filename
        intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName);
        startActivity(intent);
    }
}
