package com.pdf.toolkit;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.pdf.toolkit.AllFilesActivity;


public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView fileManagerCard = findViewById(R.id.card_file_manager);
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);

        pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html"));

        scannerCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ScannerActivity.class);
            startActivity(intent);
        });
        
        uniToolsCard.setOnClickListener(v -> launchWebViewActivity("unitools.html"));
        
        allFilesCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });
        
        fileManagerCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });
    }

    private void launchWebViewActivity(String fileName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra("EXTRA_HTML_FILE", fileName);
        startActivity(intent);
    }
}
