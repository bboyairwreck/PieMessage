import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;

/**
 * Created by eric on 11/26/15.
 */
public class OSXIncomingMessageThread implements Runnable {
    public static final String ACTION_INCOMING = "incoming";
    public static final String REGISTERED_INCOMING_SUCCESS = "Registered as OSX Client - incoming";
    Thread thread;
    Socket socket;
    ResetStatus resetStatus = new ResetStatus();

    @Override
    public void run() {
        while (resetStatus.shouldReset) {
            resetStatus.shouldReset = false;

            try {
                socket = new Socket(Constants.socketAddress, 5000);
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // This will wait for server to send the string to the client has been made.
                String inputString = input.readLine();
                System.out.println(inputString);    // Server returns "You have connected at ..."

                // Tell server we are an OSX Client
                JSONObject deviceTypeJSON = new JSONObject();
                deviceTypeJSON.put("deviceType", "OSX_CLIENT");
                deviceTypeJSON.put("action", ACTION_INCOMING);
                output.println(deviceTypeJSON.toString());

                // Wait if registered as OSX Client
                String serverStatusResponse = input.readLine();
                System.out.println(serverStatusResponse);


                if (serverStatusResponse != null && serverStatusResponse.equals(REGISTERED_INCOMING_SUCCESS)) {

                    Connection connection = null;
                    try {
                        while (true) {
                            System.out.println("Starting another sql db connection");
                            Class.forName("org.sqlite.JDBC");

                            connection = DriverManager.getConnection("jdbc:sqlite:" + PieOSXClient.getHomeDirectory() + "/Library/Messages/chat.db");
                            if (connection != null) {

                                JSONArray foundMessagesJSON = new JSONArray();
                                new IncomingMessagesListener(connection, Constants.getNowEpochSeconds(), foundMessagesJSON, this.resetStatus);

                                // if found messages notify Server
                                if (foundMessagesJSON.length() > 0) {
                                    JSONObject response = new JSONObject();
                                    response.put("incomingMessages", foundMessagesJSON);
                                    response.put("incoming_action", "notify_mobile");

                                    // Send server the incoming messages
                                    output.println(response.toString());
                                }

                                System.out.println("Should Reset = " + resetStatus.shouldReset);

                                //                        for (int i = 0; i < foundMessagesJSON.length(); i++) {
                                //                            JSONObject foundMessageJSON = (JSONObject) foundMessagesJSON.get(i);
                                //                            System.out.println(foundMessageJSON.getString("text"));
                                //                        }

                                if (resetStatus.shouldReset) {
                                    // System has been idle for an hour. Reset connection.
                                    System.out.println("Notifying server that were online at " + new Date());
                                    JSONObject response = new JSONObject();
                                    response.put("incoming_action", "online");

                                    output.println(response.toString());
                                    // TODO maybe refactor should reset to reset over night?
                                } else {
                                    Thread.sleep(100);
                                }

                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // Server was not successfully registered
                    System.out.println("Did not connect Incoming thread. Server response was either null or wasn't registered correctly");
                }

                System.out.println("Lost connection of Incoming socket");
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }   // end of reset while loop
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public class ResetStatus {
        public boolean shouldReset;

        public ResetStatus() {
            shouldReset = true;
        }
    }
}
