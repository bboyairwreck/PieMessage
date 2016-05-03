package com.ericchee.bboyairwreck.piemessage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Created by eric on 12/10/15.
 */
public class PieSQLiteOpenHelper extends SQLiteOpenHelper {
    public static final String TAG = PieSQLiteOpenHelper.class.getSimpleName();
    public static final String DATABASE_NAME = "PieDatabase";
    public static final int DATABASE_VERSION = 2;
    SQLiteDatabase mDB;
    Context mContext;

    // Table message
    public static final String tblMESSAGE = "message";
    public static final String COL_ROWID = "ROWID";
    public static final String COL_TEXT = "text";
    public static final String COL_mDATE=  "date";
    public static final String COL_mIS_SENT = "is_sent";
    public static final String COL_mIS_FROM_ME = "is_from_me";

    // Table chat
    public static final String tblCHAT = "chat";
        // COL_ROWID

    // Table chat_message_join
    public static final String tblCHAT_MESSAGE_JOIN = "chat_message_join";
    public static final String COL_CHAT_ID = "chat_id";
    public static final String COL_MESSAGE_ID = "message_id";

    // Table handle
    public static final String tblHANDLE = "handle";
        // COL_ROWID
    public static final String COL_ID = "id";

    // Table chat_handle_join
    public static final String tblCHAT_HANDLE_JOIN = "chat_handle_join";
    public static final String COL_HANDLE_ID = "handle_id";
        // COL_CHAT_ID

    // Queries
    public static final String qDB_CREATE_CHAT =
            "CREATE TABLE chat (" +
            "    ROWID integer  NOT NULL   PRIMARY KEY" +
            "); ";

    public static final String qDB_CREATE_CHAT_HANDLE_JOIN =
            "CREATE TABLE chat_handle_join (" +
            "    chat_id integer  NOT NULL," +
            "    handle_id integer  NOT NULL," +
            "    CONSTRAINT chat_handle_join_pk PRIMARY KEY (chat_id,handle_id)" +
            "); ";

    public static final String qDB_CREATE_CHAT_MESSAGE_JOIN =
            "CREATE TABLE chat_message_join (" +
            "    chat_id integer  NOT NULL," +
            "    message_id integer  NOT NULL," +
            "    CONSTRAINT chat_message_pk PRIMARY KEY (chat_id,message_id)" +
            "); ";

    public static final String qDB_CREATE_HANDLE =
            "CREATE TABLE handle (" +
            "    ROWID integer  NOT NULL   PRIMARY KEY," +
            "    id text  NOT NULL" +
            "); ";

    public static final String qDB_CREATE_MESSAGE =
            "CREATE TABLE message (" +
            "    ROWID integer  NOT NULL   PRIMARY KEY," +
            "    'text' text  NOT NULL," +
            "    date integer  NOT NULL," +
            "    is_sent integer  NOT NULL," +
            "    is_from_me integer  NOT NULL" +
            "); ";

    public PieSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        mDB = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "creating database");
        db.execSQL(qDB_CREATE_CHAT);
        db.execSQL(qDB_CREATE_MESSAGE);
        db.execSQL(qDB_CREATE_HANDLE);
        db.execSQL(qDB_CREATE_CHAT_HANDLE_JOIN);
        db.execSQL(qDB_CREATE_CHAT_MESSAGE_JOIN);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + tblMESSAGE);
        onCreate(db);
    }

