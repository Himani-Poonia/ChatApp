package com.example.chatapp.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.chatapp.Login.LoginActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.Util;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ChatMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        Util.updateDeviceToken(this, s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getData().get(Constants.NOTIFICATION_TITLE);
        String message = remoteMessage.getData().get(Constants.NOTIFICATION_MESSAGE);

        Intent intent = new Intent(this, LoginActivity.class);

        //when clicking on notification,
        // we can not send intent directly, so we have to wrap the intent class around the pending intent class

        //A Pending Intent specifies an action to take in the future. It lets you pass a future Intent to
        // another application and allow that application to execute that Intent as if it had the same permissions as your application,
        // whether or not your application is still around when the Intent is eventually invoked
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_ONE_SHOT);
        //flag_one_shot specifies that this pendingIntent can only be used once

        //to manage the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //to play a sound when notification is received
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        //to build the notification
        NotificationCompat.Builder notificationBuilder;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.CHANNEL_ID, Constants.CHANNEL_NAME
                    , NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.setDescription(Constants.CHANNEL_DESC);
            notificationManager.createNotificationChannel(notificationChannel);
            notificationBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);

        }
        else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        //form a notification (UI)
        notificationBuilder.setSmallIcon(R.drawable.ic_chat);
        notificationBuilder.setColor(getResources().getColor(R.color.colorPrimary));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSound(defaultSound);
        notificationBuilder.setContentIntent(pendingIntent);

        if(message.startsWith("https://firebasestorage.")) {

            try {

                //set image or video thumbnail in the notification
                NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                //bigPictureStyle needs the bitmap of the picture

                Glide.with(this)
                        .asBitmap()
                        .load(message)
                        .into(new CustomTarget<Bitmap>(200, 100) {  //create a custom bitmap for the url (message)
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                bigPictureStyle.bigPicture(resource);
                                notificationBuilder.setStyle(bigPictureStyle);
                                notificationManager.notify(103, notificationBuilder.build());
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });


            } catch (Exception e) {

                notificationBuilder.setContentText("New File Received");
            }

        } else {
            notificationBuilder.setContentText(message);
            notificationManager.notify(103, notificationBuilder.build());
        }
    }
}