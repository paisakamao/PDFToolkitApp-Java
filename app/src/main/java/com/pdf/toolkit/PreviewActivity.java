package com.pdf.toolkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PreviewActivity extends AppCompatActivity implements ThumbnailAdapter.OnThumbnailListener {

    private static final String TAG = "PreviewActivity";
    private ArrayList<Uri> pageUris;
    private int currentPageIndex = 0;

    // UI Elements
    private ImageView mainPreviewImage;
    private RecyclerView thumbnailsRecyclerView;
    private Button doneButton;
    private ImageButton closeButton;
    private Button cropButton, retakeButton, deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Find all UI elements
        mainPreviewImage = findViewById(R.id.main_preview_image);
        thumbnailsRecyclerView = findViewById(R.id.thumbnails_recycler_view);
        doneButton = findViewById(R.id.done_button);
        closeButton = findViewById(R.id.close_button);
        cropButton = findViewById(R.id.crop_button);
        retakeButton = findViewById(R.id.retake_button);
        deleteButton = findViewById(R.id.delete_button);

        // Get the list of scanned page URIs from HomeActivity
        ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("scanned_pages");
        pageUris = new ArrayList<>();
        for (String uriString : uriStrings) {
            pageUris.add(Uri.parse(uriString));
        }

        // Setup the views
        setupThumbnails();
        displayPage(currentPageIndex);

        // Set button listeners
        doneButton.setOnClickListener(v -> saveAsPdfAndFinish());
        closeButton.setOnClickListener(v -> finish());
        
        // Placeholder listeners for other functions
        cropButton.setOnClickListener(v -> Toast.makeText(this, "Crop & Rotate coming soon!", Toast.LENGTH_SHORT).show());
        retakeButton.setOnClickListener(v -> Toast.makeText(this, "Retake coming soon!", Toast.LENGTH_SHORT).show());
        deleteButton.setOnClickListener(v -> Toast.makeText(this, "Delete coming soon!", Toast.LENGTH_SHORT).show());
    }

    private void setupThumbnails() {
        ThumbnailAdapter adapter = new ThumbnailAdapter(this, pageUris, this);
        thumbnailsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thumbnailsRecyclerView.setAdapter(adapter);
    }

    private void displayPage(int index) {
        if (index >= 0 && index < pageUris.size()) {
            currentPageIndex = index;
            mainPreviewImage.setImageURI(pageUris.get(index));
        }
    }

    @Override
    public void onThumbnailClick(int position) {
        displayPage(position);
    }
    
    private void saveAsPdfAndFinish() {
        if (pageUris.isEmpty()) {
            Toast.makeText(this, "No pages to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Define where to save the file
        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFToolkit");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }
        String pdfFileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
        File pdfFile = new File(pdfDir, pdfFileName);

        // Create the PDF document
        new Thread(() -> {
            try {
                PdfDocument pdfDocument = new PdfDocument();
                for (Uri uri : pageUris) {
                    Bitmap bitmap = uriToBitmap(uri);
                    if (bitmap != null) {
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), pageUris.indexOf(uri) + 1).create();
                        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                        pdfDocument.finishPage(page);
                        bitmap.recycle();
                    }
                }
                pdfDocument.writeTo(new FileOutputStream(pdfFile));
                pdfDocument.close();

                // Switch back to the main thread to show Toast and navigate
                runOnUiThread(() -> {
                    Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate to the recent files screen
                    Intent intent = new Intent(PreviewActivity.this, AllFilesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // Close this preview activity
                });

            } catch (IOException e) {
                Log.e(TAG, "Error writing PDF file", e);
                runOnUiThread(() -> Toast.makeText(this, "Error saving PDF.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Helper method to convert a URI to a resized bitmap
    private Bitmap uriToBitmap(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            // First, decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            try (InputStream newInputStream = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(newInputStream, null, options);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load bitmap from URI", e);
            return null;
        }
    }
    
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}