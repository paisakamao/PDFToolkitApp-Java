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

    // --- YOUR ORIGINAL AD INITIALIZATION CODE (UNTOUCHED) ---
    private static final AtomicBoolean isMobileAdsInitialized = new AtomicBoolean(false);
    private static final List<OnAdInitializedCallback> adInitializedCallbacks = new ArrayList<>();
    private AppOpenAdManager appOpenAdManager;

    // --- NEW CODE FOR FILE CACHING ---
    private static MyApplication instance;
    private List<FileItem> fileCache = null;
    // --- END OF NEW CODE ---

    @Override
    public void onCreate() {
        super.onCreate();

        // --- NEW CODE TO INITIALIZE THE INSTANCE FOR CACHING ---
        instance = this;
        // --- END OF NEW CODE ---

        // Your original Firebase and Ad initialization logic (UNTOUCHED)
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

    /**
     * YOUR ORIGINAL STATIC METHOD FOR ADS (UNTOUCHED)
     * This will fix the build errors in your other activities.
     */
    public static void executeWhenAdSDKReady(OnAdInitializedCallback callback) {
        if (isMobileAdsInitialized.get()) {
            callback.onAdInitialized();
        } else {
            adInitializedCallbacks.add(callback);
        }
    }

    // --- NEW METHODS FOR FILE CACHING ---

    /**
     * Gets the singleton instance of the Application. This fixes the build error.
     * @return The MyApplication instance.
     */
    public static MyApplication getInstance() {
        return instance;
    }

    /**
     * Retrieves the cached list of files.
     * @return The list of FileItem, or null if not cached yet.
     */
    public List<FileItem> getFileCache() {
        return fileCache;
    }

    /**
     * Stores the scanned list of files in the cache.
     * @param fileCache The list of FileItem to cache.
     */
    public void setFileCache(List<FileItem> fileCache) {
        this.fileCache = fileCache;
    }

    /**
     * Clears the file cache, forcing a new scan on the next load.
     */
    public void clearFileCache() {
        this.fileCache = null;
    }
    // --- END OF NEW METHODS ---
}
