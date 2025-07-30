package com.pdf.toolkit;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Get the list of scanned page URIs from the Intent
        ArrayList<String> scannedPages = getIntent().getStringArrayListExtra("scanned_pages");

        // TODO: We will add the logic to display these pages in the next step.
    }
}
