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
    private static MyApplication instance;
    private AppOpenAdManager appOpenAdManager;

    // --- File Cache ---
    private List<FileItem> fileCache = null;

    // --- Ad Initialization Logic ---
    private final AtomicBoolean isMobileAdsInitialized = new AtomicBoolean(false);
    private final List<OnAdInitializedCallback> adInitializedCallbacks = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize Firebase Remote Config
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        remoteConfig.fetchAndActivate();

        // Initialize Mobile Ads SDK. This is the core of the ad logic.
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK has been initialized.");

            // The SDK is ready, so we can now create ad managers and load ads.
            appOpenAdManager = new AppOpenAdManager(this);
            AdManager.getInstance().loadInterstitialAd(this);

            // Set the flag to true AFTER setting up the initial ads.
            isMobileAdsInitialized.set(true);

            // Execute any pending callbacks that were waiting for initialization.
            for (OnAdInitializedCallback callback : adInitializedCallbacks) {
                callback.onAdInitialized();
            }
            // Clear the list so callbacks are not held in memory unnecessarily.
            adInitializedCallbacks.clear();
        });
    }

    public static MyApplication getInstance() {
        return instance;
    }
    
    /**
     * This method is the gatekeeper for any ad loading. It ensures that no ad is
     * requested before the Mobile Ads SDK is fully ready.
     *
     * @param callback The code to be executed once the SDK is initialized.
     */
    public void executeWhenAdSDKReady(OnAdInitializedCallback callback) {
        // If the SDK is already initialized, run the callback immediately.
        if (isMobileAdsInitialized.get()) {
            callback.onAdInitialized();
        } else {
            // Otherwise, add the callback to a queue to be run later when initialization completes.
            adInitializedCallbacks.add(callback);
        }
    }

    // --- Methods to manage the file cache ---
    public List<FileItem> getFileCache() {
        return fileCache;
    }

    public void setFileCache(List<FileItem> fileCache) {
        this.fileCache = fileCache;
    }

    public void clearFileCache() {
        this.fileCache = null;
    }
}
