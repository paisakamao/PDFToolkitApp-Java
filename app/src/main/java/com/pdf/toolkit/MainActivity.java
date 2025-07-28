package com.pdf.toolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    public static final String EXTRA_HTML_FILE = "com.pdf.toolkit.HTML_FILE_TO_LOAD";

    private String pendingBase64Data;
    private String pendingFileName;
    
    private PermissionRequest currentPermissionRequest;

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback == null) return;
                Uri[] results = null;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = result.getData().getClipData().getItemAt(i).getUri();
                        }
                    } else if (result.getData().getData() != null) {
                        results = new Uri[]{ result.getData().getData() };
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    saveFile(pendingBase64Data, pendingFileName);
                } else {
                    Toast.makeText(this, "Permission denied. File cannot be saved.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> microphonePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (currentPermissionRequest != null) {
                    if (isGranted) {
                        currentPermissionRequest.grant(currentPermissionRequest.getResources());
                    } else {
                        Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
                        currentPermissionRequest.deny();
                    }
                    currentPermissionRequest = null;
                }
            });

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 

        webView = findViewById(R.id.webView);
        WebView.setWebContentsDebuggingEnabled(true);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> fp, FileChooserParams fcp) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = fp;
                try {
                    Intent intent = fcp.createIntent();
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    fileChooserLauncher.launch(intent);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                String url = webView.getUrl();

                // === THIS IS THE KEY CHANGE ===
                // Only handle the permission request if it's coming from unitools.html
                if (url != null && url.contains("unitools.html")) {
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                            currentPermissionRequest = request;
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                            } else {
                                request.grant(request.getResources());
                            }
                            return; // We've handled the request
                        }
                    }
                }
                
                // For any other page (like index.html) or any other permission, do the default action.
                super.onPermissionRequest(request);
            }
        });

        webView.addJavascriptInterface(new JSBridge(this), "Android");
        
        Intent intent = getIntent();
        String htmlFileToLoad = intent.getStringExtra(EXTRA_HTML_FILE);
        if (htmlFileToLoad == null || htmlFileToLoad.isEmpty()) {
            htmlFileToLoad = "index.html";
        }
        webView.loadUrl("file:///android_asset/" + htmlFileToLoad); 
    }
    
    public class JSBridge {
        private final Context context;
        JSBridge(Context context) { this.context = context; }
        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveFile(base64Data, fileName);
                } else {
                    pendingBase64Data = base64Data;
                    pendingFileName = fileName;
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            } else {
                saveFile(base64Data, fileName);
            }
        }
    }

    private void saveFile(String base64Data, String fileName) {
        runOnUiThread(() -> {
            try {
                byte[] fileAsBytes = Base64.decode(base64Data, Base64.DEFAULT);
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                try (OutputStream os = new FileOutputStream(file)) {
                    os.write(fileAsBytes);
                }
                Toast.makeText(this, "File saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving file", Toast.LENGTH_LONG).show();
            }
        });
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
