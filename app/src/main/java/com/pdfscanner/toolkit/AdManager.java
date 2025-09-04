// Make sure this is your app's package name
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

// Singleton class to manage Interstitial Ads
public class AdManager {

    private static final String TAG = "AdManager";
    private static AdManager instance;
    private InterstitialAd mInterstitialAd;

    // Private constructor for Singleton pattern
    private AdManager() {}

    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    // Call this to pre-load an ad
    public void loadInterstitialAd(Context context) {
        // Avoid loading a new ad if one is already loaded or being loaded.
        if (mInterstitialAd != null) {
            return;
        }

        String adUnitId = FirebaseRemoteConfig.getInstance().getString("android_interstitial_ad_id");
        if (adUnitId.isEmpty()) {
            Log.e(TAG, "Interstitial Ad Unit ID from Remote Config is empty.");
            return;
        }

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

    // Call this when you want to show an ad
    public void showInterstitial(Activity activity, Runnable onAdDismissed) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Ad was dismissed.
                    Log.d(TAG, "Ad was dismissed.");
                    mInterstitialAd = null; // The ad can only be shown once.
                    loadInterstitialAd(activity); // Pre-load the next one.
                    if (onAdDismissed != null) {
                        onAdDismissed.run(); // Execute the callback
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Ad failed to show.
                    Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                    mInterstitialAd = null;
                    loadInterstitialAd(activity);
                    if (onAdDismissed != null) {
                        onAdDismissed.run(); // Still execute callback so the app doesn't get stuck
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Ad showed successfully.
                    Log.d(TAG, "Ad showed fullscreen content.");
                }
            });
            mInterstitialAd.show(activity);
        } else {
            Log.d(TAG, "Interstitial ad was not ready. Skipping.");
            if (onAdDismissed != null) {
                onAdDismissed.run(); // Execute callback immediately if no ad is ready
            }
        }
    }
}
