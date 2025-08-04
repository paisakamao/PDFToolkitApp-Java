package com.pdf.toolkit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.barteksc.pdfviewer.PDFView;

public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_URI = "extra_file_uri";

    private PDFView pdfView;
    private Uri pdfUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        pdfView = findViewById(R.id.pdfView);

        // Correct extra key usage
        pdfUri = getIntent().getParcelableExtra(EXTRA_FILE_URI);

        if (pdfUri != null) {
            Log.d("PdfViewerActivity", "Received URI: " + pdfUri.toString());
            loadPdf(false);
        } else {
            Log.e("PdfViewerActivity", "PDF URI is null! Make sure you passed the intent extra correctly.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_toggle_night_mode) {
            loadPdf(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadPdf(boolean nightMode) {
        if (pdfUri != null) {
            pdfView.fromUri(pdfUri)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .nightMode(nightMode)
                    .load();
        }
    }
}
