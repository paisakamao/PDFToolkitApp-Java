package com.pdfscanner.toolkit;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MyApplication extends Application {

    private AppOpenAdManager appOpenAdManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Firebase Remote Config
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Cache for 1 hour
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        remoteConfig.fetchAndActivate(); // ✅ Safe but async

        // 2. Initialize Google Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // 3. Initialize our Ad Managers
        // ✅ Keep reference to prevent garbage collection
        appOpenAdManager = new AppOpenAdManager(this);

        // ✅ Preload interstitial after ads SDK is ready
        AdManager.getInstance().loadInterstitialAd(this);
    }
}