package com.pdfscanner.toolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AdView mAdView;
    private ValueCallback<Uri[]> filePathCallback;
    public static final String EXTRA_HTML_FILE = "com.pdfscanner.toolkit.HTML_FILE_TO_LOAD";

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

        // --- BANNER AD IMPLEMENTATION (UPDATED) ---
        mAdView = findViewById(R.id.adView);
        String bannerAdId = remoteConfig.getString("android_banner_ad_id");

        // THIS IS THE FIX: Only load the ad if the ID from Firebase is not empty.
        // This prevents the app from crashing if the ID isn't fetched yet.
        if (bannerAdId != null && !bannerAdId.isEmpty()) {
            mAdView.setAdUnitId(bannerAdId);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else {
            // Hide the ad view if the ID is not available, so it doesn't leave an empty space.
            mAdView.setVisibility(View.GONE);
        }
        // --- END OF BANNER AD FIX ---

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

    private void showCustomExternalLinkDialog(final String url) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_external_link);
        // ... (rest of this method is unchanged)
    }

    private void openUrlInCustomTab(String url) {
        // ... (this method is unchanged)
    }

    public class JSBridge {
        private final Context context;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Handler handler = new Handler(Looper.getMainLooper());

        JSBridge(Context context) { this.context = context; }

        @JavascriptInterface
        public void showInterstitialAd() {
            handler.post(() -> {
                if (context instanceof Activity) {
                    AdManager.getInstance().showInterstitial((Activity) context, () -> {
                        webView.evaluateJavascript("javascript:onAdDismissed();", null);
                    });
                }
            });
        }

        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName, String mimeType) {
            // ... (this method is unchanged)
        }

        @JavascriptInterface
        public void previewFile(String uriString) {
             // ... (this method is unchanged)
        }

        @JavascriptInterface
        public void showTtsConfirmationDialog() {
            // ... (this method is unchanged)
        }
    }

    private Uri saveFileToDownloads(byte[] data, String fileName, String mimeType) throws Exception {
        // ... (this method is unchanged)
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