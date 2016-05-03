import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.Date;

/**
 * Created by eric on 11/20/15.
 */
public class OutgoingSQLiteChecker {
//    public static void main(String[] args) {
//        new SQLiteManager(new String[3]);
//    }

    public OutgoingSQLiteChecker(JSONObject responseJSON, String message, long postTime) {
        Connection connection = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + PieOSXClient.getHomeDirectory() + "/Library/Messages/chat.db");
            if (connection != null) {
                System.out.println("Connected to chat.db");

                getSentMeMessage(connection, message, responseJSON, postTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getSentMeMessage(Connection connection, String message, JSONObject responseJSON, long postTime) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet resultSet = null;
            boolean found = false;
            int loopCount = 0;
            while (!found) {
                resultSet = queryLastMessage(connection, message, postTime);

                int rowCount = 0;
                if (resultSet.next()) {
                    rowCount++;
                }

                if (rowCount > 0) {
                    System.out.println("Found sent message");
                    resultSet = queryLastMessage(connection, message, postTime);
                    found = true;
                    break;
                }

                System.out.println("Didn't find sent message yet");

                if (loopCount < 20) {
                    Thread.sleep(500);
                    loopCount++;
                } else {
                    setJSONUnsuccessful(responseJSON, message);

                    return;
                }
            }

            while (resultSet.next()) {
                boolean isFromMe = resultSet.getBoolean("is_from_me");
                boolean isSent = resultSet.getBoolean("is_sent");
                String text = resultSet.getString("text");
                long dateLong = resultSet.getLong("date");
                String handleID = resultSet.getString("id");
                String guid = resultSet.getString("guid");
                String cROWID = resultSet.getString("cROWID");

                Date date = new Date((dateLong + 978307200) * 1000);    // date += Jan 1st, 2001

                System.out.println("text - " + text);
                System.out.println("me - " + isFromMe);
                System.out.println("date - " + date.toString());
                System.out.println("delivered - " + isSent);
                System.out.println("handleID - " + handleID);
                System.out.println("cROWID - " + cROWID);
                System.out.println();

                responseJSON.put("message", text);
                responseJSON.put("messageID", guid);
                responseJSON.put("cROWID", cROWID);
                responseJSON.put("date", dateLong);

                if (isSent) {
                    responseJSON.put("messageStatus", "successful");
                } else {
                    responseJSON.put("messageStatus", "unsuccessful");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error has occured when searching for sent message. Sending back unsuccessful messageDetails back");
            setJSONUnsuccessful(responseJSON, message);
        }

    }

    private ResultSet queryLastMessage(Connection connection, String message, long postTime) throws SQLException {
        PreparedStatement prepStmt = connection.prepareStatement(lastMeQuery());
        prepStmt.setString(1, message);
        prepStmt.setLong(2, postTime);

        return prepStmt.executeQuery();
    }

    private void setJSONUnsuccessful(JSONObject responseJSON, String message) {
        try {
            responseJSON.put("messageStatus", "unsuccessful");
            responseJSON.put("message", message);
            responseJSON.put("messageID", "no_GUID");
            responseJSON.put("cROWID", -1);
            responseJSON.put("date", Constants.getNowEpochSeconds());
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("Error returning unsuccessful responseJSON");
        }
    }

    public String lastMeQuery() {
        return "SELECT m.is_from_me, m.is_sent, m.date, m.text, h.id, m.guid, c.ROWID AS 'cROWID' FROM message m\n" +
                "JOIN chat_message_join cmj ON m.ROWID = cmj.message_id\n" +
                "JOIN chat c ON cmj.chat_id = c.ROWID\n" +
                "JOIN chat_handle_join chj ON c.ROWID = chj.chat_id\n" +
                "JOIN handle h ON chj.handle_id = h.ROWID\n" +
                "WHERE m.is_from_me = 1\n" +
                "AND m.text = ? \n" +
                "AND m.date >= ?\n" +
                "ORDER BY date DESC\n" +
                "LIMIT 1;";
    }
}
