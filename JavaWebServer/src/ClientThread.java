import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

/**
 * Created by eric on 11/10/15.
 */
public class ClientThread implements Runnable {
    Socket threadSocket;
    Thread thread;
    DeviceType deviceType = DeviceType.UNDEFINED;
    Server server;
    PrintWriter output;
    BufferedReader input;
    String threadAction = null;

    public ClientThread(Socket socket) {
        this.threadSocket = socket;
        this.server = Server.getInstance();
    }

    @Override
    public void run() {
        try {
            // Create the streams
            output = new PrintWriter(threadSocket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(threadSocket.getInputStream()));

            // Tell the client that he/she has connected
            output.println("You have connected at: " + new Date());

            int nullCount = 0;

            // Indefinite Loop that runs server functions
            while (true) {
                // this will wait until a line of text has been sent
                String chatInput = input.readLine();    // TODO need to change name to be more generic for incoming & outgoing

                if (chatInput == null) {
                    // potentiall disconnected
                    nullCount++;

                    if (nullCount > 50) {
                        // 50 nulls to me is disconnection
                        break;
                    }
                } else {
                    // Connected and received an input
                    JSONObject jsonObject = new JSONObject(chatInput);
                    System.out.println(jsonObject.toString());

                    if (deviceType == DeviceType.UNDEFINED) {
                        // set Device type
                        String deviceTypeString = jsonObject.getString("deviceType");
                        String action = jsonObject.getString("action");
                        setDeviceType(deviceTypeString, action);

                    } else if (deviceType == DeviceType.MOBILE) {
                        // Received message from a Mobile client
                        if (this.threadAction.equals("outgoing")) {
                            // check if OSX output is set
                            if (server.hasOsxOutgoingOutput() && server.hasOsxOutgoingInput()) {
                                // Send typed message to OSX output
                                System.out.println("Forwarding ^ send msg request to OSX Client");
                                server.osxOutgoingOutput.println(chatInput);

                            } else {
                                System.out.println("ERROR: OSX input/output has not be set. Can't send client's message to OSX");
                                jsonObject.put("messageStatus", "unsuccessful");
                                jsonObject.put("messageID", "__noID__");
                                output.println(jsonObject.toString());
                            }
                        } else if (this.threadAction.equals("incoming")) {
                            if (server.hasOsxIncomingOutput() && server.hasOsxIncomingInput()) {
//                                server.osxIncomingOutput.println(chatInput);
                                System.out.println("idk what to do here");  // TODO idk? restart loop?

                            } else {
                                System.out.println("ERROR: OSX incoming input/output has not be set. Can't recieve messages from OSX");
                                jsonObject.put("recieveStatus", "unsuccessful");
                                output.println(jsonObject.toString());
                            }
                        }

                    } else if (deviceType == DeviceType.OSX_CLIENT) {
                        // Received message from an OSX Client
                        if (this.threadAction.equals("outgoing")) {

                            String messageStatus = jsonObject.getString("messageStatus");

                            if (messageStatus.equals("successful")) {
                                System.out.println("OSX Client says message was sent successfully");
                            } else if (messageStatus.equals("unsuccessful")) {
                                System.out.println("OSX Client says message was sent unsuccessfully");
                            } else {
                                System.out.println("Unknown message status");
                                jsonObject.put("messageStatus", "unsuccessful");
                            }

                            // Tell mobile clients that it was successful
                            server.notifyMobileOutgoingThreads(jsonObject);
                        } else if (this.threadAction.equals("incoming")) {

                            String incomingAction = jsonObject.getString("incoming_action");

                            if (incomingAction.equals("online")) {
                                // Log that OSX Incoming is still online
                                System.out.println("OSX incoming is still online - " + new Date());

                            } else if (incomingAction.equals("notify_mobile")) {

                                // Notify Mobile client about incoming messages
                                System.out.println("Received incoming message. Tell Server to notify mobile clients");
                                server.notifyMobileIncomingThreads(jsonObject);

                            } else {
                                System.out.println("No incoming_action set");
                            }
                        }
                    }
                }
            }

            System.out.println("Lost connection");

            if (deviceType == DeviceType.OSX_CLIENT) {
                System.out.println("Lost connection of OSX Client");
                server.setOsxOutgoingOutput(null);
                server.setOsxOutgoingInput(null);
            } else if (deviceType == DeviceType.MOBILE){
                System.out.println("Lost connection of Mobile Client");
                server.removeMobileThread(this);
            } else {
                System.out.println("Lost connection");
            }

            threadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();

            if (deviceType == DeviceType.MOBILE) {
                System.out.println("Lost connection of Mobile Client due to socket error");
                killMobileThread();
            } else if (deviceType == DeviceType.OSX_CLIENT) {
                System.out.print("Lost connection of OSX Client due to socket error. ");
                killOSXThread();
            }

        } catch (JSONException e) {
            System.out.print("Failed to create json. ");
            e.printStackTrace();

            if (deviceType == DeviceType.MOBILE) {
                System.out.println(" Lost connection of Mobile Client due to JSON error");
                killMobileThread();
            } else if (deviceType == DeviceType.OSX_CLIENT) {
                System.out.print(" Lost connection of OSX Client due to JSON error. ");
                killOSXThread();
            }

            // todo improve
            try {
                threadSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void setDeviceType(String deviceTypeString, String action) {
        this.deviceType = DeviceType.valueOf(deviceTypeString);
        String notif;

        switch (this.deviceType) {
            case OSX_CLIENT:
                if (action.equals("incoming")) {
                    notif = "Registered as OSX Client - incoming";
                    server.setOSXIncomingInput(input);
                    server.setOsxIncomingOutput(output);
                } else {
                    notif = "Registered as OSX Client - outgoing";
                    server.setOsxOutgoingOutput(output);
                    server.setOsxOutgoingInput(input);
                }
                break;
            case MOBILE:
                if (action.equals("outgoing")) {
                    notif = "Registered as Mobile Client - outgoing";
                    server.addMobileOutgoingThread(this);
                } else {
                    notif = "Registered as Mobile Client - incoming";
                    server.addMobileIncomingThread(this);
                }
                break;
            default:
                notif = "Unknown device type";
        }

        this.threadAction = action;

        output.println(notif);  // Tell client the device type has been registered
    }

    private enum DeviceType {
        UNDEFINED ("UNDEFINED"),
        OSX_CLIENT ("OSX_CLIENT"),
        MOBILE ("MOBILE");

        private String deviceString;

        DeviceType(String deviceNum) {
            this.deviceString = deviceNum;
        }

        public String getVal() {
            return this.deviceString;
        }
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    private void killMobileThread() {
        server.removeMobileThread(this);
    }

    private void killOSXThread() {
        if (this.threadAction == "outgoing") {
            System.out.println("Removing outgoing I/O refs");
            server.osxOutgoingInput = null;
            server.osxOutgoingOutput = null;
        } else if (this.threadAction == "incoming") {
            System.out.println("Removing incoming I/O refs");
            server.osxIncomingInput = null;
            server.osxIncomingOutput = null;
        }
    }
}
