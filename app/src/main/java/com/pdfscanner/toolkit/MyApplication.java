// Create this new file: MyApplication.java
package com.pdfscanner.toolkit;

import android.app.Application;
import android.util.Log;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase Remote Config and set the defaults from our new XML file
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults); // <-- Uses the new file
        remoteConfig.fetchAndActivate();

        // Initialize the Google Mobile Ads SDK ONCE for the entire app.
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK Initialized.");
        });
    }
}