//    public void insertMessage() {
//        Log.i(TAG, "Inserting message");
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        ContentValues values = new ContentValues();
//        values.put(COL_ROWID, 2);
//        values.put(COL_TEXT, "second message!");
//        values.put(COL_mDATE, 123457);
//        values.put(COL_mIS_SENT, 1);
//        values.put(COL_mIS_FROM_ME, 0);
//
//
//        long rowid = db.insert(tblMESSAGE, null, values);
////        Log.i(TAG, "ROW ID = " + rowid);
//
//        db.close();
//    }

    public void insertMessage(JSONObject foundMessageJSON) throws JSONException {

        // message
        long mROWID = foundMessageJSON.getLong("m.ROWID");
        String text = foundMessageJSON.getString("text");
        int is_sent = foundMessageJSON.getInt("is_sent");
        int is_from_me = foundMessageJSON.getInt("is_from_me");
        long date = foundMessageJSON.getLong("date");
        if (!messageExists(mROWID)) {
            insertMessage(mROWID, text, is_sent, is_from_me, date);
        }

        // chat
        long cROWID = foundMessageJSON.getLong("c.ROWID");
        if (!chatExists(cROWID)) {
            insertChat(cROWID);
        }


        // chat_message_join
        long chat_id = foundMessageJSON.getLong("cmj.chat_id");
        long message_id = foundMessageJSON.getLong("cmj.message_id");
        if (!chatMessageJoinExists(chat_id, message_id)) {
            insertChatMessageJoin(chat_id, message_id);
        }

        // handle
        long hROWID = foundMessageJSON.getLong("h.ROWID");
        String hID = foundMessageJSON.getString("h.id");
        if (!handleExists(hROWID)) {
            insertHandle(hROWID, hID);
        }

        // chat_handle_join
        long chjChatID = foundMessageJSON.getLong("chj.chat_id");
        long chjHandleID = foundMessageJSON.getLong("chj.handle_id");
        if (!chatHandleJoinExists(chjChatID, chjHandleID)) {
            insertChatHandleJoin(chjChatID, chjHandleID);
        }
    }

    private boolean chatExists(long cROWID) {
        return queryExists(
                "SELECT * FROM " + tblCHAT +
                        " WHERE " + COL_ROWID + " = " + cROWID);
    }

    private boolean messageExists(long mROWID) {
        return queryExists(
                "SELECT * FROM " + tblMESSAGE +
                        " WHERE " + COL_ROWID + " = " + mROWID);
    }

    private boolean chatMessageJoinExists(long chat_id, long message_id) {
        return queryExists(
                "SELECT * FROM " + tblCHAT_MESSAGE_JOIN +
                        " WHERE " + COL_CHAT_ID  + " = " + chat_id +
                        " AND " + COL_MESSAGE_ID + " = " + message_id);
    }

    private boolean handleExists(long hROWID) {
        return queryExists(
                "SELECT * FROM " + tblHANDLE +
                        " WHERE " + COL_ROWID + " = " + hROWID);
    }

    private boolean chatHandleJoinExists(long chjChatID, long chjHandleID) {
        return queryExists(
                "SELECT * FROM " + tblCHAT_HANDLE_JOIN +
                        " WHERE " + COL_CHAT_ID + " = " + chjChatID +
                        " AND " + COL_HANDLE_ID + " = " + chjHandleID
        );
    }

    private boolean queryExists(String query) {
        Cursor cursor = ExecSELECTQuery(query);
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }

        cursor.close();
        return true;
    }

    private void insertMessage(long mROWID, String text, int is_sent, int is_from_me, long date) {
        Log.i(TAG, "Inserting \"" + text + "\" into message");

        ContentValues values = new ContentValues();
        values.put(COL_ROWID, mROWID);
        values.put(COL_TEXT, text);
        values.put(COL_mDATE, date);
        values.put(COL_mIS_SENT, is_sent);
        values.put(COL_mIS_FROM_ME, is_from_me);

        dbInsert(tblMESSAGE, null, values);
    }

    private void insertChat(long cROWID) {
        Log.i(TAG, "Inserting \"" + cROWID + "\" into chat");

        ContentValues values = new ContentValues();
        values.put(COL_ROWID, cROWID);

        dbInsert(tblCHAT, null, values);
    }

    private void insertHandle(long hROWID, String hID) {
        Log.i(TAG, "Inserting " + hID + " into handle");

        ContentValues values = new ContentValues();
        values.put(COL_ROWID, hROWID);
        values.put(COL_ID, hID);

        dbInsert(tblHANDLE, null, values);
    }

    private void insertChatMessageJoin(long chat_id, long message_id) {
        Log.i(TAG, String.format("Inserting %d, %d into chat_message_join", chat_id, message_id));

        ContentValues values = new ContentValues();
        values.put(COL_CHAT_ID, chat_id);
        values.put(COL_MESSAGE_ID, message_id);

        dbInsert(tblCHAT_MESSAGE_JOIN, null, values);
    }

    private void insertChatHandleJoin(long chjChadID, long chjHandleID) {
        Log.i(TAG, String.format("Inserting %d, %d into chat_handle_join", chjChadID, chjHandleID));

        ContentValues values = new ContentValues();
        values.put(COL_CHAT_ID, chjChadID);
        values.put(COL_HANDLE_ID, chjHandleID);

        dbInsert(tblCHAT_HANDLE_JOIN, null, values);
    }

    private long dbInsert(String tblName, String nullColumnHack, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        long rowid = db.insert(tblName, null, values);

        db.close();
        return rowid;
    }

    public void getAllMessages() {
        SQLiteDatabase db = getReadableDatabase();
//        db.execSQL("SELECT * FROM message");
        Cursor c = ExecSELECTQuery("SELECT * FROM message");
        if(c != null){
//            if(c.getCount() > 0){
            int i = 0;
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Log.i(TAG, "VALUEEEE " + c.getString(c.getColumnIndex("text")));
                i++;
                c.moveToNext();
            }

            c.close();
        }
        c = null;

    }

    public void getAllChats() {
        Log.i(TAG, "Getting all chats");
        Cursor cursor = ExecSELECTQuery("SELECT c.ROWID as 'cROWID', h.id as 'hid' FROM chat c JOIN chat_handle_join chj ON c.ROWID = chj.chat_id JOIN handle h ON chj.handle_id = h.ROWID;");
        if (cursor != null) {
            TreeMap<Long, Chat> chatsMap = PieMessageApplication.getInstance().getChatsMap();

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                long cROWID = cursor.getLong(cursor.getColumnIndex("cROWID"));
                String hID = cursor.getString(cursor.getColumnIndex("hid"));    // i.e. piemessage@gmail.com

                Chat chat = chatsMap.get(cROWID);
                if (chat == null) {
                    // if brand new chat, create new chat
                    HashSet<String> handles = new HashSet<>();
                    handles.add(hID);
                    chat = new Chat(handles, cROWID);
                    chatsMap.put(cROWID, chat);
                } else {
                    // Add handle if chat does not contain handle
                    if (! chat.handles.contains(hID)) {
                        chat.handles.add(hID);
                    }
                }

                // Set date and lastText data
                setLastTextDataOfChat(chat);

                Log.i(TAG, "Got chat with ROWID = " + cROWID + ", " + hID);
                cursor.moveToNext();
            }
            cursor.close();
        }
    }

    public ArrayList<Message> getAllMessagesOfChat(long cROWID) {
        String query = "SELECT * FROM chat c" +
                " JOIN chat_message_join cmj ON c.ROWID = cmj.chat_id" +
                " JOIN message m ON cmj.message_id = m.ROWID" +
                " JOIN chat_handle_join chj ON c.ROWID = chj.chat_id" +
                " JOIN handle h ON chj.handle_id = h.ROWID" +
                " WHERE c.ROWID = " + cROWID;

        ArrayList<Message> messagesList = new ArrayList<>();

        Cursor cursor = ExecSELECTQuery(query);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String text = cursor.getString(cursor.getColumnIndex("text"));
                int is_from_me = cursor.getInt(cursor.getColumnIndex("is_from_me"));
                int is_sent = cursor.getInt(cursor.getColumnIndex("is_sent"));
                long date = cursor.getLong(cursor.getColumnIndex("date"));
                String handleID = cursor.getString(cursor.getColumnIndex("id"));

                MessageStatus status = MessageStatus.SUCCESSFUL;
                if (is_sent == 0) {
                    status = MessageStatus.UNSUCCESSFUL;
                }

                MessageType msgType = MessageType.SENT;
                if (is_from_me == 0) {
                    msgType = MessageType.RECEIVED;
                }

                Message message = new Message(text, msgType, status, handleID, date);
                messagesList.add(message);
                cursor.moveToNext();
            }
            cursor.close();
        }

        return messagesList;
    }

    public Cursor ExecSELECTQuery(String SQL) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try{
            c = db.rawQuery(SQL, null);
        }catch(SQLException e){
            Log.e(TAG, e.getMessage());
        }
        return c;
    }

    /**
     * Messages from a json are inserted into the SQLite database.
     * @param   receiveMessagesJsonString String that has an array or incoming messages
     * @return  boolean true if has incoming messages from others.
     *          return false if only contains sent messages.
     */
    public boolean insertMessagesFromJSONString(String receiveMessagesJsonString) {
        Log.i(TAG, "Parsing received json to add to database");
        boolean hasIncoming = false;
        JSONObject receivedMessagesJSON = null;
        JSONArray foundMessagesJSON = null;
        try {
            receivedMessagesJSON = new JSONObject(receiveMessagesJsonString);

            foundMessagesJSON = receivedMessagesJSON.getJSONArray("incomingMessages");

            for (int i = 0; i < foundMessagesJSON.length(); i++) {
                JSONObject foundMessageJSON = (JSONObject) foundMessagesJSON.get(i);

                addMessageToDB(foundMessageJSON);

                int is_from_me = foundMessageJSON.getInt("is_from_me");
                if (is_from_me == 0) {
                    hasIncoming = true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return hasIncoming;
    }

    private void addMessageToDB(JSONObject foundMessageJSON) throws JSONException {
        Log.d(TAG, "adding message to database");
        PieSQLiteOpenHelper dbHelper = PieMessageApplication.getInstance().getDbHelper();
        dbHelper.insertMessage(foundMessageJSON);
    }

    public void setLastTextDataOfChat(Chat chat) {
        if (chatExists(chat.cROWID)) {
            String query = "SELECT * FROM chat c" +
                    " JOIN chat_message_join cmj ON c.ROWID = cmj.chat_id" +
                    " JOIN message m ON cmj.message_id = m.ROWID" +
                    " WHERE c.ROWID = " + chat.cROWID +
                    " ORDER BY date DESC" +
                    " LIMIT 1;";

            Cursor cursor = ExecSELECTQuery(query);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    chat.lastText = cursor.getString(cursor.getColumnIndex("text"));
                    chat.date = cursor.getLong(cursor.getColumnIndex("date"));

                    cursor.moveToNext();
                }
                cursor.close();
            }
        } else {
            Log.i(TAG, "Chat does not exist. Can't get last text of chat.rowid = " + chat.cROWID);
            chat.lastText = "...";
        }
    }

    public String getLastTextOfChat(long cROWID) {
        Chat tempChat = new Chat();
        tempChat.cROWID = cROWID;
        setLastTextDataOfChat(tempChat);
        return tempChat.lastText;
    }

    public Message getLastMessage() {
        Message lastMessage = new Message("Unknown");

        String query = "SELECT m.text, cmj.chat_id, h.id as 'hID'" +
                " FROM message m JOIN chat_message_join cmj ON m.ROWID = cmj.message_id" +
                " JOIN chat c ON cmj.chat_id = c.ROWID" +
                " JOIN chat_handle_join chj ON c.ROWID = chj.chat_id" +
                " JOIN handle h ON chj.handle_id = h.ROWID" +
                " WHERE m.is_from_me = 0" +
                " ORDER BY date DESC" +
                " LIMIT 1;";

        Cursor cursor = ExecSELECTQuery(query);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                lastMessage.text = cursor.getString(cursor.getColumnIndex("text"));
                lastMessage.setHandleID(cursor.getString(cursor.getColumnIndex("hID")));
                lastMessage.cROWID = cursor.getLong(cursor.getColumnIndex("chat_id"));

                cursor.moveToNext();
            }
            cursor.close();
        }

        return lastMessage;
    }
}
