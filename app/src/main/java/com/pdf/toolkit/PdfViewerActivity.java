package com.pdf.toolkit;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.barteksc.pdfviewer.PDFView;
import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    // --- THIS IS THE SINGLE, OFFICIAL KEY WE WILL USE EVERYWHERE ---
    public static final String EXTRA_FILE_URI = "com.pdf.toolkit.FILE_URI";

    private PDFView pdfView;
    private Uri pdfUri;
    private boolean isNightMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        pdfView = findViewById(R.id.pdfView);

        // --- THIS IS THE CORRECTED LOGIC ---
        Intent intent = getIntent();
        String uriString = intent.getStringExtra(EXTRA_FILE_URI); // Correctly get the String

        if (uriString != null && !uriString.isEmpty()) {
            pdfUri = Uri.parse(uriString); // Convert the String back to a Uri

            // Set the toolbar's title to the real filename
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getFileNameFromUri(pdfUri));
            }
            
            // Load the PDF from the now valid Uri
            loadPdf(isNightMode);
        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadPdf(boolean nightModeEnabled) {
        if (pdfUri != null) {
            pdfView.fromUri(pdfUri)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .defaultPage(0)
                .nightMode(nightModeEnabled)
                .load();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_share) {
            sharePdf();
            return true;
        } else if (id == R.id.action_toggle_night_mode) {
            isNightMode = !isNightMode;
            loadPdf(isNightMode);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sharePdf() {
        if (pdfUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            // Grant permission for the receiving app to read the file
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
        }
    }
    
    private String getFileNameFromUri(Uri uri) {
        String fileName = "Document";
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) { fileName = cursor.getString(nameIndex); }
                }
            }
        } else if (scheme != null && scheme.equals("file")) {
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }
}
