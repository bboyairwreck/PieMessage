import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by eric on 11/24/15.
 */
public class OSXOutgoingMessageThread implements Runnable {
    public static final String ACTION_OUTGOING = "outgoing";
    public static final String REGISTERED_OUTGOING_SUCCESS = "Registered as OSX Client - outgoing";
    Thread thread;
    Socket socket;

    public OSXOutgoingMessageThread() {
    }

    @Override
    public void run() {
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
            deviceTypeJSON.put("action", ACTION_OUTGOING);
            output.println(deviceTypeJSON.toString());

            // Wait if registered as OSX Client
            String serverStatusResponse = input.readLine();
            System.out.println(serverStatusResponse);


            if (serverStatusResponse != null && serverStatusResponse.equals(REGISTERED_OUTGOING_SUCCESS)) {
                // Successfully registered as OSX Client
                int nullCount = 0;

                // Indefinite Loop that runs server functions
                while (true) {
                    // this will wait until a line of text has been sent
                    String serverResponse = input.readLine();

                    if (serverResponse == null) {
                        nullCount++;

                        if (nullCount > 50) {
                            break;
                        }
                    } else {
                        JSONObject jsonObject = new JSONObject(serverResponse);
                        System.out.println(jsonObject.toString());

                        String number = jsonObject.getString("number");
                        String message = jsonObject.getString("message");

                        long nowEpochSeconds = Constants.getNowEpochSeconds();

                        String userHome = PieOSXClient.getHomeDirectory();
                        String[] cmdString = {
                                "osascript",
                                userHome + "/messages.applescript",
                                message,
                                number
                        };

                        // Send iMessage via applescript
                        Runtime.getRuntime().exec(cmdString);

                        // Get metadata of recently sent message ^
                        JSONObject responseJSON = new JSONObject();
                        new OutgoingSQLiteChecker(responseJSON, message, nowEpochSeconds);

                        // Send response back to server
                        output.println(responseJSON.toString());
                        nullCount = 0;
                    }
                }
            } else {
                // Server was not successfully registered
                System.out.println("Did not connect Outgoing thread. Server response was either null or wasn't registered correctly");
            }

            System.out.println("Lost connection of outgoing socket");
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}
