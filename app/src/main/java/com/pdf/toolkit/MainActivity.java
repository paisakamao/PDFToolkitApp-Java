package com.pdf.toolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
                    Toast.makeText(this, "Permission granted. Saving file...", Toast.LENGTH_SHORT).show();
                    performSave(pendingBase64Data, pendingFileName);
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
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
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
                    performSave(base64Data, fileName);
                } else {
                    pendingBase64Data = base64Data;
                    pendingFileName = fileName;
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            } else { 
                performSave(base64Data, fileName);
            }
        }
    }

    private void performSave(String base64Data, String fileName) {
        runOnUiThread(() -> {
            try {
                Uri downloadsCollection;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadsCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                } else {
                    downloadsCollection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                }
                
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 1);
                }

                Uri fileUri = getContentResolver().insert(downloadsCollection, contentValues);
                
                if (fileUri == null) throw new Exception("Failed to create MediaStore record.");

                try (OutputStream os = getContentResolver().openOutputStream(fileUri)) {
                    if (os == null) throw new Exception("Failed to get output stream.");
                    byte[] fileAsBytes = Base64.decode(base64Data, Base64.DEFAULT);
                    os.write(fileAsBytes);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear();
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(fileUri, contentValues, null, null);
                }

                Toast.makeText(this, "File saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- START: THIS IS THE CORRECTED FUNCTION ---
    private String getMimeType(String fileName) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mime != null) {
                return mime;
            }
        }
        // This is the fallback return statement that was missing.
        // It handles files with no extension or unknown extensions.
        return "application/octet-stream";
    }
    // --- END: THIS IS THE CORRECTED FUNCTION ---

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
