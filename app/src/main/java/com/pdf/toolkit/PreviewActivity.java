package com.pdf.toolkit;

import android.app.ProgressDialog; // Import for the loading dialog
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
        if (uriStrings != null) {
            for (String uriString : uriStrings) {
                pageUris.add(Uri.parse(uriString));
            }
        }

        // Setup the views if we have pages
        if (!pageUris.isEmpty()) {
            setupThumbnails();
            displayPage(currentPageIndex);
        }

        // Set button listeners
        doneButton.setOnClickListener(v -> saveAsPdfAndFinish());
        closeButton.setOnClickListener(v -> finish());
        
        cropButton.setOnClickListener(v -> Toast.makeText(this, "Crop & Rotate coming soon!", Toast.LENGTH_SHORT).show());
        retakeButton.setOnClickListener(v -> Toast.makeText(this, "Retake coming soon!", Toast.LENGTH_SHORT).show());
        deleteButton.setOnClickListener(v -> Toast.makeText(this, "Delete coming soon!", Toast.LENGTH_SHORT).show());
    }

    private void setupThumbnails() {
        thumbnailsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ThumbnailAdapter adapter = new ThumbnailAdapter(this, pageUris, this);
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
    
    // --- THIS IS THE CORRECTED, CRASH-PROOF METHOD ---
    private void saveAsPdfAndFinish() {
        if (pageUris == null || pageUris.isEmpty()) {
            Toast.makeText(this, "No pages to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a loading dialog so the user knows something is happening
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Run all file operations on a background thread to prevent crashing
        new Thread(() -> {
            try {
                // Define where to save the file
                File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFToolkit");
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs();
                }
                String pdfFileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
                File pdfFile = new File(pdfDir, pdfFileName);

                // Create the PDF document
                PdfDocument pdfDocument = new PdfDocument();
                
                // This loop now runs entirely on the background thread.
                for (Uri uri : pageUris) {
                    Bitmap bitmap = uriToBitmap(uri); // This file operation is now safe
                    if (bitmap != null) {
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), pageUris.indexOf(uri) + 1).create();
                        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                        pdfDocument.finishPage(page);
                        bitmap.recycle();
                    }
                }
                
                // This file operation is also safe now
                pdfDocument.writeTo(new FileOutputStream(pdfFile));
                pdfDocument.close();

                // Switch back to the main thread to update UI (dismiss dialog, show toast, navigate)
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to the All Files screen
                    Intent intent = new Intent(PreviewActivity.this, AllFilesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error writing PDF file", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error saving PDF.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private Bitmap uriToBitmap(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(inputStream);
            // We can simplify this and remove the complex resizing logic for now,
            // as the Google Scanner already provides reasonably sized images.
        } catch (IOException e) {
            Log.e(TAG, "Failed to load bitmap from URI", e);
            return null;
        }
    }
}