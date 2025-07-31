package com.pdf.toolkit;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PreviewActivity extends AppCompatActivity implements ThumbnailAdapter.OnThumbnailListener {
    
    private static final String TAG = "PreviewActivity";
    private ArrayList<Uri> pageUris;
    private int currentPageIndex = 0;
    private ImageView mainPreviewImage;
    private RecyclerView thumbnailsRecyclerView;
    private ProgressBar saveProgressBar;
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        mainPreviewImage = findViewById(R.id.main_preview_image);
        thumbnailsRecyclerView = findViewById(R.id.thumbnails_recycler_view);
        saveProgressBar = findViewById(R.id.save_progress_bar);
        doneButton = findViewById(R.id.done_button);
        ImageButton closeButton = findViewById(R.id.close_button);
        
        ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("scanned_pages");
        pageUris = new ArrayList<>();
        if (uriStrings != null) { for (String uriString : uriStrings) { pageUris.add(Uri.parse(uriString)); } }

        if (!pageUris.isEmpty()) { setupThumbnails(); displayPage(currentPageIndex); }

        doneButton.setOnClickListener(v -> saveAsPdfAndShowDialog());
        closeButton.setOnClickListener(v -> finish());
    }

    private void saveAsPdfAndShowDialog() {
        if (pageUris == null || pageUris.isEmpty()) return;
        doneButton.setEnabled(false);
        saveProgressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            Uri finalPdfUri = null;
            boolean success = false;
            try {
                PdfDocument pdfDocument = new PdfDocument();
                for (Uri uri : pageUris) {
                    Bitmap bitmap = uriToResizedBitmap(uri);
                    if (bitmap != null) {
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), pageUris.indexOf(uri) + 1).create();
                        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                        pdfDocument.finishPage(page);
                        bitmap.recycle();
                    }
                }
                ContentValues values = new ContentValues();
                String fileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Downloads/PDFToolkit"); }
                Uri pdfUri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (pdfUri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(pdfUri)) { pdfDocument.writeTo(outputStream); finalPdfUri = pdfUri; success = true; }
                }
                pdfDocument.close();
            } catch (Exception e) { Log.e(TAG, "Error saving PDF", e); success = false; }

            final boolean finalSuccess = success;
            final Uri savedUri = finalPdfUri;
            runOnUiThread(() -> {
                saveProgressBar.setVisibility(View.GONE);
                cleanupCache();
                if (finalSuccess && savedUri != null) {
                    // This is the restored, working dialog
                    showSuccessDialog(savedUri);
                } else {
                    Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
                    doneButton.setEnabled(true);
                }
            });
        }).start();
    }

    private void showSuccessDialog(Uri pdfUri) {
        new AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("PDF saved to your Downloads folder.")
            .setCancelable(false)
            .setPositiveButton("View File", (dialog, which) -> {
                Intent intent = new Intent(PreviewActivity.this, PdfViewerActivity.class);
                intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, pdfUri.toString());
                // This flag is essential for the viewer to have permission to open the file
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("New Scan", (dialog, which) -> {
                // This correctly finishes the activity, returning to the Home screen
                finish();
            })
            .show();
    }
    
    // (All helper methods are the same and correct)
    private void setupThumbnails() { thumbnailsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)); ThumbnailAdapter adapter = new ThumbnailAdapter(this, pageUris, this); thumbnailsRecyclerView.setAdapter(adapter); }
    private void displayPage(int index) { if (index >= 0 && index < pageUris.size()) { currentPageIndex = index; mainPreviewImage.setImageURI(pageUris.get(index)); } }
    @Override
    public void onThumbnailClick(int position) { displayPage(position); }
    private void cleanupCache() { File imageDir = new File(getCacheDir(), "images"); if (imageDir.exists()) { File[] files = imageDir.listFiles(); if (files != null) { for (File file : files) { file.delete(); } } } }
    private Bitmap uriToResizedBitmap(Uri uri) { try (InputStream inputStream = getContentResolver().openInputStream(uri)) { BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true; BitmapFactory.decodeStream(inputStream, null, options); options.inSampleSize = calculateInSampleSize(options, 1024, 1024); options.inJustDecodeBounds = false; try (InputStream newInputStream = getContentResolver().openInputStream(uri)) { return BitmapFactory.decodeStream(newInputStream, null, options); } } catch (Exception e) { Log.e(TAG, "Failed to load bitmap from URI", e); return null; } }
    private int calculateInSampleSize(Factory.Options options, int reqWidth, int reqHeight) { final int height = options.outHeight; final int width = options.outWidth; int inSampleSize = 1; if (height > reqHeight || width > reqWidth) { final int halfHeight = height / 2; final int halfWidth = width / 2; while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) { inSampleSize *= 2; } } return inSampleSize; }
}
