package com.pdf.toolkit;

import android.app.ProgressDialog;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        mainPreviewImage = findViewById(R.id.main_preview_image);
        thumbnailsRecyclerView = findViewById(R.id.thumbnails_recycler_view);
        doneButton = findViewById(R.id.done_button);
        ImageButton closeButton = findViewById(R.id.close_button);
        Button cropButton = findViewById(R.id.crop_button);
        Button retakeButton = findViewById(R.id.retake_button);
        Button deleteButton = findViewById(R.id.delete_button);

        ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("scanned_pages");
        pageUris = new ArrayList<>();
        if (uriStrings != null) {
            for (String uriString : uriStrings) {
                pageUris.add(Uri.parse(uriString));
            }
        }

        if (!pageUris.isEmpty()) {
            setupThumbnails();
            displayPage(currentPageIndex);
        }

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
    
    // --- THIS IS THE FULLY REWRITTEN AND CORRECTED SAVE METHOD ---
    private void saveAsPdfAndFinish() {
        if (pageUris == null || pageUris.isEmpty()) {
            Toast.makeText(this, "No pages to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                PdfDocument pdfDocument = new PdfDocument();
                for (Uri uri : pageUris) {
                    // This method now includes resizing to prevent memory errors
                    Bitmap bitmap = uriToResizedBitmap(uri);
                    
                    // --- CRASH FIX #1: SAFETY CHECK FOR NULL BITMAP ---
                    if (bitmap != null) {
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), pageUris.indexOf(uri) + 1).create();
                        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                        pdfDocument.finishPage(page);
                        bitmap.recycle();
                    } else {
                        Log.e(TAG, "Could not decode bitmap for URI: " + uri);
                    }
                }

                // --- CRASH FIX #2: USE MODERN MEDIATOR API TO SAVE THE FILE ---
                ContentValues values = new ContentValues();
                String fileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                // The "Downloads" folder is a more common and accessible place for saved documents
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Downloads/PDFToolkit");
                }

                Uri pdfUri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

                if (pdfUri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(pdfUri)) {
                        pdfDocument.writeTo(outputStream);
                    }
                } else {
                    throw new IOException("Failed to create new MediaStore entry.");
                }
                
                pdfDocument.close();

                // UI updates must run on the main thread
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(PreviewActivity.this, AllFilesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error saving PDF", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // This is the robust helper method to get a resized bitmap from a URI
    private Bitmap uriToResizedBitmap(Uri uri) {
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
        } catch (Exception e) {
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