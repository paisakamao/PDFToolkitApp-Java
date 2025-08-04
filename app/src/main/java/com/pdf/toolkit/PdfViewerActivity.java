package com.pdf.toolkit;

import android.content.DialogInterface;
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
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import java.io.File;
import java.util.Locale; // --- THIS IS THE MISSING IMPORT THAT CAUSED THE CRASH ---

public class PdfViewerActivity extends AppCompatActivity implements OnPageChangeListener {

    public static final String EXTRA_FILE_URI = "com.pdf.toolkit.FILE_URI";

    private PDFView pdfView;
    private Uri pdfUri;
    private TextView pageCountText;

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
        pageCountText = findViewById(R.id.page_count_text);

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
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .onPageChange(this)
                .scrollHandle(new DefaultScrollHandle(this))
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
            if (pdfUri.getScheme().equals("file")) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go to Page");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter page number");
        builder.setView(input);

        builder.setPositiveButton("Go", (dialog, which) -> {
            String pageNumStr = input.getText().toString();
            if (!pageNumStr.isEmpty()) {
                try {
                    int pageNum = Integer.parseInt(pageNumStr);
                    pdfView.jumpTo(pageNum - 1, true);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    @Override
    public void onPageChanged(int page, int pageCount) {
        // This line will now work because 'Locale' is imported
        pageCountText.setText(String.format(Locale.getDefault(), "%d / %d", page + 1, pageCount));
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
