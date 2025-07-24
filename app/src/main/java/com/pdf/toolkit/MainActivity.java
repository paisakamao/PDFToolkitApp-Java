package com.pdf.toolkit;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Add JS interface for file saving
        webView.addJavascriptInterface(new JSBridge(this), "Android");

        webView.loadUrl("file:///android_asset/index.html");
    }

    // Handle WebView back navigation
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // JavaScript interface class
    public static class JSBridge {
        private final Context context;

        JSBridge(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName) {
            try {
                byte[] decoded;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    decoded = Base64.getDecoder().decode(base64Data);
                } else {
                    decoded = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                }

                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                OutputStream os = new FileOutputStream(file);
                os.write(decoded);
                os.close();

                Toast.makeText(context, "File saved to Downloads", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}