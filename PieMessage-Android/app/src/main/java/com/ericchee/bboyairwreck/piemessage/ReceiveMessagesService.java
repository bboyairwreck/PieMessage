package com.ericchee.bboyairwreck.piemessage;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ReceiveMessagesService extends IntentService {
    public static final String TAG = ReceiveMessagesService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    Callbacks activity;
    Socket socket;

    public ReceiveMessagesService() {
        super("ReceiveMessagesService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "starting service");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "ReceiveService created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "binding activity");
        return mBinder;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        activity = null;
        Log.i(TAG, "unbinding service and setting activity callback to null");
        super.unbindService(conn);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            Log.i(TAG, "onHandleIntent");
            try {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String socketAddress = sharedPreferences.getString(getString(R.string.pref_socket_address_key), "127.0.0.1");
                socket = new Socket(socketAddress, 5000);
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Print if we got connected at <Date>
                String inputString = input.readLine();
                Log.i(TAG, inputString);

                // Sent to the server what device type it is
                JSONObject deviceTypeJSON = new JSONObject();
                deviceTypeJSON.put("deviceType", "MOBILE");
                deviceTypeJSON.put("action", "incoming");
                output.println(deviceTypeJSON.toString());

                // Log if Server registered self as a Mobile Client
                String isRegistered = input.readLine();
                Log.i(TAG, isRegistered);

                int nullCount = 0;
                while (true) {
                    Log.i(TAG, "listening for incoming messages");
                    String receivedMessageString = input.readLine();

                    if (receivedMessageString == null) {
                        // if null count
                        nullCount++;
                        if (nullCount > 50) {
                            Log.i(TAG, "Lost connection");
                            socket.close();
                            break;
                        }
                    } else {
                        Log.i(TAG, "Service received incoming message(s)");

//                        JSONObject receivedMessagesJSON = new JSONObject(receivedMessageString);
//
//                        JSONArray foundMessagesJSON = recievedMessagesJSON.getJSONArray("incomingMessages");
//
//                        for (int i = 0; i < foundMessagesJSON.length(); i++) {
//                            JSONObject foundMessageJSON = (JSONObject) foundMessagesJSON.get(i);
//                            Log.i(TAG, foundMessageJSON.getString("text"));
//                        }

                        PieSQLiteOpenHelper dbHelper = PieMessageApplication.getInstance().getDbHelper();
                        boolean hasIncomingMessages = dbHelper.insertMessagesFromJSONString(receivedMessageString);

                        // Notify Activity that service received messages
                        if (activity != null) {
                            Log.d(TAG, "Service Async about to call activity callback");
                            activity.onReceivedMessages(receivedMessageString);
                        } else {

                            // update chats map
                            dbHelper.getAllChats();

                            if (hasIncomingMessages) {
                                Message lastMessage = dbHelper.getLastMessage();
                                Chat chat = PieMessageApplication.getInstance().getChatsMap().get(lastMessage.cROWID);
                                String handlesString = chat.getHandlesString();
                                postNotification(handlesString, lastMessage.text, lastMessage.cROWID);
                                playNotification();
                            }

                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class LocalBinder extends Binder {
        public ReceiveMessagesService getServiceInstance() {
            return ReceiveMessagesService.this;
        }
    }

    public void registerActivity(Activity activity) {
        Log.i(TAG, "Registering Activity");
        this.activity = (Callbacks) activity;
    }

    public void unregisterActivity() {
        Log.i(TAG, "Unregistering Activity");
        this.activity = null;
    }

    public interface Callbacks {
        void onReceivedMessages(String receiveMessagesJsonString);
    }

    private void playNotification() {
        Log.i(TAG, "Playing notification sound");
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void postNotification(String handleID, String message, long cROWID) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.notif_icon)
                        .setContentTitle(handleID)
                        .setContentText(message)
                        .setTicker("New message from " + handleID + " - \n" + message)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_VIBRATE);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MessageActivity.class);
        resultIntent.putExtra(Constants.chatROWID, cROWID);
        resultIntent.putExtra(Constants.chatHandlesString, handleID);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MessageActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);   // this removes notification on tap
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        Notification notification = mBuilder.build();
        mNotificationManager.notify(0, notification);
    }
}
