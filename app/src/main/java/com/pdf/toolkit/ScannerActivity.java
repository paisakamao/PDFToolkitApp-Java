// Paste this code into ScannerActivity.java

package com.yourapp.name; // <-- IMPORTANT: Make sure this matches your app's package name

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

public class ScannerActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line links this Java file to the XML layout we will create in the next step
        setContentView(R.layout.activity_scanner);

        // Hide the top bar (ActionBar) for a full-screen scanner experience
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException e) {
            // No action needed, it's already null
        }


        // Find the WebView in our layout and set it up
        webView = findViewById(R.id.webView);
        setupWebView();

        // Load the scanner HTML file from your project's 'assets' folder
        webView.loadUrl("file:///android_asset/scanner.html");
    }

    /**
     * Configures the WebView with all necessary settings for the scanner to work.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        // Enable JavaScript, which is required for the scanner logic
        webView.getSettings().setJavaScriptEnabled(true);
        // Enable DOM Storage for libraries like jsPDF to work
        webView.getSettings().setDomStorageEnabled(true);
        // Allow video to play automatically without user interaction
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        // This is the CRITICAL part: it creates the "Android" bridge object in JavaScript
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // This client handles browser-level events, like permission requests
        webView.setWebChromeClient(new WebChromeClient() {
            // This method is called when the HTML/JavaScript tries to access the camera
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Grant the camera permission to the WebView
                request.grant(request.getResources());
            }
        });
    }

    /**
     * This is the "bridge" class. Public methods inside this class with the @JavascriptInterface
     * annotation can be called directly from the JavaScript code in the WebView.
     */
    public class WebAppInterface {
        private final AppCompatActivity context;

        WebAppInterface(AppCompatActivity c) {
            context = c;
        }

        /**
         * This method is called from JavaScript to save the generated PDF.
         * It receives the PDF as a Base64 encoded string.
         *
         * @param base64String The PDF file, encoded as a Base64 string.
         * @param fileName The suggested file name for the PDF.
         */
        @JavascriptInterface
        public void savePdf(String base64String, String fileName) {
            // We need to run UI-related code (like Toasts) on the main thread
            context.runOnUiThread(() -> {
                try {
                    // Define the file path in the app's external storage directory
                    File file = new File(context.getExternalFilesDir(null), fileName);
                    FileOutputStream outputStream = new FileOutputStream(file);

                    // The Base64 string from the web includes a prefix like "data:application/pdf;base64,"
                    // We need to remove it before decoding.
                    String base64Pdf = base64String.substring(base64String.indexOf(",") + 1);
                    byte[] pdfAsBytes = Base64.decode(base64Pdf, Base64.DEFAULT);

                    // Write the bytes to the file and close the stream
                    outputStream.write(pdfAsBytes);
                    outputStream.close();

                    // Show a success message to the user
                    Toast.makeText(context, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

                    // Optionally, you could automatically close the scanner after saving
                    // context.finish();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Handles the device's back button. If the WebView can go back (e.g., from the
     * preview screen to the camera), it will. Otherwise, it closes the activity.
     */
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
