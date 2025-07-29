// Paste this complete, updated code into your ScannerActivity.java file

package com.pdf.toolkit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// All other imports remain the same
import android.annotation.SuppressLint;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;
import com.pdf.toolkit.R;

public class ScannerActivity extends AppCompatActivity {

    private WebView webView;

    // This is the new launcher for the permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue and load the WebView.
                    loadWebView();
                } else {
                    // Permission was denied. Show a message and close the activity.
                    Toast.makeText(this, "Camera permission is required to use the scanner.", Toast.LENGTH_LONG).show();
                    finish(); // Close ScannerActivity
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException e) {
            // No action needed
        }

        webView = findViewById(R.id.webView);
        
        // CHECK FOR PERMISSION BEFORE LOADING THE SCANNER
        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // You have the permission, so load the scanner
            loadWebView();
        } else {
            // You don't have the permission, so request it
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void loadWebView() {
        setupWebView();
        webView.loadUrl("file:///android_asset/scanner.html");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        // This code remains the same as before
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // We still need to grant the WebView's internal request
                request.grant(request.getResources());
            }
        });
    }

    // The WebAppInterface and onBackPressed methods remain completely unchanged
    public class WebAppInterface {
        private final AppCompatActivity context;

        WebAppInterface(AppCompatActivity c) { context = c; }

        @JavascriptInterface
        public void savePdf(String base64String, String fileName) {
            context.runOnUiThread(() -> {
                try {
                    File file = new File(context.getExternalFilesDir(null), fileName);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    String base64Pdf = base64String.substring(base64String.indexOf(",") + 1);
                    byte[] pdfAsBytes = Base64.decode(base64Pdf, Base64.DEFAULT);

                    outputStream.write(pdfAsBytes);
                    outputStream.close();

                    Toast.makeText(context, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
