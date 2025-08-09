package com.pdf.toolkit;

// All necessary imports for the final version
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;

    private FirebaseRemoteConfig remoteConfig;
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("PDF Toolkit");
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitle_Large);
        setSupportActionBar(toolbar);

        // Initialize Remote Config and fetch AdMob App ID dynamically
        setupRemoteConfigAndInitializeAds();

        scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    GmsDocumentScanningResult scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                    if (scanningResult != null && scanningResult.getPages() != null && !scanningResult.getPages().isEmpty()) {
                        saveAsPdfAndShowDialog(scanningResult.getPages());
                    }
                }
            }
        );

        setupCardListeners();
        setupPrivacyPolicyLink();
    }

    private void setupCardListeners() {
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);

        scannerCard.setOnClickListener(v -> checkAndRequestStoragePermission());
        pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html", ""));
        
        uniToolsCard.setOnClickListener(v -> {
            String ttsUrl = remoteConfig.getString("tts_tool_url");
            launchWebViewActivity("unitools.html", ttsUrl);
        });

        allFilesCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });
    }

    private void setupRemoteConfigAndInitializeAds() {
        remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("admob_native_ad_enabled", false);
        defaultConfigMap.put("admob_native_ad_unit_id", "ca-app-pub-3940256099942544/2247696110");
        defaultConfigMap.put("privacy_policy_url", "https://your-company.com/default-privacy-policy.html");
        defaultConfigMap.put("tts_tool_url", "https://textiispeech.blogspot.com/p/unitools.html");
        // Also add default AdMob App ID here for safety fallback:
        defaultConfigMap.put("admob_app_id", "ca-app-pub-3940256099942544~3347511713");
        remoteConfig.setDefaultsAsync(defaultConfigMap);

        remoteConfig.fetchAndActivate().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                String admobAppId = remoteConfig.getString("admob_app_id");
                MobileAds.initialize(this, admobAppId);
                loadAdFromConfig();
            } else {
                // fallback to default static app ID if fetch fails
                MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
                loadAdFromConfig();
            }
        });
    }

    private void loadAdFromConfig() {
        boolean isAdEnabled = remoteConfig.getBoolean("admob_native_ad_enabled");
        if (isAdEnabled) {
            String adUnitId = remoteConfig.getString("admob_native_ad_unit_id");
            if (adUnitId.isEmpty()) return;

            AdLoader.Builder builder = new AdLoader.Builder(this, adUnitId);
            builder.forNativeAd(nativeAd -> {
                if (isDestroyed()) {
                    nativeAd.destroy();
                    return;
                }
                FrameLayout adContainer = findViewById(R.id.ad_container);
                NativeAdView adView = (NativeAdView) LayoutInflater.from(this).inflate(R.layout.native_ad_layout, null);
                populateNativeAdView(nativeAd, adView);
                adContainer.removeAllViews();
                adContainer.addView(adView);
            });

            builder.withAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    Log.e(TAG, "Ad failed to load: " + adError.getMessage());
                }
            });

            builder.build().loadAd(new AdRequest.Builder().build());
        }
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        TextView headlineView = adView.findViewById(R.id.ad_headline);
        TextView advertiserView = adView.findViewById(R.id.ad_advertiser);
        Button callToActionView = adView.findViewById(R.id.ad_call_to_action);
        ImageView iconView = adView.findViewById(R.id.ad_app_icon);

        adView.setHeadlineView(headlineView);
        adView.setCallToActionView(callToActionView);
        adView.setIconView(iconView);
        adView.setAdvertiserView(advertiserView);
        
        if (nativeAd.getMediaContent() != null) {
            adView.setMediaView(mediaView);
            mediaView.setMediaContent(nativeAd.getMediaContent());
            mediaView.setVisibility(View.VISIBLE);
        } else {
            mediaView.setVisibility(View.GONE);
        }

        headlineView.setText(nativeAd.getHeadline());
        callToActionView.setText(nativeAd.getCallToAction());

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }
        
        adView.setNativeAd(nativeAd);
    }

    private void setupPrivacyPolicyLink() {
        TextView privacyPolicyText = findViewById(R.id.privacy_policy_text);
        privacyPolicyText.setOnClickListener(v -> {
            String url = remoteConfig.getString("privacy_policy_url");
            if (url == null || url.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Privacy Policy not available.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(HomeActivity.this, "No browser found to open link.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAsPdfAndShowDialog(java.util.List<GmsDocumentScanningResult.Page> pages) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            Uri finalPdfUri = null;
            String finalFileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
            boolean success = false;
            
            Uri firstPageUri = null;
            if (!pages.isEmpty()) {
                firstPageUri = pages.get(0).getImageUri();
            }

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
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Downloads/PDFToolkit");
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
            } catch (Exception e) {
                Log.e(TAG, "Error saving PDF", e);
            }

            final boolean finalSuccess = success;
            final Uri savedUri = finalPdfUri;
            final int pageCount = pages.size();
            final Uri finalFirstPageUri = firstPageUri;

            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (finalSuccess && savedUri != null) {
                    showSuccessDialog(savedUri, finalFileName, pageCount, finalFirstPageUri);
                } else {
                    Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showSuccessDialog(@NonNull Uri pdfUri, @NonNull String fileName, int pageCount, @Nullable Uri thumbnailUri) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_success, null);

        ImageView ivThumbnail = dialogView.findViewById(R.id.dialog_thumbnail);
        TextView tvPath = dialogView.findViewById(R.id.dialog_path);
        TextView tvDetails = dialogView.findViewById(R.id.dialog_details);
        ImageButton btnClose = dialogView.findViewById(R.id.dialog_btn_close);
        ImageButton btnShare = dialogView.findViewById(R.id.dialog_btn_share);
        Button btnNewScan = dialogView.findViewById(R.id.dialog_btn_new_scan);
        Button btnViewFile = dialogView.findViewById(R.id.dialog_btn_view_file);
        ImageView doneIcon = dialogView.findViewById(R.id.dialog_done_icon); // GIF ImageView

        // Thumbnail
        if (thumbnailUri != null) {
            ivThumbnail.setImageURI(thumbnailUri);
            ivThumbnail.setVisibility(View.VISIBLE);
        } else {
            ivThumbnail.setVisibility(View.GONE);
        }

        // File size lookup (safe)
        String fileSize = "Unknown";
        try (Cursor cursor = getContentResolver().query(pdfUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    long size = cursor.getLong(sizeIndex);
                    fileSize = android.text.format.Formatter.formatShortFileSize(this, size);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not get file size.", e);
        }

        tvPath.setText("Path: Downloads/PDFToolkit");
        tvDetails.setText("Pages: " + pageCount + " | Size: " + fileSize);

        // Build dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Button listeners (same behaviour as before)
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnNewScan.setOnClickListener(v -> {
            dialog.dismiss();
            startGoogleScanner();
        });
        btnViewFile.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(HomeActivity.this, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, pdfUri.toString());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });
        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF using..."));
        });

        // Show toast
        Toast.makeText(this, "PDF saved to your Downloads folder", Toast.LENGTH_LONG).show();

        // === GIF: load from res/raw BEFORE showing dialog ===
        // Ensure you placed ic_done.gif into res/raw/ic_done.gif
        try {
            // Make visible (in case it's "gone" in XML)
            doneIcon.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .asGif()
                    .load(R.raw.ic_done) // raw resource
                    .into(doneIcon);
        } catch (Exception e) {
            // If GIF fails to load, log but still show dialog
            Log.e(TAG, "Failed to load ic_done.gif with Glide", e);
            // Optionally fallback to a static drawable:
            // doneIcon.setImageResource(R.drawable.ic_done_static);
            doneIcon.setVisibility(View.GONE);
        }

        // Show dialog
        dialog.show();

        // Auto-hide GIF after 1500ms (so it doesn't loop while dialog stays open).
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (doneIcon != null) doneIcon.setVisibility(View.GONE);
        }, 1500);
    }

    private void checkAndRequestStoragePermission() {
        if (hasStoragePermission()) {
            startGoogleScanner();
        } else {
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGoogleScanner();
        } else {
            Toast.makeText(this, "Storage permission is required to save scanned files.", Toast.LENGTH_LONG).show();
        }
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
            .addOnSuccessListener(intentSender ->
                scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
            .addOnFailureListener(e ->
                Toast.makeText(this, "Scanner not available.", Toast.LENGTH_SHORT).show());
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
            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    
    private void launchWebViewActivity(String fileName, String ttsUrl) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName);
        intent.putExtra("tts_url_extra", ttsUrl);
        startActivity(intent);
    }
}
