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
import androidx.core.content.FileProvider;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MergePdfActivity extends AppCompatActivity {

    private AdView adView;
    private List<Uri> selectedPdfUris = new ArrayList<>();
    private Button selectBtn, mergeBtn;

    private final ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedPdfUris.clear();
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            selectedPdfUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        selectedPdfUris.add(result.getData().getData());
                    }
                    Toast.makeText(this, selectedPdfUris.size() + " PDF(s) selected", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdf);

        MobileAds.initialize(this, initializationStatus -> {});
        adView = findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());

        selectBtn = findViewById(R.id.btn_select_pdfs);
        mergeBtn = findViewById(R.id.btn_merge_pdfs);

        selectBtn.setOnClickListener(v -> selectPdfFiles());

        mergeBtn.setOnClickListener(v -> {
            if (selectedPdfUris.size() < 2) {
                Toast.makeText(this, "Select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show();
            } else {
                mergeSelectedPdfs();
            }
        });
    }

    private void selectPdfFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(Intent.createChooser(intent, "Select PDF files"));
    }

    private void mergeSelectedPdfs() {
        Document document = null;
        PdfCopy copy = null;
        FileOutputStream fos = null;

        try {
            // Output file with timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "merged_" + timeStamp + ".pdf");
            fos = new FileOutputStream(outputFile);

            document = new Document();
            copy = new PdfCopy(document, fos);
            document.open();

            for (Uri uri : selectedPdfUris) {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    PdfReader reader = new PdfReader(inputStream);
                    int pages = reader.getNumberOfPages();
                    for (int i = 1; i <= pages; i++) {
                        copy.addPage(copy.getImportedPage(reader, i));
                    }
                    reader.close();
                    inputStream.close();
                }
            }

            Toast.makeText(this, "PDFs merged successfully!", Toast.LENGTH_LONG).show();

            Uri mergedUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", outputFile);
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(mergedUri, "application/pdf");
            viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(viewIntent, "Open merged PDF"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Merge failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (document != null) document.close();
                if (fos != null) fos.close();
            } catch (Exception ignored) {}
        }
    }
}
