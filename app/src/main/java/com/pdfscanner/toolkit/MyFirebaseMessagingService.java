// File: app/src/main/java/com/pdfscanner/toolkit/MyFirebaseMessagingService.java
package com.pdfscanner.toolkit;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // This is called when a message is received.
        // If the app is in the foreground, you could build a custom notification here.
        // If the app is in the background, FCM handles showing the notification automatically.
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        // This is called when a new FCM registration token is generated for the device.
        // You would typically send this token to your own server to store it for targeted messaging.
        Log.d(TAG, "Refreshed token: " + token);
    }
}
