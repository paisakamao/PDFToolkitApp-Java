package com.pdf.toolkit;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    CardView mergeCard, splitCard, compressCard, pdfToImageCard, imageToPdfCard,
            lockCard, unlockCard, watermarkCard, rotateCard, extractTextCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Initialize Mobile Ads
        MobileAds.initialize(this, initializationStatus -> {});

        // ✅ Load Banner Ad
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Cards
        mergeCard = findViewById(R.id.card_merge_pdf);
        splitCard = findViewById(R.id.card_split_pdf);
        compressCard = findViewById(R.id.card_compress_pdf);
        pdfToImageCard = findViewById(R.id.card_pdf_to_image);
        imageToPdfCard = findViewById(R.id.card_image_to_pdf);
        lockCard = findViewById(R.id.card_lock_pdf);
        unlockCard = findViewById(R.id.card_unlock_pdf);
        watermarkCard = findViewById(R.id.card_watermark_pdf);
        rotateCard = findViewById(R.id.card_rotate_pdf);
        extractTextCard = findViewById(R.id.card_extract_text);

        // Click Listeners
        mergeCard.setOnClickListener(v -> startActivity(new Intent(this, MergePdfActivity.class)));
        splitCard.setOnClickListener(v -> startActivity(new Intent(this, SplitPdfActivity.class)));
        compressCard.setOnClickListener(v -> startActivity(new Intent(this, CompressPdfActivity.class)));
        pdfToImageCard.setOnClickListener(v -> startActivity(new Intent(this, PdfToImageActivity.class)));
        ImageToPdfCard.setOnClickListener(v -> startActivity(new Intent(this, ImageToPdfActivity.class)));
        lockCard.setOnClickListener(v -> startActivity(new Intent(this, LockPdfActivity.class)));
        unlockCard.setOnClickListener(v -> startActivity(new Intent(this, UnlockPdfActivity.class)));
        watermarkCard.setOnClickListener(v -> startActivity(new Intent(this, WatermarkPdfActivity.class)));
        rotateCard.setOnClickListener(v -> startActivity(new Intent(this, RotatePdfActivity.class)));
        extractTextCard.setOnClickListener(v -> startActivity(new Intent(this, ExtractTextActivity.class)));
    }
}