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

        // 1. Initialize Firebase Remote Config with Listeners for Debugging
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Cache for 1 hour
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        // Fetch and activate, but now with listeners to see what's happening
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "Remote Config parameters activated. Were they updated? " + updated);
                    } else {
                        Log.e(TAG, "Remote Config fetch failed");
                    }
                });

        // 2. Initialize Google Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {
             Log.d(TAG, "Mobile Ads SDK Initialized.");
        });

        // 3. Initialize our Ad Managers
        appOpenAdManager = new AppOpenAdManager(this);
        AdManager.getInstance().loadInterstitialAd(this);
    }
}
