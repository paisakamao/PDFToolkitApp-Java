package com.pdf.toolkit;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback != null) {
                    Uri[] uris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                    filePathCallback.onReceiveValue(uris);
                    filePathCallback = null;
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
                fileChooserLauncher.launch(fcp.createIntent());
                return true;
            }
        });

        webView.addJavascriptInterface(new JSBridge(this), "Android");
        
        webView.loadUrl("file:///android_asset/index.html"); 
    }
    
    // The JavaScript Interface Class using the Modern MediaStore API
    public class JSBridge {
        private final Context context;
        JSBridge(Context context) { this.context = context; }

        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName) {
            runOnUiThread(() -> {
                try {
                    // Get the correct URI for the Downloads folder based on Android version
                    Uri downloadsCollection;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        downloadsCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    } else {
                        // This is for older versions, though our minSdk is 24, this is safe
                        downloadsCollection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                    }
                    
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    contentValues.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
                    // For Android 10+, this tells the system the file is ready to be used
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 1);
                    }

                    // Insert the file into the MediaStore and get its URI
                    Uri fileUri = getContentResolver().insert(downloadsCollection, contentValues);
                    
                    if (fileUri == null) {
                        throw new Exception("Failed to create new MediaStore record.");
                    }

                    // Open an OutputStream to write the file data
                    try (OutputStream os = getContentResolver().openOutputStream(fileUri)) {
                        if (os == null) {
                            throw new Exception("Failed to get output stream.");
                        }
                        byte[] fileAsBytes = Base64.decode(base64Data, Base64.DEFAULT);
                        os.write(fileAsBytes);
                    }

                    // Now that we're done writing, mark the file as no longer pending
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear();
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0);
                        getContentResolver().update(fileUri, contentValues, null, null);
                    }

                    Toast.makeText(context, "File saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
        
        // Helper function to determine the MIME type from the file extension
        private String getMimeType(String fileName) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (extension != null) {
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            return "application/octet-stream"; // Default binary type
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
