function handleFiles(newFiles) {
    // This new version ADDS files instead of replacing them.
    // This correctly handles both single and multiple file selections.
    const filesToAdd = Array.from(newFiles);
    
    filesToAdd.forEach(file => {
        // Add the file to our master list
        uploadedFiles.push(file);

        // Create and add the visual preview for this new file
        const previewItem = document.createElement('div');
        previewItem.className = 'file-preview-item';
        previewItem.innerHTML = `<span><i class="fas fa-file-alt"></i> ${file.name}</span><span class="status-badge">Ready</span>`;
        filePreviews.appendChild(previewItem);
    });

    // If there is at least one file in our master list, show the button.
    if (uploadedFiles.length > 0) {
        processBtn.style.display = 'inline-block';
    }
}```

---

#### **Step 2: The Final `MainActivity.java`**

This version has cleaner permission logic. It will only ask for permission on old devices when the user actually tries to save a file, which is a better user experience. On modern phones, it will continue to work correctly without asking.

*   **File to Update:** `app/src/main/java/com/pdf/toolkit/MainActivity.java`
*   **Action:** Replace the entire content of this file with the code below.

```java
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

    // These variables will temporarily hold the file data while we ask for permission.
    private String pendingBase64Data;
    private String pendingFileName;

    // Launcher for the file chooser (handles multi-select correctly)
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback != null) {
                    Uri[] uris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                    filePathCallback.onReceiveValue(uris);
                    filePathCallback = null;
                }
            });

    // Launcher for the Permission Request
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
                filePathCallback = fp;
                // This correctly tells the file picker to allow multiple selections
                Intent intent = fcp.createIntent();
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                fileChooserLauncher.launch(intent);
                return true;
            }
        });

        webView.addJavascriptInterface(new JSBridge(this), "Android");
        
        webView.loadUrl("file:///android_asset/index.html"); 
    }
    
    // The JavaScript Interface Class
    public class JSBridge {
        private final Context context;
        JSBridge(Context context) { this.context = context; }

        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName) {
            // On modern Android (10+), we don't need permission to save to Downloads.
            // On old Android (9 and below), we must check for permission first.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { 
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    performSave(base64Data, fileName);
                } else {
                    // We don't have permission. Store the data and ask for it.
                    pendingBase64Data = base64Data;
                    pendingFileName = fileName;
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            } else { 
                // We are on a modern phone, no permission check is needed.
                performSave(base64Data, fileName);
            }
        }
    }

    // This is the actual file-saving logic, using the modern MediaStore API.
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

    // Helper function to determine MIME type
    private String getMimeType(String fileName) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            return mime != null ? mime : "application/octet-stream";
        }
        return "application/octet-stream";
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
