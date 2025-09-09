// File Location: app/src/main/java/com/pdfscanner/toolkit/MyApplication.java
package com.pdfscanner.toolkit;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    // --- THIS IS THE NEW, ROBUST INITIALIZATION LOGIC ---
    private static final AtomicBoolean isMobileAdsInitialized = new AtomicBoolean(false);
    private static final List<OnAdInitializedCallback> adInitializedCallbacks = new ArrayList<>();

    private AppOpenAdManager appOpenAdManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase Remote Config (this is safe to do first)
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        remoteConfig.fetchAndActivate();

        // Initialize the Ad SDK and use the completion listener to notify waiting activities.
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK is fully initialized.");
            isMobileAdsInitialized.set(true);

            // Pre-load ads that are needed for the whole app
            appOpenAdManager = new AppOpenAdManager(this);
            AdManager.getInstance().loadInterstitialAd(this);

            // Notify any waiting activities that it's now safe to create ads
            for (OnAdInitializedCallback callback : adInitializedCallbacks) {
                callback.onAdInitialized();
            }
            adInitializedCallbacks.clear();
        });
    }

    /**
     * Activities will call this method. It will either run the callback immediately
     * if ads are ready, or add it to a queue to be run later.
     */
    public static void executeWhenAdSDKReady(OnAdInitializedCallback callback) {
        if (isMobileAdsInitialized.get()) {
            callback.onAdInitialized();
        } else {
            adInitializedCallbacks.add(callback);
        }
    }
}
