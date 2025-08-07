package com.pdf.toolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    public static final String EXTRA_HTML_FILE = "com.pdf.toolkit.HTML_FILE_TO_LOAD";

    private PermissionRequest currentPermissionRequest;
    private FirebaseRemoteConfig remoteConfig;

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

        remoteConfig = FirebaseRemoteConfig.getInstance();
        
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
                if (url != null && url.contains("unitools.html")) {
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                            currentPermissionRequest = request;
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                            } else {
                                request.grant(request.getResources());
                            }
                            return;
                        }
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

    public class JSBridge {
        private final Context context;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Handler handler = new Handler(Looper.getMainLooper());

        JSBridge(Context context) { this.context = context; }

        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName, String mimeType) {
            executor.execute(() -> {
                try {
                    byte[] fileAsBytes = Base64.decode(base64Data, Base64.DEFAULT);
                    Uri fileUri = saveFileToDownloads(fileAsBytes, fileName, mimeType);

                    if (fileUri != null) {
                        String uriString = fileUri.toString();
                        String jsCallback = String.format("javascript:onFileSaved('%s', '%s')", fileName, uriString);
                        handler.post(() -> {
                            Toast.makeText(context, "File saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                            webView.evaluateJavascript(jsCallback, null);
                        });
                    } else {
                        throw new Exception("Failed to get URI for saved file.");
                    }
                } catch (Exception e) {
                    handler.post(() -> Toast.makeText(context, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        }

        @JavascriptInterface
        public void previewFile(String uriString) {
            if (uriString == null || uriString.isEmpty()) {
                Toast.makeText(context, "Cannot view file: No URI provided.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Uri pdfUri = Uri.parse(uriString);
                Intent intent = new Intent(context, PdfViewerActivity.class);
                intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, pdfUri.toString());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Error opening PDF Viewer.", Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface
        public String getTtsToolUrl() {
            return remoteConfig.getString("tts_tool_url");
        }

        @JavascriptInterface
        public void openToolInBrowser(String url) {
            if (url == null || url.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "URL is not available.", Toast.LENGTH_SHORT).show()
                );
                return;
            }
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(context, R.color.card_background));
            builder.setShowTitle(true);
            builder.setStartAnimations(context, R.anim.slide_in_up, R.anim.stay);
            builder.setExitAnimations(context, R.anim.stay, R.anim.slide_out_down);
            builder.setUrlBarHidingEnabled(true);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(context, Uri.parse(url));
        }
    }

    private Uri saveFileToDownloads(byte[] data, String fileName, String mimeType) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDFToolkit");
        }

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new Exception("Failed to create new MediaStore record.");
        }
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new Exception("Failed to open output stream.");
            }
            outputStream.write(data);
        }
        return uri;
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
