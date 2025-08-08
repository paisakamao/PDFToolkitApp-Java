package com.pdf.toolkit;;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 5000; // ms for 5s GIF

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashGif = findViewById(R.id.splash_gif);

        // Load GIF from res/raw using Glide
        Glide.with(this)
                .asGif()
                .load(R.raw.my_splash)
                .into(new DrawableImageViewTarget(splashGif));

        // Open HomeActivity after GIF finishes
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            finish();
        }, SPLASH_DURATION);
    }
}