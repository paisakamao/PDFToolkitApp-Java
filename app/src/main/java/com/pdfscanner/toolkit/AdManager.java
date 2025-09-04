package com.pdfscanner.toolkit;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class AdManager {

    private static final String TAG = "AdManager";
    private static AdManager instance;
    private InterstitialAd mInterstitialAd;

    // ✅ Google-provided test interstitial ID
    private static final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    private AdManager() {}

    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    public void loadInterstitialAd(Context context) {
        if (mInterstitialAd != null) {
            return;
        }

        String adUnitId = FirebaseRemoteConfig.getInstance().getString("android_interstitial_ad_id");

        // ✅ Always fall back to test ID
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.w(TAG, "Remote Config adUnitId empty. Using test ID.");
            adUnitId = TEST_AD_UNIT_ID;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, adUnitId, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "Interstitial ad loaded successfully.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }

    public void showInterstitial(Activity activity, Runnable onAdDismissed) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed.");
                    mInterstitialAd = null;
                    loadInterstitialAd(activity);
                    if (onAdDismissed != null) onAdDismissed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                    mInterstitialAd = null;
                    loadInterstitialAd(activity);
                    if (onAdDismissed != null) onAdDismissed.run();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen.");
                }
            });
            mInterstitialAd.show(activity);
        } else {
            Log.w(TAG, "Interstitial ad not ready, running fallback.");
            if (onAdDismissed != null) onAdDismissed.run();
        }
    }
}