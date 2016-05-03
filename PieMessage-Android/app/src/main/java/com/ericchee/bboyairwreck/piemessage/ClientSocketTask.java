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
 * Created by eric on 11/8/15.
 * This class is unused. // TODO DELETE THIS
 */
public class ClientSocketTask extends AsyncTask {
    public final static String TAG = ClientSocketTask.class.getSimpleName();
    public PrintWriter output;
    public BufferedReader input;
    public boolean keepAlive = true;
    public Socket socket;

    @Override
    protected Object doInBackground(Object[] objects) {
        final MessageActivity activity = (MessageActivity) objects[0];

        try {
            Log.i(TAG, "Entering Client");
            socket = new Socket("localhost", 4000);
            output = new PrintWriter(socket.getOutputStream(), true);
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


            activity.runOnUiThread(new Runnable() {
                public void run() {
//                    activity.addSocket(socket);    // Let activity know what socket to close onDestory
//                    activity.setBtnListener(output);    // Tell button to print to output on press
                }
            });

//            while (keepAlive) {
//                sleep(1000);
//            }

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
                    JSONObject messageStatusJSON = new JSONObject(messageStatusString);

                    if (messageStatusJSON.getString("messageStatus").equals("successful")) {
                        Log.i(TAG, "Message was successfully sent");
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
//                                activity.messageSentSuccessfully();
                            }
                        });
                    } else {
                        Log.i(TAG, "Message did NOT send successfully");
                    }
                    Log.i(TAG, "Response : " + messageStatusString);
                }

            }


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
