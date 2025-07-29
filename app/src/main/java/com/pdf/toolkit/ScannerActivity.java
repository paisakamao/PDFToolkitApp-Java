// Paste this complete, corrected code into your ScannerActivity.java file

package com.pdf.toolkit; // <<< THIS IS THE MOST IMPORTANT FIX

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

// This import is also automatically handled when the package name is correct,
// but adding it explicitly can help.
import com.pdf.toolkit.R;

public class ScannerActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line links this Java file to the XML layout
        setContentView(R.layout.activity_scanner); // This will now find R.layout

        // Hide the top bar (ActionBar) for a full-screen scanner experience
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException e) {
            // No action needed, it's already null
        }

        // Find the WebView in our layout and set it up
        webView = findViewById(R.id.webView); // This will now find R.id
        setupWebView();

        // Load the scanner HTML file from your project's 'assets' folder
        webView.loadUrl("file:///android_asset/scanner.html");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });
    }

    public class WebAppInterface {
        private final AppCompatActivity context;

        WebAppInterface(AppCompatActivity c) {
            context = c;
        }

        @JavascriptInterface
        public void savePdf(String base64String, String fileName) {
            context.runOnUiThread(() -> {
                try {
                    File file = new File(context.getExternalFilesDir(null), fileName);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    String base64Pdf = base64String.substring(base64String.indexOf(",") + 1);
                    byte[] pdfAsBytes = Base64.decode(base64Pdf, Base64.DEFAULT);

                    outputStream.write(pdfAsBytes);
                    outputStream.close();

                    Toast.makeText(context, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            });
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
