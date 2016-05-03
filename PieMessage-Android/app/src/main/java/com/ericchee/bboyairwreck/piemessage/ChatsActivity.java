package com.ericchee.bboyairwreck.piemessage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.TreeMap;


public class ChatsActivity extends AppCompatActivity implements ReceiveMessagesService.Callbacks {
    public static final String TAG = ChatsActivity.class.getSimpleName();
    private ListView lvChats;
    private ChatsAdapter chatsAdapter;
    private ArrayList<Chat> chatsList;
    ReceiveMessagesService receiveMessagesService;
    private boolean boundReceiveService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarChat);
//        setSupportActionBar(toolbar);

        Log.i(TAG, "creating ChatsActivity");

        reloadChatListAndAdapter();

        // Navigate to MessageActivity when selecting chat item
        lvChats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Chat chat = (Chat) parent.getItemAtPosition(position);

                Intent messageIntent = new Intent(ChatsActivity.this, MessageActivity.class);
                messageIntent.putExtra(Constants.chatROWID, chat.cROWID);
                messageIntent.putExtra(Constants.chatHandlesString, chat.getHandlesString());
                startActivity(messageIntent);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fabNewMessage);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent messageIntent = new Intent(ChatsActivity.this, MessageActivity.class);
                startActivity(messageIntent);
            }
        });

        bindToReceiveService();
    }

    private void reloadChatListAndAdapter() {
        PieSQLiteOpenHelper dbHelper = PieMessageApplication.getInstance().getDbHelper();
        dbHelper.getAllChats();

        TreeMap<Long, Chat> chatsMap = PieMessageApplication.getInstance().getChatsMap();

        chatsList = new ArrayList<>();

        for (Long cROWID : chatsMap.keySet()) {
            Log.i(TAG, "parsing chatsMap");
            Chat chat = chatsMap.get(cROWID);
            chatsList.add(chat);
//            // todo delete
//            String handlesString = "";
//            int i = 0;
//            for (String handle : chat.handles) {
//                handlesString += handle;
//                if (i < chat.handles.size() -1) {
//                    handlesString += ", ";
//                }
//                i++;
//            }
//
//            Log.i(TAG, "Chat - " + handlesString);
        }

        // Set adapter for chats list view
        lvChats = (ListView) findViewById(R.id.lvChats);
        chatsAdapter = new ChatsAdapter(this, chatsList);
        lvChats.setAdapter(chatsAdapter);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected called");

            // Ever service has a binder. In service class we created LocalBinder to get service instance
            // - set our service field equal to the service instance
            ReceiveMessagesService.LocalBinder binder = (ReceiveMessagesService.LocalBinder) service;
            receiveMessagesService = binder.getServiceInstance();

            // Register this activity to get callbacks
            receiveMessagesService.registerActivity(ChatsActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service has disconnected");
            boundReceiveService = false;
        }
    };

    private void bindToReceiveService() {
        Intent receiveMessagesServiceIntent = new Intent(this, ReceiveMessagesService.class);
        boundReceiveService = getApplicationContext().bindService(receiveMessagesServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindToReceiveService() {
        if (boundReceiveService) {
            if (receiveMessagesService != null) {
                receiveMessagesService.unregisterActivity();
            }
            getApplicationContext().unbindService(mConnection);
            boundReceiveService = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Resuming ChatsActivity");
        bindToReceiveService();
        reloadChatListAndAdapter();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroying activity");
        unbindToReceiveService();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Pausing activity");
        unbindToReceiveService();
        super.onPause();
    }

    @Override
    public void onReceivedMessages(String receiveMessagesJsonString) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reloadChatListAndAdapter();
            }
        });
    }
}
