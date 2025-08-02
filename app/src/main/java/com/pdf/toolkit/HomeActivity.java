package com.pdf.toolkit;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private FirebaseRemoteConfig remoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("PDF Toolkit");
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitle_Large);
        setSupportActionBar(toolbar);

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});

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
        setupRemoteConfigAndLoadAd();
    }
    
    // This method is now correct for the 4 functional cards
    private void setupCardListeners() {
        CardView scannerCard = findViewById(R.id.card_scanner);
        CardView pdfToolCard = findViewById(R.id.card_pdf_tool);
        CardView allFilesCard = findViewById(R.id.card_all_files);
        CardView uniToolsCard = findViewById(R.id.card_uni_tools);
        
        scannerCard.setOnClickListener(v -> checkAndRequestStoragePermission());
        pdfToolCard.setOnClickListener(v -> launchWebViewActivity("index.html"));
        uniToolsCard.setOnClickListener(v -> launchWebViewActivity("unitools.html"));
        allFilesCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AllFilesActivity.class);
            startActivity(intent);
        });
    }

    private void setupRemoteConfigAndLoadAd() {
        remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("admob_native_ad_enabled", true);
        defaultConfigMap.put("admob_native_ad_unit_id", "ca-app-pub-3940256099942544/2247696110");
        remoteConfig.setDefaultsAsync(defaultConfigMap);

        remoteConfig.fetchAndActivate().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                loadAdFromConfig();
            }
        });
    }

    private void loadAdFromConfig() {
        boolean isAdEnabled = remoteConfig.getBoolean("admob_native_ad_enabled");
        if (isAdEnabled) {
            String adUnitId = remoteConfig.getString("admob_native_ad_unit_id");
            if (adUnitId.isEmpty()) { return; }

            AdLoader.Builder builder = new AdLoader.Builder(this, adUnitId);
            builder.forNativeAd(nativeAd -> {
                if (isDestroyed()) {
                    nativeAd.destroy();
                    return;
                }
                FrameLayout adContainer = findViewById(R.id.ad_container);
                NativeAdView adView = (NativeAdView) getLayoutInflater().inflate(R.layout.native_ad_layout, null);
                populateNativeAdView(nativeAd, adView);
                adContainer.removeAllViews();
                adContainer.addView(adView);
            });
            AdLoader adLoader = builder.build();
            adLoader.loadAd(new AdRequest.Builder().build());
        }
    }
    
    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        if (nativeAd.getHeadline() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }
        if (nativeAd.getCallToAction() != null) {
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }
        adView.setNativeAd(nativeAd);
    }
    
    // --- All other methods are the final, stable versions from your working file ---
    private void checkAndRequestStoragePermission() { if (hasStoragePermission()) { startGoogleScanner(); } else { requestStoragePermission(); } }
    private boolean hasStoragePermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { return Environment.isExternalStorageManager(); } else { return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED; } }
    private void requestStoragePermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { try { Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION); intent.addCategory("android.intent.category.DEFAULT"); intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName()))); startActivity(intent); } catch (Exception e) { Intent intent = new Intent(); intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION); startActivity(intent); } } else { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE); } }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) { if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { startGoogleScanner(); } else { Toast.makeText(this, "Storage permission is required to save scanned files.", Toast.LENGTH_LONG).show(); } } }
    private void startGoogleScanner() { GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder().setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).setGalleryImportAllowed(false).setPageLimit(20).setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG).build(); GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options); scanner.getStartScanIntent(this).addOnSuccessListener(intentSender -> scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build())).addOnFailureListener(e -> Toast.makeText(this, "Scanner not available.", Toast.LENGTH_SHORT).show()); }
    private void saveAsPdfAndShowDialog(java.util.List<GmsDocumentScanningResult.Page> pages) { ProgressDialog progressDialog = new ProgressDialog(this); progressDialog.setMessage("Creating PDF..."); progressDialog.setCancelable(false); progressDialog.show(); new Thread(() -> { Uri finalPdfUri = null; boolean success = false; try { PdfDocument pdfDocument = new PdfDocument(); for (GmsDocumentScanningResult.Page page : pages) { Bitmap bitmap = uriToResizedBitmap(page.getImageUri()); if (bitmap != null) { PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), pages.indexOf(page) + 1).create(); PdfDocument.Page pdfPage = pdfDocument.startPage(pageInfo); pdfPage.getCanvas().drawBitmap(bitmap, 0, 0, null); pdfDocument.finishPage(pdfPage); bitmap.recycle(); } } ContentValues values = new ContentValues(); String fileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf"; values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf"); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Downloads/PDFToolkit"); } Uri pdfUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values); if (pdfUri != null) { try (OutputStream outputStream = getContentResolver().openOutputStream(pdfUri)) { pdfDocument.writeTo(outputStream); finalPdfUri = pdfUri; success = true; } } pdfDocument.close(); } catch (Exception e) { Log.e(TAG, "Error saving PDF", e); } final boolean finalSuccess = success; final Uri savedUri = finalPdfUri; runOnUiThread(() -> { progressDialog.dismiss(); if (finalSuccess && savedUri != null) { showSuccessDialog(savedUri); } else { Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show(); } }); }).start(); }
    private void showSuccessDialog(@NonNull Uri pdfUri) { new AlertDialog.Builder(this).setTitle("Success").setMessage("PDF saved to your Downloads folder.").setCancelable(false).setPositiveButton("View File", (dialog, which) -> { dialog.dismiss(); Intent intent = new Intent(HomeActivity.this, PdfViewerActivity.class); intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, pdfUri.toString()); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(intent); }).setNegativeButton("New Scan", (dialog, which) -> { dialog.dismiss(); startGoogleScanner(); }).show(); }
    private Bitmap uriToResizedBitmap(Uri uri) { try (InputStream inputStream = getContentResolver().openInputStream(uri)) { BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true; BitmapFactory.decodeStream(inputStream, null, options); options.inSampleSize = calculateInSampleSize(options, 1024, 1024); options.inJustDecodeBounds = false; try (InputStream newInputStream = getContentResolver().openInputStream(uri)) { return BitmapFactory.decodeStream(newInputStream, null, options); } } catch (Exception e) { Log.e(TAG, "Failed to load bitmap from URI", e); return null; } }
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) { final int height = options.outHeight; final int width = options.outHeight; int inSampleSize = 1; if (height > reqHeight || width > reqWidth) { final int halfHeight = height / 2; final int halfWidth = width / 2; while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) { inSampleSize *= 2; } } return inSampleSize; }
    private void launchWebViewActivity(String fileName) { Intent intent = new Intent(HomeActivity.this, MainActivity.class); intent.putExtra(MainActivity.EXTRA_HTML_FILE, fileName); startActivity(intent); }
}
