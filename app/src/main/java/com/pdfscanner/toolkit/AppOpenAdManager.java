// ❗ REPLACE the entire contents of AppOpenAdManager.java with this code.
package com.pdfscanner.toolkit;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class AppOpenAdManager implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private static final String TAG = "AppOpenAdManager";
    private final Application myApplication;
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private Activity currentActivity;
    private final String AD_UNIT_ID;

    public AppOpenAdManager(Application myApplication) {
        this.myApplication = myApplication;

        // --- ROBUSTNESS FIX: Three-level fallback for Ad Unit ID ---
        String adUnitId = FirebaseRemoteConfig.getInstance().getString("android_app_open_ad_id");
        if (adUnitId == null || adUnitId.isEmpty()) {
            // Level 2 fallback is handled by the defaults in MyApplication.
            // Level 3 is a hardcoded test ID as a final safety net.
            adUnitId = "ca-app-pub-3940256099942544/9257395921"; // Test ID
            Log.e(TAG, "App Open Ad ID from Remote Config is empty. Using hardcoded test ID.");
        }
        this.AD_UNIT_ID = adUnitId;
        // --- END OF FIX ---

        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        loadAd(); // Proactively load the first ad
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        showAdIfAvailable();
        Log.d(TAG, "App is in foreground.");
    }

    public void loadAd() {
        if (isLoadingAd || isAdAvailable() || AD_UNIT_ID.isEmpty()) {
            return;
        }

        isLoadingAd = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(
                myApplication, AD_UNIT_ID, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        isLoadingAd = false;
                        Log.d(TAG, "App Open Ad loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoadingAd = false;
                        Log.e(TAG, "App Open Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    private boolean isAdAvailable() {
        return appOpenAd != null;
    }

    private void showAdIfAvailable() {
        if (isShowingAd || !isAdAvailable()) {
            Log.d(TAG, "Ad not available or already showing.");
            loadAd();
            return;
        }

        appOpenAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        appOpenAd = null;
                        isShowingAd = false;
                        Log.d(TAG, "Ad dismissed.");
                        loadAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        appOpenAd = null;
                        isShowingAd = false;
                        Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                        loadAd();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed successfully.");
                    }
                });
        isShowingAd = true;
        appOpenAd.show(currentActivity);
    }

    // ActivityLifecycleCallbacks methods
    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}
    @Override
    public void onActivityStarted(@NonNull Activity activity) { currentActivity = activity; }
    @Override
    public void onActivityResumed(@NonNull Activity activity) { currentActivity = activity; }
    @Override
    public void onActivityPaused(@NonNull Activity activity) {}
    @Override
    public void onActivityStopped(@NonNull Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override
    public void onActivityDestroyed(@NonNull Activity) { currentActivity = null; }
}
