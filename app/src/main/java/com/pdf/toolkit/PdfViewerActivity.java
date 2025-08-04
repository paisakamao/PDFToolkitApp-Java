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

    // --- FIX #1: Use the single, consistent key from our other activities ---
    public static final String EXTRA_FILE_URI = "com.pdf.toolkit.FILE_URI";

    private PDFView pdfView;
    private Uri pdfUri;
    private boolean nightMode = false;

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

        // --- FIX #2: Correctly receive the URI as a String and parse it ---
        Intent intent = getIntent();
        String uriString = intent.getStringExtra(EXTRA_FILE_URI); // Get the String
        if (uriString != null && !uriString.isEmpty()) {
            pdfUri = Uri.parse(uriString); // Convert it to a Uri object

            // Set the toolbar title with the real filename
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getFileNameFromUri(pdfUri));
            }
            
            loadPdf(false); // Load the PDF
        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // This is your original, working method
    private void loadPdf(boolean nightModeEnabled) {
        if (pdfUri != null) {
            pdfView.fromUri(pdfUri)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .scrollHandle(new com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle(this))
                .spacing(10)
                .nightMode(nightModeEnabled)
                .load();
        }
    }

    // This is your original, working menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        return true;
    }

    // This is your original, working menu handler
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
            nightMode = !nightMode;
            loadPdf(nightMode);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This is your original share method, with one critical fix for modern Android
    private void sharePdf() {
        if (pdfUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            // --- FIX #3: This permission flag is required to share files securely ---
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"));
        }
    }

    // This is a robust helper method to get the filename from any type of Uri
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
