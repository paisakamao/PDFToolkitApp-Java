package com.pdf.toolkit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.barteksc.pdfviewer.PDFView;
import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_NAME = "com.pdf.toolkit.FILE_NAME"; // This is now a PATH

    @Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pdf_viewer);

    // ✅ Hide the default white ActionBar
    if (getSupportActionBar() != null) {
        getSupportActionBar().hide();
    }

    // Set filename in black TextView title
    TextView titleText = findViewById(R.id.textViewPdfTitle);

    Intent intent = getIntent();
    String filePath = intent.getStringExtra(EXTRA_FILE_NAME);

    if (filePath != null && !filePath.isEmpty()) {
        File file = new File(filePath);
        if (file.exists()) {
            titleText.setText(file.getName());

            PDFView pdfView = findViewById(R.id.pdfView);
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

    // This handles the back arrow in the ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
