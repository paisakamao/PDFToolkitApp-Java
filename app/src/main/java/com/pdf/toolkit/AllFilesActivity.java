package com.pdf.toolkit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class AllFilesActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_NAME = "com.pdf.toolkit.FILE_NAME"; // This is now a PATH

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        // Hide the default ActionBar (in case theme still enables it)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        PDFView pdfView = findViewById(R.id.pdfView);
        TextView titleText = findViewById(R.id.textViewTitlePdf);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(EXTRA_FILE_NAME);

        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);

            // Set title from file name
            if (titleText != null) {
                titleText.setText(file.getName());
            }

            if (file.exists()) {
                pdfView.fromFile(file)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .load();
            } else {
                Toast.makeText(this, "Error: File not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
        }
    }
}
