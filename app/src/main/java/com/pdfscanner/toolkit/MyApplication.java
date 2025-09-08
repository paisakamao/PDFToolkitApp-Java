package com.pdfscanner.toolkit;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MyApplication extends Application {

    private AppOpenAdManager appOpenAdManager;
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Firebase Remote Config (this can still be done first)
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Remote Config fetch successful.");
                    } else {
                        Log.e(TAG, "Remote Config fetch failed.");
                    }
                });

        // 2. Initialize Google Mobile Ads and WAIT for it to be ready.
        MobileAds.initialize(this, initializationStatus -> {
             Log.d(TAG, "Mobile Ads SDK Initialized. All ads can now be loaded.");
             
             // --- THIS IS THE CRITICAL FIX ---
             // All ad-related initializations are now moved INSIDE this listener.
             // This guarantees they will only run AFTER the SDK is ready.
             
             // Initialize App Open Ad Manager
             appOpenAdManager = new AppOpenAdManager(this);
             
             // Pre-load the first Interstitial Ad
             AdManager.getInstance().loadInterstitialAd(this);
             // --- END OF CRITICAL FIX ---
        });
    }
}
