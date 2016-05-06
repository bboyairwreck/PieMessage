package com.ericchee.bboyairwreck.piemessage;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.TreeMap;

/**
 * Created by eric on 11/27/15.
 */
public class PieMessageApplication extends Application {
    public final static String TAG = PieMessageApplication.class.getSimpleName();
    private static PieMessageApplication instance;
    private PieSQLiteOpenHelper dbHelper;
    private TreeMap<Long, Chat> chatsMap;  //  chat.ROWID, Chat

    public PieMessageApplication() {
        if (instance == null) {
            instance = this;
        } else {
            Log.e(TAG, "There is an error. You tried to create more than 1 PieMessageApp");
        }
    }

    public static PieMessageApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "PieMessageApp is loaded and running");

//        getApplicationContext().deleteDatabase(PieSQLiteOpenHelper.DATABASE_NAME);

        dbHelper = new PieSQLiteOpenHelper(getApplicationContext());
        chatsMap = new TreeMap<>();
        dbHelper.getAllChats();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.contains(getString(R.string.pref_socket_address_key))) {
            startReceieveMessagesService();
        }
    }

    public void startReceieveMessagesService() {
        Intent receiveService = new Intent(this, ReceiveMessagesService.class);
        startService(receiveService);
    }

    public PieSQLiteOpenHelper getDbHelper() {
        return dbHelper;
    }

    public TreeMap<Long, Chat> getChatsMap() {
        return chatsMap;
    }
}
