package com.pdf.toolkit;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_URI = "com.pdf.toolkit.FILE_URI";

    private TextView pageIndicator;  // 🔹 Add this
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        pdfView = findViewById(R.id.pdfView);
        pageIndicator = findViewById(R.id.pageIndicator); // 🔹 Reference the new TextView

        Intent intent = getIntent();
        String uriString = intent.getStringExtra(EXTRA_FILE_URI);

        if (uriString != null && !uriString.isEmpty()) {
            Uri fileUri = Uri.parse(uriString);

            String fileName = getFileNameFromUri(fileUri);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(fileName);
            }

            pdfView.fromUri(fileUri)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .defaultPage(0)
                .enableDoubletap(true)
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        // 🔹 Update page indicator
                        pageIndicator.setText("Page " + (page + 1) + " / " + pageCount);
                    }
                })
                .load();

        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Document";
        String scheme = uri.getScheme();

        if ("content".equals(scheme)) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        } else if ("file".equals(scheme)) {
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }
}
