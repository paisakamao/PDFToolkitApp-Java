package com.pdf.toolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
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
import androidx.core.content.FileProvider; // IMPORTANT: Import FileProvider

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    private String pendingBase64Data;
    private String pendingFileName;
    
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback != null) {
                    Uri[] uris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                    filePathCallback.onReceiveValue(uris);
                    filePathCallback = null;
                }
            });
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    saveFile(pendingBase64Data, pendingFileName);
                } else {
                    Toast.makeText(this, "Permission denied. File cannot be saved.", Toast.LENGTH_LONG).show();
                }
            });

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 

        webView = findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> fp, FileChooserParams fcp) {
                filePathCallback = fp;
                Intent intent = fcp.createIntent();
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                fileChooserLauncher.launch(intent);
                return true;
            }
        });

        webView.addJavascriptInterface(new JSBridge(this), "Android");
        
        webView.loadUrl("file:///android_asset/index.html"); 
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

        // --- START: THIS IS THE NEW PREVIEW FUNCTION ---
        @JavascriptInterface
        public void previewFile(String fileName) {
            runOnUiThread(() -> {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);

                if (!file.exists()) {
                    Toast.makeText(context, "Error: File not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Use the FileProvider to get a secure content URI
                Uri uri = FileProvider.getUriForFile(context, "com.pdf.toolkit.fileprovider", file);
                
                // Create an Intent to view the file
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String mimeType = getMimeType(fileName);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant permission to the receiving app

                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, "No app found to open this file type.", Toast.LENGTH_LONG).show();
                }
            });
        }
        // --- END: THIS IS THE NEW PREVIEW FUNCTION ---
    }

    private void saveFile(String base64Data, String fileName) {
        // ... (This function remains unchanged)
    }

    private String getMimeType(String fileName) {
        // ... (This function remains unchanged)
    }

    @Override
    public void onBackPressed() {
        // ... (This function remains unchanged)
    }
}
