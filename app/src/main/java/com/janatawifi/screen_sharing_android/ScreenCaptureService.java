package com.janatawifi.screen_sharing_android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

public class ScreenCaptureService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("TAG", "onBind: Inside On Bind Method of Screen Capture Service Class");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return startMediaCaptureServices();
    }

    private int startMediaCaptureServices() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = "My Foreground Service";
            String description = "My Foreground Service Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("my_foreground_service", name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Building the notification
        Notification.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(this, "my_foreground_service");
        } else {
            // For devices running on an Android version lower than Oreo (API 26)
            notificationBuilder = new Notification.Builder(this);
        }

        Notification notification = notificationBuilder
                .setContentTitle("Foreground Service")
                .setContentText("Doing some work...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Set the icon
                .setOngoing(true)
                .build();

        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }
        else {
            startForeground(1, notification);
        }

        return START_STICKY;
    }
}
