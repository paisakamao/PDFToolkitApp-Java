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

        // --- START: THIS IS THE FIX ---
        // We now set the title in the system-provided title bar.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Document");
        }
        // --- END: THIS IS THE FIX ---

        // The rest of your code for finding and clicking buttons is correct.
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        // ... find other cards ...
        
        pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html"));
        // ... set other listeners ...
    }

    private void launchWebViewActivity(String fileName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName);
        startActivity(intent);
    }
}
