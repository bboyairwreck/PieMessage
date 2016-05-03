import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

/**
 * Created by echee on 11/20/15.
 */
public class IncomingMessagesListener {
//    public static void main(String[] args) {
////        System.out.println(getNowEpochSeconds());
//
//        Connection connection = null;
//        try {
//            Class.forName("org.sqlite.JDBC");
//
//            connection = DriverManager.getConnection("jdbc:sqlite:" + PieOSXClient.getHomeDirectory() + "/Library/Messages/chat.db");
//            if (connection != null) {
//                JSONArray foundMessagesJSON = new JSONArray();
//                new IncomingMessagesListener(connection, getNowEpochSeconds(), foundMessagesJSON);
//
//                for (int i = 0; i < foundMessagesJSON.length(); i++) {
//                    JSONObject foundMessageJSON = (JSONObject) foundMessagesJSON.get(i);
//                    System.out.println(foundMessageJSON.getString("text"));
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    public IncomingMessagesListener(Connection connection, long lastFetchTime, JSONArray foundMessagesJSON, OSXIncomingMessageThread.ResetStatus resetStatus) {

        long listenerStartTime = System.currentTimeMillis();

        boolean foundMessages = false;

        int loopStep = 0;

        while (! foundMessages) {
            Statement stmt = null;
            try {
                stmt = connection.createStatement();
                ResultSet resultSet = stmt.executeQuery(messagesSinceQuery(lastFetchTime));
                while (resultSet.next()) {
                    // get Message table values
                    long mROWID = resultSet.getLong("m.ROWID");
                    int mIsFromMe = resultSet.getInt("is_from_me");
                    int mIsSent = resultSet.getInt("is_sent");
                    long dateLong = resultSet.getLong("date");
                    String text = resultSet.getString("text");


                    // get Chat_Message_Join values
                    long cmjChatID = resultSet.getLong("cmj.chat_id");
                    long cmjMessageID = resultSet.getLong("cmj.message_id");

                    // get Chat values
                    long cROWID = resultSet.getLong("c.ROWID");

                    // get Chat_Handle_Join values
                    long chjChatID = resultSet.getLong("chj.chat_id");
                    long chjHandleID = resultSet.getLong("chj.handle_id");

                    // get Handle values
                    long hROWID = resultSet.getLong("h.ROWID");
                    String handleID = resultSet.getString("h.id");



                    Date date = new Date((dateLong + Constants.get2001Seconds()) * 1000);    // date += Jan 1st, 2001

                    JSONObject foundMessageJSON = new JSONObject();
                    foundMessageJSON.put("m.ROWID", mROWID);
                    foundMessageJSON.put("is_from_me", mIsFromMe);
                    foundMessageJSON.put("is_sent", mIsSent);
                    foundMessageJSON.put("date", dateLong);
                    foundMessageJSON.put("text", text);
                    foundMessageJSON.put("cmj.chat_id", cmjChatID);
                    foundMessageJSON.put("cmj.message_id", cmjMessageID);
                    foundMessageJSON.put("c.ROWID", cROWID);
                    foundMessageJSON.put("chj.chat_id", chjChatID);
                    foundMessageJSON.put("chj.handle_id", chjHandleID);
                    foundMessageJSON.put("h.ROWID", hROWID);
                    foundMessageJSON.put("h.id", handleID);


//                    printJSONObjectKeyValues(foundMessageJSON);

                    foundMessagesJSON.put(foundMessageJSON);

                    foundMessages = true;
                }

                if (foundMessages) {
                    System.out.println("Found messages, breaking");
                    break;
                } else {
//                    System.out.println("Did not Find messages. Sleeping");
                    System.out.print(".");

                    if ((System.currentTimeMillis() - listenerStartTime) >= Constants.IncomingThreadResetDuration) {
                        System.out.println();
                        System.out.println("Been 1 hour since connection. Resetting IncomingMessagesListener");
                        resetStatus.shouldReset = true;
                        break;
                    }

                    if (loopStep > 40) {
                        System.out.println();
                        loopStep = 0;
                    }
                    loopStep++;
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Ending IncomingMessages");
    }

    public static String messagesSinceQuery(long lastFetchTime) {
        return "SELECT m.ROWID AS 'm.ROWID', m.is_from_me, m.is_sent, m.date, m.text, m.guid AS 'm.guid',\n" +
                "cmj.chat_id AS 'cmj.chat_id', cmj.message_id AS 'cmj.message_id',\n" +
                "c.ROWID AS 'c.ROWID',\n" +
                "chj.chat_id AS 'chj.chat_id', chj.handle_id AS 'chj.handle_id',\n" +
                "h.ROWID AS 'h.ROWID', h.id AS 'h.id'\n" +
                "FROM message m\n" +
                "JOIN chat_message_join cmj ON m.ROWID = cmj.message_id\n" +
                "JOIN chat c ON cmj.chat_id = c.ROWID\n" +
                "JOIN chat_handle_join chj ON c.ROWID = chj.chat_id\n" +
                "JOIN handle h ON chj.handle_id = h.ROWID\n" +
                "AND m.date > " + lastFetchTime + "\n" +
                "ORDER BY date DESC\n" +
                "LIMIT 5;";
    }

    private void printJSONObjectKeyValues(JSONObject foundMessageJSON) throws JSONException {
        System.out.println();
        for (int i = 0; i < foundMessageJSON.names().length(); i++) {
            String key = foundMessageJSON.names().getString(i);
            String value = foundMessageJSON.get(key).toString();
            System.out.println(key + " - " + value);
        }
        System.out.println();
    }
}

