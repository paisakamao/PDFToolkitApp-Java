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

    // ✅ Google Test App Open Ad ID (safe fallback)
    private static final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";

    private String adUnitId;

    public AppOpenAdManager(Application myApplication) {
        this.myApplication = myApplication;

        // Pull from Remote Config first
        String rcAdUnitId = FirebaseRemoteConfig.getInstance().getString("android_app_open_ad_id");

        // ✅ Always fallback to test ID if RC is empty
        if (rcAdUnitId == null || rcAdUnitId.isEmpty()) {
            Log.w(TAG, "Remote Config app open ad ID empty. Using test ID.");
            adUnitId = TEST_AD_UNIT_ID;
        } else {
            adUnitId = rcAdUnitId;
        }

        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // Preload on startup
        loadAd();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App in foreground, checking ad...");
        showAdIfAvailable();
    }

    public void loadAd() {
        if (isLoadingAd || isAdAvailable()) return;

        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.e(TAG, "No valid Ad Unit ID. Skipping load.");
            return;
        }

        isLoadingAd = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(
                myApplication, adUnitId, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        isLoadingAd = false;
                        Log.d(TAG, "App Open Ad loaded successfully.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoadingAd = false;
                        Log.e(TAG, "App Open Ad failed: " + loadAdError.getMessage());
                    }
                });
    }

    private boolean isAdAvailable() {
        return appOpenAd != null;
    }

    private void showAdIfAvailable() {
        if (isShowingAd || !isAdAvailable() || currentActivity == null) {
            Log.d(TAG, "Ad not ready to show.");
            loadAd();
            return;
        }

        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed.");
                appOpenAd = null;
                isShowingAd = false;
                loadAd(); // Load next one
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App Open Ad failed to show: " + adError.getMessage());
                appOpenAd = null;
                isShowingAd = false;
                loadAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad showed.");
                isShowingAd = true;
            }
        });

        isShowingAd = true;
        appOpenAd.show(currentActivity);
    }

    // --- Activity Lifecycle Callbacks ---
    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
