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
                    if (scanningResult != null && scanningResult.getPages() != null) {
                        
                        ArrayList<String> pageUris = new ArrayList<>();
                        for (GmsDocumentScanningResult.Page page : scanningResult.getPages()) {
                            pageUris.add(page.getImageUri().toString());
                        }
                        
                        if (!pageUris.isEmpty()) {
                            Intent intent = new Intent(HomeActivity.this, PreviewActivity.class);
                            intent.putStringArrayListExtra("scanned_pages", pageUris);
                            
                            // --- THIS IS THE CRITICAL FIX ---
                            // This flag grants the PreviewActivity permission to read the URIs.
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            
                            startActivity(intent);
                        }
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
            .addOnSuccessListener(intentSender -> {
                scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Scanner not available.", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void setupOtherCards() {
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView fileManagerCard = findViewById(R.id.card_file_manager);
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);
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
    }

    private void launchWebViewActivity(String fileName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName);
        startActivity(intent);
    }
}