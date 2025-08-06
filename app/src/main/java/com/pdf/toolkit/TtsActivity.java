package com.pdf.toolkit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TtsActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        // This allows the activity to be closed by tapping outside of it
        setFinishOnTouchOutside(true);

        // Get data from the Intent
        String urlToLoad = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        // Find views from the new layout
        WebView webView = findViewById(R.id.tts_webview);
        TextView titleView = findViewById(R.id.tts_title);
        ImageButton closeButton = findViewById(R.id.tts_close_btn);

        // Set the title in the header
        if (title != null) {
            titleView.setText(title);
        }

        // Set the close button's action
        closeButton.setOnClickListener(v -> finish());

        // Configure and load the WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        if (urlToLoad != null && !urlToLoad.isEmpty()) {
            webView.loadUrl(urlToLoad);
        }
    }
}
