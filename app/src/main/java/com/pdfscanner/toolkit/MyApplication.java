// Make sure this is your app's package name
package com.pdfscanner.toolkit;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

// Main Application class to initialize services
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Firebase Remote Config
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Cache for 1 hour
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults); // Your test IDs
        remoteConfig.fetchAndActivate(); // Fetch new values

        // 2. Initialize Google Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // 3. Initialize our Ad Managers
        // This starts the App Open Ad logic for the entire app
        new AppOpenAdManager(this);
        // This pre-loads the first interstitial ad
        AdManager.getInstance().loadInterstitialAd(this);
    }
}
