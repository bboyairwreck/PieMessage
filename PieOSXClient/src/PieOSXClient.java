import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by eric on 11/17/15.
 */
public class PieOSXClient {
    public static void main(String[] args) {
        new PieOSXClient();
    }

    public PieOSXClient() {
        doesMessageScriptExist();   // Ensure we have a ~/messages.applescript;

        // Start outgoing thread
        OSXOutgoingMessageThread outgoingThread = new OSXOutgoingMessageThread();
        Thread outThread = new Thread(outgoingThread);
        outgoingThread.setThread(outThread);
        outThread.start();

        // Start incoming thread
        OSXIncomingMessageThread incomingThread = new OSXIncomingMessageThread();
        Thread inThread = new Thread(incomingThread);
        incomingThread.setThread(inThread);
        inThread.start();
    }

    public static String getDateString() {
        Date d = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);

        int month = cal.get(Calendar.MONTH) + 1;
        int dateNum = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);

        String dateString = year + "-" + month + "-" + dateNum;

        return dateString;
    }

    public static String getHomeDirectory() {
        return System.getProperty("user.home");
    }

    private boolean doesMessageScriptExist() {
        String filePath = getHomeDirectory() + "/messages.applescript";
        File messagesFile = new File(filePath);

        if (messagesFile.exists()) {
            System.out.println("File \"/messages.applescript\" exists");
        } else {
            System.out.println("WARNING - \"/messages.applescript\" does NOT exist");
        }

        return messagesFile.exists();
    }
}
