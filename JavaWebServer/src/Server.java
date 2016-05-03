import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by eric on 11/7/15.
 */
public class Server {
    public static Server instance;
    public PrintWriter osxOutgoingOutput;
    public BufferedReader osxOutgoingInput;
    public PrintWriter osxIncomingOutput;
    public BufferedReader osxIncomingInput;
    HashSet<ClientThread> setOfMobileOutgoingThreads;
    HashSet<ClientThread> setOfMobileIncomingThreads;
    HashSet<ClientThread> setOfOSXOutgoingThreads;
    HashSet<ClientThread> setOfOSXIncomingThreads;

    public static void main(String[] args) {
         new Server();
    }

    public static Server getInstance() {
        return instance;
    }

    public Server() {
        if (instance == null) {
            instance = this;

            try {
                ServerSocket sSocket = new ServerSocket(5000);  // using port 5000 for our server socket
                System.out.println("Server started at: " + new Date());
                setOfMobileOutgoingThreads = new HashSet<ClientThread>();
                setOfMobileIncomingThreads = new HashSet<ClientThread>();
                setOfOSXOutgoingThreads =  new HashSet<ClientThread>();
                setOfOSXIncomingThreads =  new HashSet<ClientThread>();

                // loop that runs server functions
                while(true) {
                    // Wait for a client to connect
                    Socket socket = sSocket.accept();

                    // Create a new custom thread to handle connection
                    ClientThread cT = new ClientThread(socket);

                    // Create thread and pass ID
                    Thread thread = new Thread(cT);
                    cT.setThread(thread);

                    // Start the thread!
                    thread.start();

                }

            } catch (IOException exception) {
                System.out.println("Error: " + exception);
            }

        } else {
            System.out.println("ERROR: You tried to create more than 1 Server");
        }
    }

    public void setOsxOutgoingOutput(PrintWriter osxOutgoingOutput) {
        System.out.println("Server - Setting osxOutgoingOutput");
        this.osxOutgoingOutput = osxOutgoingOutput;
    }

    public void setOsxOutgoingInput(BufferedReader osxOutgoingInput) {
        System.out.println("Server - Setting osxOutgoingInput");
        this.osxOutgoingInput = osxOutgoingInput;
    }

    public void setOSXIncomingInput(BufferedReader osxIncomingInput) {
        System.out.println("Server - Setting osxIncomingInput");
        this.osxIncomingInput = osxIncomingInput;
    }

    public void setOsxIncomingOutput(PrintWriter osxIncomingOutput) {
        System.out.println("Server - Setting osxIncomingOutput");
        this.osxIncomingOutput = osxIncomingOutput;
    }

    public boolean hasOsxOutgoingOutput() {
        return this.osxOutgoingOutput != null;
    }

    public boolean hasOsxOutgoingInput() {
        return this.osxOutgoingInput != null;
    }

    public boolean hasOsxIncomingOutput() {
        return this.osxIncomingOutput != null;
    }

    public boolean hasOsxIncomingInput() {
        return this.osxIncomingInput != null;
    }

    public void addOSXOutgoingThread(ClientThread clientThread) {
        this.setOfOSXOutgoingThreads.add(clientThread);
    }

    public void addOSXIncomingThread(ClientThread clientThread) {
        this.setOfOSXIncomingThreads.add(clientThread);
    }

    public void removeOSXOutgoingThread(ClientThread clientThread) {
        this.setOfOSXOutgoingThreads.remove(clientThread);
    }

    public void removeOSXIncomingThread(ClientThread clientThread) {
        this.setOfOSXIncomingThreads.remove(clientThread);
    }

    public boolean notifyOSXOutgoingThread(JSONObject jsonObject) {
        ArrayList<ClientThread> removeList = new ArrayList<>();
        boolean didSend = false;

        for (ClientThread osxThread : setOfOSXOutgoingThreads) {
            if (osxThread.thread.isAlive()) {
                osxThread.output.println(jsonObject.toString());
                didSend = true;
            } else {
                removeList.add(osxThread);
            }
        }

        for (ClientThread clientThread : removeList) {
            this.setOfOSXOutgoingThreads.remove(clientThread);
        }

        return didSend;
    }

    public void addMobileOutgoingThread(ClientThread clientThread) {
        this.setOfMobileOutgoingThreads.add(clientThread);
    }

    public void addMobileIncomingThread(ClientThread clientThread) {
        this.setOfMobileIncomingThreads.add(clientThread);
    }

    public void removeMobileThread(ClientThread clientThread) {
        this.setOfMobileOutgoingThreads.remove(clientThread);
    }

    public void notifyMobileOutgoingThreads(JSONObject jsonObject) {
        ArrayList<ClientThread> removeList = new ArrayList<>();

        for (ClientThread mobileThread : setOfMobileOutgoingThreads) {
            if (mobileThread.thread.isAlive()) {
                System.out.println("Mobile outgoing thread is alive, sending msg to client");
                mobileThread.output.println(jsonObject.toString());
            } else {
                System.out.println("Mobile outgoing thread is dead");
                removeList.add(mobileThread);
            }
        }

        for (ClientThread clientThread : removeList) {
            this.setOfMobileOutgoingThreads.remove(clientThread);
        }

        System.out.println("Finished return notifying of outgoing message to Mobile");
    }

    public void notifyMobileIncomingThreads(JSONObject jsonObject) {
        ArrayList<ClientThread> removeList = new ArrayList<ClientThread>();

        for (ClientThread mobileThread : setOfMobileIncomingThreads) {
            if (mobileThread.thread.isAlive()) {
                System.out.println("Mobile incoming thread is alive, sending msg to client");
                mobileThread.output.println(jsonObject.toString());
            } else {
                System.out.println("Mobile incoming thread is dead");
                removeList.add(mobileThread);
            }
        }

        for (ClientThread clientThread : removeList) {
            this.setOfMobileOutgoingThreads.remove(clientThread);
        }

        System.out.println("Finished notifying of incoming messages");
    }

}
