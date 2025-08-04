package com.pdf.toolkit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.barteksc.pdfviewer.PDFView;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;
    private Uri pdfUri;
    private boolean nightMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pdfView = findViewById(R.id.pdfView);

        // Get PDF Uri
        pdfUri = getIntent().getParcelableExtra("pdfUri");
        if (pdfUri != null) {
            loadPdf(false);
        }
    }

    private void loadPdf(boolean nightModeEnabled) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    private void sharePdf() {
        if (pdfUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"));
        }
    }
}
