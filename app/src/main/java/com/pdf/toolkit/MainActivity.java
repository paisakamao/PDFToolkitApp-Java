package com.pdf.toolkit;

// All your existing, correct imports
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // <-- NEW IMPORT for remembering files
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet; // <-- NEW IMPORT
import java.util.Set;    // <-- NEW IMPORT

public class MainActivity extends AppCompatActivity {

    // All your existing variables are correct and remain the same
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    public static final String EXTRA_HTML_FILE = "com.pdf.toolkit.HTML_FILE_TO_LOAD";
    private String pendingBase64Data;
    private String pendingFileName;
    private PermissionRequest currentPermissionRequest;

    // All your existing permission launchers are correct and remain the same
    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> { /* ... your code ... */ });
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> { /* ... your code ... */ });
    private final ActivityResultLauncher<String> microphonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> { /* ... your code ... */ });


    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Your entire onCreate method is already correct and does not need to be changed.
        // I am including it in full here for completeness.
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
                currentPermissionRequest = request;
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                        } else {
                            request.grant(request.getResources());
                        }
                        return;
                    }
                }
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
    
    // Your JSBridge is correct and remains the same
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

    // --- START: THIS IS THE UPDATED saveFile FUNCTION ---
    private void saveFile(String base64Data, String fileName) {
        runOnUiThread(() -> {
            try {
                byte[] fileAsBytes = Base64.decode(base64Data, Base64.DEFAULT);
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                try (OutputStream os = new FileOutputStream(file)) {
                    os.write(fileAsBytes);
                }

                // THIS IS THE NEW PART: After saving the file, we add it to our recent files list.
                addRecentFile(file.getAbsolutePath());
                
                Toast.makeText(this, "File saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving file", Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- START: THIS IS THE NEW FUNCTION TO REMEMBER FILES ---
    private void addRecentFile(String path) {
        // This is like opening our "notebook"
        SharedPreferences prefs = getSharedPreferences("RecentFiles", MODE_PRIVATE);
        // We get the existing set of file paths
        Set<String> recentFiles = new HashSet<>(prefs.getStringSet("files", new HashSet<>()));
        // We add the new file's path to the set
        recentFiles.add(path);
        // We save the updated set back to the notebook
        prefs.edit().putStringSet("files", recentFiles).apply();
    }
    // --- END: THIS IS THE NEW FUNCTION ---

    // Your onBackPressed method is correct and remains the same
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
