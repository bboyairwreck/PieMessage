package com.ericchee.bboyairwreck.piemessage;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by eric on 11/18/15.
 */
public class SendMessageTask extends AsyncTask {
    public static final String TAG = SendMessageTask.class.getSimpleName();
    Socket socket;
    MessageActivity activity;
    String targetPhoneNumber;
    String message;

    public SendMessageTask(MessageActivity activity, String targetPhoneNumber, String message) {
        this.activity = activity;
        this.targetPhoneNumber = targetPhoneNumber;
        this.message = message;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        try {
            Log.i(TAG, "Entering Client");
            socket = new Socket(Constants.socketAddress, 5000);
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Print if we got connected at <Date>
            String inputString = input.readLine();
            Log.i(TAG, inputString);

            // Sent to the server what device type it is
            JSONObject deviceTypeJSON = new JSONObject();
            deviceTypeJSON.put("deviceType", "MOBILE");
            deviceTypeJSON.put("action", "outgoing");
            output.println(deviceTypeJSON.toString());

            // Log if Server registered self as a Mobile Client
            String isRegistered = input.readLine();
            Log.i(TAG, isRegistered);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("number", targetPhoneNumber);
            jsonObject.put("message", message);
            output.println(jsonObject.toString());    // send message

            int nullCount = 0;
            while (!isCancelled()) {
                String messageStatusString = input.readLine();

                if (messageStatusString == null) {
                    // if null count
                    nullCount++;
                    if (nullCount > 50) {
                        Log.i(TAG, "Lost connection");
                        socket.close();
                        break;
                    }
                } else {
                    final JSONObject messageStatusJSON = new JSONObject(messageStatusString);

                    if (messageStatusJSON.getString("messageStatus").equals("successful")) {
                        Log.i(TAG, "Message was successfully sent");
                    } else {
                        Log.i(TAG, "Message did NOT send successfully");
                    }

                    // Update list message status and listView
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                activity.messageStatusReceived(messageStatusJSON);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    Log.i(TAG, "Response - " + messageStatusString);
                    break;
                }

            }


//            Log.i(TAG, "sending message");
//            output.println("killThisThread-23");

            Log.i(TAG, "Closing socket");
            socket.close();

        } catch (IOException e) {
            Log.e(TAG, "There was an error" + e.getStackTrace().toString());
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void sleep(long sleepDuration) {
        try
        {
            Thread.sleep(sleepDuration);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
