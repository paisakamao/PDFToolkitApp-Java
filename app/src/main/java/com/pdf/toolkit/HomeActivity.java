package com.pdf.toolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    // NEW: A launcher to start the Google scanner and handle its result.
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // --- NEW: This block registers a callback for the scanner result ---
        // It runs AFTER the Google Scanner finishes.
        scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // The result is an object containing the scanned pages.
                    GmsDocumentScanningResult scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                    if (scanningResult != null && scanningResult.getPages() != null) {
                        
                        // We extract the file locations (URIs) of the scanned pages.
                        ArrayList<String> pageUris = new ArrayList<>();
                        for (GmsDocumentScanningResult.Page page : scanningResult.getPages()) {
                            pageUris.add(page.getImageUri().toString());
                        }
                        
                        // Now we launch our NEW PreviewActivity to show the results.
                        Intent intent = new Intent(HomeActivity.this, PreviewActivity.class);
                        intent.putStringArrayListExtra("scanned_pages", pageUris);
                        startActivity(intent);
                    }
                }
            }
        );

        // --- Find all your cards as before ---
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView fileManagerCard = findViewById(R.id.card_file_manager);
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);

        // --- UNCHANGED: These listeners are exactly as you provided ---
        pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html"));
        uniToolsCard.setOnClickListener(v -> launchWebViewActivity("unitools.html"));
        
        allFilesCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });
        
        fileManagerCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        // --- THIS IS THE ONLY LISTENER THAT HAS CHANGED ---
        // Instead of starting your old ScannerActivity, it calls our new method.
        scannerCard.setOnClickListener(v -> startGoogleScanner());
    }

    // --- NEW: This method configures and launches the Google Document Scanner ---
    private void startGoogleScanner() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL) // This enables both Auto and Manual modes from your video.
            .setGalleryImportAllowed(false) // User cannot import from gallery.
            .setPageLimit(20) // Let's allow up to 20 pages.
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG) // We want image files.
            .build();

        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);

        scanner.getStartScanIntent(this)
            .addOnSuccessListener(intentSender -> {
                // The scanner is ready to be launched.
                scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
            })
            .addOnFailureListener(e -> {
                // This happens if the scanner isn't available (e.g., Google Play Services issue).
                Toast.makeText(this, "Scanner not available.", Toast.LENGTH_SHORT).show();
            });
    }

    // --- UNCHANGED: This helper method is exactly as you provided ---
    private void launchWebViewActivity(String fileName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName);
        startActivity(intent);
    }
}
