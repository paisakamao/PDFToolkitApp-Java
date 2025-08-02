package com.pdf.toolkit;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.barteksc.pdfviewer.PDFView;
import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_URI = "com.pdf.toolkit.FILE_URI";

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

        PDFView pdfView = findViewById(R.id.pdfView);
        Intent intent = getIntent();
        String uriString = intent.getStringExtra(EXTRA_FILE_URI);

        if (uriString != null && !uriString.isEmpty()) {
            Uri fileUri = Uri.parse(uriString);
            
            // --- THIS IS THE FINAL, CORRECTED LOGIC ---
            // It correctly gets the filename and sets it as the toolbar's title.
            String fileName = getFileNameFromUri(fileUri);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(fileName);
            }
            
            pdfView.fromUri(fileUri)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .defaultPage(0)
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

    // This is the robust helper method that handles both types of URIs
    private String getFileNameFromUri(Uri uri) {
        String fileName = "Document";
        String scheme = uri.getScheme();

        if (scheme != null && scheme.equals("content")) {
            // This handles URIs from the MediaStore (like from the scanner)
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        } else if (scheme != null && scheme.equals("file")) {
            // This handles URIs from the All Files list (legacy file paths)
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }
}
