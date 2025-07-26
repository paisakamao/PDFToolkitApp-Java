package com.pdf.toolkit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_NAME = "com.pdf.toolkit.FILE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        PDFView pdfView = findViewById(R.id.pdfView);

        Intent intent = getIntent();
        String fileName = intent.getStringExtra(EXTRA_FILE_NAME);

        if (fileName != null && !fileName.isEmpty()) {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            if (file.exists()) {
                pdfView.fromFile(file)
                        .enableSwipe(true) // allow changing pages with swipe
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
