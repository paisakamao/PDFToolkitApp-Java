package com.pdf.toolkit;

import android.Manifest;
import android.app.Activity; // Required for Activity.RESULT_OK
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    // --- START: CORRECTED FILE CHOOSER LOGIC ---
    // This launcher now correctly handles BOTH single and multiple file selections.
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback == null) {
                    return;
                }

                Uri[] results = null;
                // Check if the result is valid and there's data
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Check if multiple files were returned
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = result.getData().getClipData().getItemAt(i).getUri();
                        }
                    } else if (result.getData().getData() != null) {
                        // A single file was returned
                        results = new Uri[]{ result.getData().getData() };
                    }
                }

                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            });
    // --- END: CORRECTED FILE CHOOSER LOGIC ---

    // Launcher for the Permission Request
    private String urlToDownload;
    private String userAgentToDownload;
    private String contentDispositionToDownload;
    private String mimetypeToDownload;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    downloadFile(urlToDownload, userAgentToDownload, contentDispositionToDownload, mimetypeToDownload);
                } else {
                    Toast.makeText(this, "Permission denied. Download cannot continue.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 

        webView = findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> fp, FileChooserParams fcp) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = fp;
                try {
                    // --- START: MODIFICATION TO ALLOW MULTIPLE FILES ---
                    Intent intent = fcp.createIntent();
                    // This tells the file picker that it's okay to select more than one file
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    fileChooserLauncher.launch(intent);
                    // --- END: MODIFICATION TO ALLOW MULTIPLE FILES ---
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            urlToDownload = url;
            userAgentToDownload = userAgent;
            contentDispositionToDownload = contentDisposition;
            mimetypeToDownload = mimetype;

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { 
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    downloadFile(url, userAgent, contentDisposition, mimetype);
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            } else { 
                downloadFile(url, userAgent, contentDisposition, mimetype);
            }
        });

        webView.loadUrl("file:///android_asset/index.html"); 
    }

    private void downloadFile(String url, String userAgent, String contentDisposition, String mimetype) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimetype);
        String cookies = android.webkit.CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading file...");
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        request.setTitle(fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(request);
        Toast.makeText(getApplicationContext(), "Download Started...", Toast.LENGTH_LONG).show();
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
