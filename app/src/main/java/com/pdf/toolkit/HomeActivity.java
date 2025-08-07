package com.pdf.toolkit;

// Add all necessary imports for the final version
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
// ... (Your other existing imports are correct) ...

public class HomeActivity extends AppCompatActivity {
    // ... all your existing variables are correct ...
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- THIS IS THE FINAL, SIMPLE, AND CORRECT SPLASH SCREEN LOGIC ---

        // 1. Go edge-to-edge. MUST be called before super.onCreate().
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2. Install the splash screen. The system will read your theme,
        //    show your static image, and handle the timing automatically.
        SplashScreen.installSplashScreen(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // --- ALL THE LOTTIE AND ANIMATION LOGIC IS GONE ---
        // The system handles everything now. It's much cleaner and faster.

        // --- ALL YOUR ORIGINAL SETUP CODE RUNS AS NORMAL ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("PDF Toolkit");
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitle_Large);
        setSupportActionBar(toolbar);

        MobileAds.initialize(this, initializationStatus -> {});
        setupRemoteConfigAndLoadAd();

        scannerLauncher = registerForActivityResult(/* ... */);

        setupCardListeners();
        setupPrivacyPolicyLink();
    }
    
    // --- The rest of your HomeActivity.java file is unchanged and correct ---
}