package com.pdf.toolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    private String pendingBase64Data;
    private String pendingFileName;

    // For history/recent files
    public static final String PREFS_NAME = "recent_files";
    public static final String PREFS_KEY = "files";

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
                    Toast.makeText(this, "Permission granted. Saving file...", Toast.LENGTH_SHORT).show();
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
        WebView.setWebContentsDebuggingEnabled(true);

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
                try {
                    Intent intent = fcp.createIntent();
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multi-select
                    fileChooserLauncher.launch(intent);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new JSBridge(this), "Android");
        webView.loadUrl("file:///android_asset/index.html");

        // Recent Files Button (Make sure you add this Button in your activity_main.xml with id btn_recent_files)
        Button btnRecent = findViewById(R.id.btn_recent_files);
        btnRecent.setOnClickListener(v -> {
            startActivity(new Intent(this, RecentFilesActivity.class));
        });
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
                // Add to history
                addRecentFile(this, fileName, Uri.fromFile(file));
                Toast.makeText(this, "File saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving file", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Add file to recent/history list in SharedPreferences
    private void addRecentFile(Context context, String fileName, Uri fileUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> recent = prefs.getStringSet(PREFS_KEY, new LinkedHashSet<>());
        LinkedHashSet<String> newRecent = new LinkedHashSet<>();
        newRecent.add(fileName + "|" + fileUri.toString());
        if (recent != null) {
            for (String s : recent) {
                if (!s.equals(fileName + "|" + fileUri.toString())) {
                    newRecent.add(s);
                }
            }
        }
        // Limit history size to 20
        while (newRecent.size() > 20) {
            String last = null;
            for (String s : newRecent) last = s;
            if (last != null) newRecent.remove(last);
        }
        prefs.edit().putStringSet(PREFS_KEY, newRecent).apply();
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
