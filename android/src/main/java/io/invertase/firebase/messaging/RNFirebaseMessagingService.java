package io.invertase.firebase.messaging;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

//import com.facebook.react.HeadlessJsTaskService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.invertase.firebase.Utils;
import me.leolin.shortcutbadger.ShortcutBadger;

public class RNFirebaseMessagingService extends FirebaseMessagingService {
  private static final String TAG = "RNFMessagingService";

  public static final String MESSAGE_EVENT = "messaging-message";
  public static final String NEW_TOKEN_EVENT = "messaging-token-refresh";
  public static final String REMOTE_NOTIFICATION_EVENT = "notifications-remote-notification";

  @Override
  public void onNewToken(String token) {
    Log.d(TAG, "onNewToken event received");

    Intent newTokenEvent = new Intent(NEW_TOKEN_EVENT);
    LocalBroadcastManager
      .getInstance(this)
      .sendBroadcast(newTokenEvent);
  }

  @Override
  public void onMessageReceived(RemoteMessage message) {
    Log.d(TAG, "onMessageReceived event received");

    if (message.getNotification() != null) {
      // It's a notification, pass to the Notifications module
      Intent notificationEvent = new Intent(REMOTE_NOTIFICATION_EVENT);
      notificationEvent.putExtra("notification", message);

      // Broadcast it to the (foreground) RN Application
      LocalBroadcastManager
        .getInstance(this)
        .sendBroadcast(notificationEvent);
    } else {
      // It's a data message
      // If the app is in the foreground we send it to the Messaging module
      if (Utils.isAppInForeground(this.getApplicationContext())) {
        Intent messagingEvent = new Intent(MESSAGE_EVENT);
        messagingEvent.putExtra("message", message);
        // Broadcast it so it is only available to the RN Application
        LocalBroadcastManager
          .getInstance(this)
          .sendBroadcast(messagingEvent);
      } else {
        try {
          
          int badge = 0;
          try {
            Log.d(TAG, "Received push data " + message.getData().toString());
            String data = message.getData().get("badge");
            if (data != null)
              badge = Integer.parseInt(data);
          } catch (Exception e) {
            Log.d(TAG, "JSON Parse error");
          }
          if (badge == 0) {
            Log.d(TAG, "Remove badge count");
            ShortcutBadger.removeCount(this.getApplicationContext());
          } else {
            Log.d(TAG, "Apply badge count: " + badge);
            ShortcutBadger.applyCount(this.getApplicationContext(), badge);
          }
          
          // If the app is in the background we send it to the Headless JS Service
          /*
          Intent headlessIntent = new Intent(
            this.getApplicationContext(),
            RNFirebaseBackgroundMessagingService.class
          );
          headlessIntent.putExtra("message", message);
          this
            .getApplicationContext()
            .startService(headlessIntent);
          HeadlessJsTaskService.acquireWakeLockNow(this.getApplicationContext());
          */
        } catch (IllegalStateException ex) {
          Log.e(
            TAG,
            "Background messages will only work if the message priority is set to 'high'",
            ex
          );
        }
      }
    }
  }
}
