package com.pdf.toolkit;

import android.app.Activity;
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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    GmsDocumentScanningResult scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                    if (scanningResult != null && scanningResult.getPages() != null && !scanningResult.getPages().isEmpty()) {
                        // Immediately save the results as a PDF
                        saveAsPdfAndShowDialog(scanningResult.getPages());
                    }
                }
            }
        );

        CardView scannerCard = findViewById(R.id.card_scanner);
        scannerCard.setOnClickListener(v -> startGoogleScanner());
        setupOtherCards();
    }

    private void startGoogleScanner() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(false)
            .setPageLimit(20)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build();
        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);
        scanner.getStartScanIntent(this)
            .addOnSuccessListener(intentSender -> scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
            .addOnFailureListener(e -> Toast.makeText(this, "Scanner not available.", Toast.LENGTH_SHORT).show());
    }

    private void saveAsPdfAndShowDialog(java.util.List<GmsDocumentScanningResult.Page> pages) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            Uri finalPdfUri = null;
            boolean success = false;
            try {
                PdfDocument pdfDocument = new PdfDocument();
                for (GmsDocumentScanningResult.Page page : pages) {
                    Bitmap bitmap = uriToResizedBitmap(page.getImageUri());
                    if (bitmap != null) {
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), pages.indexOf(page) + 1).create();
                        PdfDocument.Page pdfPage = pdfDocument.startPage(pageInfo);
                        pdfPage.getCanvas().drawBitmap(bitmap, 0, 0, null);
                        pdfDocument.finishPage(pdfPage);
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
                    try (OutputStream outputStream = getContentResolver().openOutputStream(pdfUri)) {
                        pdfDocument.writeTo(outputStream);
                        finalPdfUri = pdfUri;
                        success = true;
                    }
                }
                pdfDocument.close();
            } catch (Exception e) { Log.e(TAG, "Error saving PDF", e); }

            final boolean finalSuccess = success;
            final Uri savedUri = finalPdfUri;
            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (finalSuccess && savedUri != null) {
                    showSuccessDialog(savedUri);
                } else {
                    Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
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
                Intent intent = new Intent(HomeActivity.this, PdfViewerActivity.class);
                intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, pdfUri.toString());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            })
            .setNegativeButton("New Scan", (dialog, which) -> dialog.dismiss())
            .show();
    }
    
    private Bitmap uriToResizedBitmap(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            options.inJustDecodeBounds = false;
            try (InputStream newInputStream = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(newInputStream, null, options);
            }
        } catch (Exception e) { Log.e(TAG, "Failed to load bitmap from URI", e); return null; }
    }
    
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) { final int height = options.outHeight; final int width = options.outWidth; int inSampleSize = 1; if (height > reqHeight || width > reqWidth) { final int halfHeight = height / 2; final int halfWidth = width / 2; while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) { inSampleSize *= 2; } } return inSampleSize; }
    
    // (Your other card setup methods)
    private void setupOtherCards() { CardView pdfToolCard = findViewById(R.id.card_pdf_tool); CardView allFilesCard = findViewById(R.id.card_all_files); CardView fileManagerCard = findViewById(R.id.card_file_manager); CardView uniToolsCard = findViewById(R.id.card_uni_tools); pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html")); uniToolsCard.setOnClickListener(v -> launchWebViewActivity("unitools.html")); allFilesCard.setOnClickListener(v -> { Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class); startActivity(intent); }); fileManagerCard.setOnClickListener(v -> { Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); if (intent.resolveActivity(getPackageManager()) != null) { startActivity(intent); } }); }
    private void launchWebViewActivity(String fileName) { Intent intent = new Intent(HomeActivity.this, MainActivity.class); intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName); startActivity(intent); }
}
