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

    private AdManager() {}

    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    // --- METHOD (UPDATED) ---
    public void loadInterstitialAd(Context context) {
        if (mInterstitialAd != null) {
            return;
        }

        String adUnitId = FirebaseRemoteConfig.getInstance().getString("android_interstitial_ad_id");

        // THIS IS THE FIX: If the Ad Unit ID is empty, log an error and do not proceed.
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.e(TAG, "Interstitial Ad Unit ID from Remote Config is empty. Ad will not be loaded.");
            return;
        }
        // --- END OF FIX ---

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, adUnitId, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "Interstitial ad loaded.");
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
                    Log.d(TAG, "Ad was dismissed.");
                    mInterstitialAd = null;
                    loadInterstitialAd(activity);
                    if (onAdDismissed != null) {
                        onAdDismissed.run();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                    mInterstitialAd = null;
                    loadInterstitialAd(activity);
                    if (onAdDismissed != null) {
                        onAdDismissed.run();
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.");
                }
            });
            mInterstitialAd.show(activity);
        } else {
            Log.d(TAG, "Interstitial ad was not ready. Skipping.");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
        }
    }
}