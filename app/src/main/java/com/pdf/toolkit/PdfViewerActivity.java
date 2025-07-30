package com.pdf.toolkit;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.barteksc.pdfviewer.PDFView;

public class PdfViewerActivity extends AppCompatActivity {

    // IMPORTANT: The key is now EXTRA_FILE_URI, not EXTRA_FILE_NAME
    public static final String EXTRA_FILE_URI = "com.pdf.toolkit.FILE_URI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        TextView titleText = findViewById(R.id.textViewPdfTitle);
        PDFView pdfView = findViewById(R.id.pdfView);

        Intent intent = getIntent();
        // We receive the URI as a String and parse it back into a Uri object
        String uriString = intent.getStringExtra(EXTRA_FILE_URI);

        if (uriString != null && !uriString.isEmpty()) {
            Uri fileUri = Uri.parse(uriString);
            
            // Set the title using a helper method that works with URIs
            titleText.setText(getFileNameFromUri(fileUri));

            // Load the PDF from the URI instead of a File object
            pdfView.fromUri(fileUri)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .load();
        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // This is a new helper method to get the file's display name from its content URI
    private String getFileNameFromUri(Uri uri) {
        String fileName = "Document";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            // If it fails for any reason, we just use a generic name
        }
        return fileName;
    }

    // This method is no longer needed with our current setup, but it's fine to keep.
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}