package com.example.chatapp.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

public class Util {

    public static boolean connectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null) {

            return connectivityManager.getActiveNetworkInfo().isAvailable();

        } else
             return  false;
    }

    public static void updateDeviceToken(final Context context, String token) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if(currentUser!=null) {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference databaseReference = rootRef.child(NodeNames.TOKEN).child(currentUser.getUid());

            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(NodeNames.DEVICE_TOKEN, token);

            databaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(!task.isSuccessful()) {
                        Toast.makeText(context, context.getString(R.string.failed_to_save_device_token,task.getException()), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public static void sendNotification(Context context, String title, String message, String userId) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = rootRef.child(NodeNames.TOKEN).child(userId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(NodeNames.DEVICE_TOKEN).getValue()!=null) {
                    String deviceToken = snapshot.child(NodeNames.DEVICE_TOKEN).getValue().toString();

                    //main notification jsonObject
                    JSONObject notification = new JSONObject();
                    //data for main notification jsonObject
                    JSONObject notificationData = new JSONObject();

                    try {
                        //put title and message to notificationData object and then put notificationData to notification
                        notificationData.put(Constants.NOTIFICATION_TITLE, title);
                        notificationData.put(Constants.NOTIFICATION_MESSAGE, message);

                        notification.put(Constants.NOTIFICATION_TO, deviceToken);
                        notification.put(Constants.NOTIFICATION_DATA, notificationData);

                        //url for api
                        String fcmApiUrl = "https://fcm.googleapis.com/fcm/send";
                        String contentType = "application/json";

                        //a class from volley library
                        Response.Listener successListener = new Response.Listener() {
                            @Override
                            public void onResponse(Object response) {
                                Toast.makeText(context, "Notification sent", Toast.LENGTH_SHORT).show();
                            }
                        };

                        Response.ErrorListener failureListener = new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(context, context.getString(R.string.failed_to_send_notification, error.getMessage()),
                                        Toast.LENGTH_SHORT).show();
                            }
                        };

                        //use volley for api calling
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(fcmApiUrl, notification,
                                successListener, failureListener){

                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {

                                Map<String, String> params = new HashMap<>();
                                params.put("Authorization", "key=" + Constants.FIREBASE_KEY);
                                params.put("Sender", "id=" + Constants.SENDER_ID);
                                params.put("Content-Type", contentType);

                                return params;
                            }
                        };

                        //to make a request, make a requestQueue
                        RequestQueue requestQueue = Volley.newRequestQueue(context);
                        requestQueue.add(jsonObjectRequest);

                    } catch (JSONException e) {
                        Toast.makeText(context,
                                context.getString(R.string.failed_to_send_notification, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, context.getString(R.string.failed_to_send_notification, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void updateChatDetails(Context context, String currentUserId, String chatUserId, String lastMessage) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatRef = rootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);

        //update the unread count of chatUser
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentCount = "0";
                if(snapshot.child(NodeNames.UNREAD_COUNT).getValue() != null) {
                    currentCount = snapshot.child(NodeNames.UNREAD_COUNT).getValue().toString();
                }

                Map chatMap = new HashMap<>();
                chatMap.put(NodeNames.TIME_STAMP, ServerValue.TIMESTAMP);
                chatMap.put(NodeNames.UNREAD_COUNT, Integer.valueOf(currentCount)+1);
                chatMap.put(NodeNames.LAST_MESSAGE, lastMessage);
                chatMap.put(NodeNames.LAST_MESSAGE_TIME, ServerValue.TIMESTAMP);

                Map chatUserMap = new HashMap();
                chatUserMap.put(NodeNames.CHATS + "/" + chatUserId + "/" + currentUserId, chatMap);

                rootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if(error != null) {
                            Toast.makeText(context, context.getString(R.string.something_went_wrong, error.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, context.getString(R.string.something_went_wrong, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String getTimeAgo (long time) {
        final int SECOND_MILLIS = 1000;  //milliseconds in a second
        final int MINUTE_MILLIS = 60 * SECOND_MILLIS;   //milliseconds in a minute
        final int HOUR_MILLIS = 60 * MINUTE_MILLIS;   //milliseconds in an hour
        final int DAY_MILLIS = 24 * HOUR_MILLIS;   //milliseconds in a day
        final int MONTH_MILLIS = 30 * DAY_MILLIS;   //milliseconds in a month
        final int YEAR_MILLIS = 12 * MONTH_MILLIS;   //milliseconds in a year

        time *= 1000;   //convert time to milliseconds

        //current time
        long now = System.currentTimeMillis();

        if(time > now || time <= 0) {
            return "";
        }

        final long diff = now - time;

        if(diff < MINUTE_MILLIS){
            return "just now";
        }
        else if(diff < 2*MINUTE_MILLIS){
            return "a minute ago";
        }
        else if(diff < 59*MINUTE_MILLIS){
            return diff/MINUTE_MILLIS + " minutes ago";
        }
        else if(diff < 2*HOUR_MILLIS) {
            return "an hour ago";
        }
        else if(diff < 24*HOUR_MILLIS){
            return diff/HOUR_MILLIS + " hours ago";
        }
        else if(diff < 2*DAY_MILLIS) {
            return "yesterday";
        }
        else if(diff < 30 * DAY_MILLIS) {
            return diff/DAY_MILLIS + " days ago";
        }
        else if(diff < 2*MONTH_MILLIS) {
            return "a month ago";
        }
        else if(diff < YEAR_MILLIS) {
            return diff/MONTH_MILLIS + " months ago";
        }
        else if(diff < 2*YEAR_MILLIS){
            return "a year ago";
        }
        else
            return diff/YEAR_MILLIS + " years ago";
    }
}
