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
    private static final AtomicBoolean isMobileAdsInitialized = new AtomicBoolean(false);
    private static final List<OnAdInitializedCallback> adInitializedCallbacks = new ArrayList<>();
    private AppOpenAdManager appOpenAdManager;

    // --- NEW: Static instance and cache for the file list ---
    private static MyApplication instance;
    private List<FileItem> fileCache = null;

    @Override
    public void onCreate() {
        super.onCreate();
        // --- NEW: Initialize the static instance ---
        instance = this;

        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        remoteConfig.fetchAndActivate();

        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK is fully initialized.");
            isMobileAdsInitialized.set(true);

            appOpenAdManager = new AppOpenAdManager(this);
            AdManager.getInstance().loadInterstitialAd(this);

            for (OnAdInitializedCallback callback : adInitializedCallbacks) {
                callback.onAdInitialized();
            }
            adInitializedCallbacks.clear();
        });
    }

    // --- NEW: Static method to get the application instance ---
    public static MyApplication getInstance() {
        return instance;
    }

    // --- NEW: Methods to manage the file cache ---
    public List<FileItem> getFileCache() {
        return fileCache;
    }

    public void setFileCache(List<FileItem> fileCache) {
        this.fileCache = fileCache;
    }

    public void clearFileCache() {
        this.fileCache = null;
    }
    
    // --- Your existing Ad SDK Ready method ---
    public static void executeWhenAdSDKReady(OnAdInitializedCallback callback) {
        if (isMobileAdsInitialized.get()) {
            callback.onAdInitialized();
        } else {
            adInitializedCallbacks.add(callback);
        }
    }
}
