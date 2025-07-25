package com.pdf.toolkit;

// --- START OF CORRECTIONS: ADDED MISSING IMPORTS ---
import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent; // This was the missing line that caused the build to fail
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
// --- END OF CORRECTIONS ---

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    // Launcher for the file chooser intent
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback != null) {
                    Uri[] uris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                    filePathCallback.onReceiveValue(uris);
                    filePathCallback = null;
                }
            });

    // Launcher for the Permission Request
    private String urlToDownload;
    private String userAgentToDownload;
    private String contentDispositionToDownload;
    private String mimetypeToDownload;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the download.
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

        // This handles the "Choose File" button
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> fp, FileChooserParams fcp) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = fp;
                try {
                    fileChooserLauncher.launch(fcp.createIntent());
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        // This Download Listener now checks for permission
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

    // Centralized download function
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
