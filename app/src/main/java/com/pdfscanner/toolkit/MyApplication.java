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

    // --- YOUR ORIGINAL AD INITIALIZATION CODE (RESTORED) ---
    private static final AtomicBoolean isMobileAdsInitialized = new AtomicBoolean(false);
    private static final List<OnAdInitializedCallback> adInitializedCallbacks = new ArrayList<>();
    private AppOpenAdManager appOpenAdManager;

    // --- CACHING LOGIC (CORRECTLY ADDED) ---
    private static MyApplication instance;
    private List<FileItem> fileCache = null;

    @Override
    public void onCreate() {
        super.onCreate();
        // Set the static instance for caching
        instance = this;

        // Your original Firebase and Ad initialization logic
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
     * YOUR ORIGINAL STATIC METHOD (RESTORED)
     * This will fix the build errors in MainActivity, HomeActivity, and PdfViewerActivity.
     */
    public static void executeWhenAdSDKReady(OnAdInitializedCallback callback) {
        if (isMobileAdsInitialized.get()) {
            callback.onAdInitialized();
        } else {
            adInitializedCallbacks.add(callback);
        }
    }

    // --- METHODS FOR FILE CACHING (CORRECTLY ADDED) ---

    /**
     * Gets the singleton instance of the Application.
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
}
