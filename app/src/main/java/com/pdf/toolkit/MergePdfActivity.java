package com.pdf.toolkit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MergePdfActivity extends AppCompatActivity {

    private AdView adView;
    private List<Uri> selectedPdfUris = new ArrayList<>();
    private Button selectBtn, mergeBtn;

    private final ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            selectedPdfUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        selectedPdfUris.add(result.getData().getData());
                    }
                    Toast.makeText(this, selectedPdfUris.size() + " files selected", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdf);

        // AdMob
        MobileAds.initialize(this, initializationStatus -> {});
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        selectBtn = findViewById(R.id.btn_select_pdfs);
        mergeBtn = findViewById(R.id.btn_merge_pdfs);

        selectBtn.setOnClickListener(v -> selectPdfFiles());
        mergeBtn.setOnClickListener(v -> {
            if (selectedPdfUris.isEmpty()) {
                Toast.makeText(this, "Please select PDFs to merge", Toast.LENGTH_SHORT).show();
            } else {
                mergePdfs();
            }
        });
mergePdfButton.setOnClickListener(v -> {
    if (selectedPdfUris.size() < 2) {
        Toast.makeText(this, "Select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show();
        return;
    }

    try {
        mergePdfs(selectedPdfUris);
    } catch (IOException e) {
        Toast.makeText(this, "Error merging PDFs: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    }
});

    }

    private void selectPdfFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(Intent.createChooser(intent, "Select PDFs"));
    }
    private void mergePdfs(List<Uri> pdfUris) throws IOException {
    PDFMergerUtility merger = new PDFMergerUtility();

    for (Uri uri : pdfUris) {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream != null) {
            merger.addSource(inputStream);
        }
    }

    File mergedFile = new File(getExternalFilesDir(null), "merged_output.pdf");
    merger.setDestinationFileName(mergedFile.getAbsolutePath());
    merger.mergeDocuments(null);

    Toast.makeText(this, "Merged PDF saved: " + mergedFile.getName(), Toast.LENGTH_LONG).show();

    // Optional: Open or share the merged PDF
    Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", mergedFile);

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(pdfUri, "application/pdf");
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(intent);

    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    startActivity(intent);
}

    private void mergePdfs() {
        try {
            File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "merged_output.pdf");
            FileOutputStream fos = new FileOutputStream(outputFile);
            Document document = new Document();
            PdfCopy copy = new PdfCopy(document, fos);
            document.open();

            for (Uri uri : selectedPdfUris) {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                PdfReader reader = new PdfReader(inputStream);
                int n = reader.getNumberOfPages();
                for (int page = 1; page <= n; page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }
                reader.close();
            }

            document.close();
            fos.close();

            Toast.makeText(this, "Merged PDF saved to: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to merge PDFs: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
