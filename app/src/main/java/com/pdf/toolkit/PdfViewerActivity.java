package com.pdf.toolkit;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import java.io.File;

// MODIFIED: Implemented the two listeners for page events
public class PdfViewerActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener {

    public static final String EXTRA_FILE_URI = "com.pdf.toolkit.FILE_URI";

    private PDFView pdfView;
    private Uri pdfUri;
    private TextView pageIndicator; // Added variable for the indicator
    private int totalPages = 0;     // Added variable to store total pages

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // MODIFIED: Set your custom back arrow icon
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }

        pdfView = findViewById(R.id.pdfView);
        // MODIFIED: Initialize the TextView for the page indicator
        pageIndicator = findViewById(R.id.pageIndicator);

        Intent intent = getIntent();
        String uriString = intent.getStringExtra(EXTRA_FILE_URI);

        if (uriString != null && !uriString.isEmpty()) {
            pdfUri = Uri.parse(uriString);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getFileNameFromUri(pdfUri));
            }
            loadPdf();
        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadPdf() {
        if (pdfUri != null) {
            pdfView.fromUri(pdfUri)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .pageSnap(false) // Set to false for smoother scrolling
                .autoSpacing(false)
                .pageFling(true)
                .spacing(10) // Your page break spacing
                // MODIFIED: Added listeners and removed the default scroll handle
                .onLoad(this)
                .onPageChange(this)
                .scrollHandle(null) // This removes the old indicator
                .load();
        }
    }

    // --- NEW METHODS TO HANDLE PAGE INDICATOR ---

    /**
     * Called when the PDF is finished loading.
     * We get the total page count here.
     */
    @Override
    public void loadComplete(int nbPages) {
        this.totalPages = nbPages;
        // Update the indicator with the first page number
        onPageChanged(0, totalPages);
    }

    /**
     * Called every time the user scrolls to a new page.
     * We update our TextView here.
     */
    @Override
    public void onPageChanged(int page, int pageCount) {
        // Page index is 0-based, so we add 1 for display
        pageIndicator.setText(String.format("%s / %s", page + 1, totalPages));
    }


    // --- YOUR EXISTING METHODS (with one improvement) ---

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
        } else if (id == R.id.action_go_to_page) {
            showGoToPageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sharePdf() {
        if (pdfUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            if ("file".equals(pdfUri.getScheme())) {
                File file = new File(pdfUri.getPath());
                Uri shareUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
                shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
            } else {
                shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
        }
    }

    private void showGoToPageDialog() {
        // MODIFIED: Improved dialog to prevent crashes and guide the user
        if (totalPages == 0) {
            Toast.makeText(this, "Document not fully loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go to Page");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter page (1 - " + totalPages + ")"); // Give user a valid range
        builder.setView(input);

        builder.setPositiveButton("Go", (dialog, which) -> {
            String pageNumStr = input.getText().toString();
            if (!pageNumStr.isEmpty()) {
                try {
                    int pageNum = Integer.parseInt(pageNumStr);
                    if (pageNum >= 1 && pageNum <= totalPages) {
                        pdfView.jumpTo(pageNum - 1, true); // Jump to 0-indexed page
                    } else {
                        Toast.makeText(this, "Page number is out of range.", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid page number.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
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
