package com.pdfscanner.toolkit;

// (This is the stable version using file paths)
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PreviewActivity extends AppCompatActivity implements ThumbnailAdapter.OnThumbnailListener {
    private static final String TAG = "PreviewActivity";
    private ArrayList<String> pagePaths;
    private int currentPageIndex = 0;
    private ImageView mainPreviewImage;
    private ProgressBar saveProgressBar;
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        mainPreviewImage = findViewById(R.id.main_preview_image);
        RecyclerView thumbnailsRecyclerView = findViewById(R.id.thumbnails_recycler_view);
        saveProgressBar = findViewById(R.id.save_progress_bar);
        doneButton = findViewById(R.id.done_button);
        ImageButton closeButton = findViewById(R.id.close_button);
        pagePaths = getIntent().getStringArrayListExtra("scanned_pages_paths");
        if (pagePaths != null && !pagePaths.isEmpty()) {
            thumbnailsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            ThumbnailAdapter adapter = new ThumbnailAdapter(pagePaths, this);
            thumbnailsRecyclerView.setAdapter(adapter);
            displayPage(currentPageIndex);
        }
        doneButton.setOnClickListener(v -> saveAsPdfAndShowDialog());
        closeButton.setOnClickListener(v -> finish());
    }

    private void displayPage(int index) {
        if (index >= 0 && index < pagePaths.size()) {
            currentPageIndex = index;
            File imgFile = new File(pagePaths.get(index));
            if(imgFile.exists()){
                mainPreviewImage.setImageURI(Uri.fromFile(imgFile));
            }
        }
    }

    @Override
    public void onThumbnailClick(int position) {
        displayPage(position);
    }

    private void saveAsPdfAndShowDialog() {
        if (pagePaths == null || pagePaths.isEmpty()) return;
        doneButton.setEnabled(false);
        saveProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            Uri finalPdfUri = null;
            boolean success = false;
            try {
                PdfDocument pdfDocument = new PdfDocument();
                for (String path : pagePaths) {
                    Bitmap bitmap = pathToResizedBitmap(path);
                    if (bitmap != null) {
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), pagePaths.indexOf(path) + 1).create();
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Downloads/PDF Kit Pro");
                }
                Uri pdfUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (pdfUri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(pdfUri)) {
                        pdfDocument.writeTo(outputStream);
                        finalPdfUri = pdfUri;
                        success = true;
                    }
                }
                pdfDocument.close();
                cleanupCache();
            } catch (Exception e) {
                Log.e(TAG, "Error saving PDF", e);
            }
            final boolean finalSuccess = success;
            final Uri savedUri = finalPdfUri;
            runOnUiThread(() -> {
                saveProgressBar.setVisibility(View.GONE);
                if (finalSuccess && savedUri != null) {
                    // --- AD INTEGRATION START ---
                    // Instead of showing the dialog directly, we first show an interstitial ad.
                    // The success dialog will be shown in the callback, after the user closes the ad.
                    AdManager.getInstance().showInterstitial(PreviewActivity.this, () -> {
                        showSuccessDialog(savedUri);
                    });
                    // --- AD INTEGRATION END ---
                } else {
                    Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
                    doneButton.setEnabled(true);
                }
            });
        }).start();
    }

    private void showSuccessDialog(@NonNull Uri pdfUri) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("PDF saved to your Downloads folder.")
                .setCancelable(false)
                .setPositiveButton("View File", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(PreviewActivity.this, PdfViewerActivity.class);
                    intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, pdfUri.toString());
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("New Scan", (dialog, which) -> {
                    dialog.dismiss();
                    finish(); // This will take the user back to the previous screen to start a new scan.
                })
                .show();
    }

    private void cleanupCache() {
        File imageDir = new File(getCacheDir(), "images");
        if (imageDir.exists()) {
            File[] files = image.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    private Bitmap pathToResizedBitmap(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap from path", e);
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
