package com.ericchee.bboyairwreck.piemessage;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class MessageActivity extends AppCompatActivity implements ReceiveMessagesService.Callbacks {
    public static final String TAG = MessageActivity.class.getSimpleName();
    TextView tvTarget;
    EditText etTarget;
    EditText etMessage;
    ImageButton ibCheckmark;
    ClientSocketTask asyncTask;
    ArrayList<Socket> listOfSockets;
    ListView lvMessages;
    ArrayList<Message> arrayOfMessages;
    MessagesAdapter adapter;
    Button btnSend;
    ReceiveMessagesService receiveMessagesService;
    private boolean boundReceiveService = false;
    private long chatROWID = -1;
    String targetString;
    boolean isNewChat = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setStatusBarColor();    // set status bar color

        Log.i(TAG, "Creating activity");

        if (getIntent() != null) {
            chatROWID = getIntent().getLongExtra(Constants.chatROWID, -1);
            if (chatROWID != -1) {
                isNewChat = false;
            }
        }

        tvTarget = (TextView) findViewById(R.id.tvTarget);
        etTarget = (EditText) findViewById(R.id.etTarget);
        ibCheckmark = (ImageButton) findViewById(R.id.ibCheckmark);
        listOfSockets = new ArrayList<>();

        setTvTargetListener();
        setIbCheckmarkListener();
        initMessagesListAdapter();


        btnSend = (Button) findViewById(R.id.btnSend);
        etMessage = (EditText) findViewById(R.id.etMessage);

        btnSend.setEnabled(true);
        btnSend.setBackgroundResource(R.color.purple);

        setSendOnClickListener();
        setBackButtonListener();

        // bind ReceiveMessages
        bindToReceiveService();
    }

    private void setSendOnClickListener() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "pressed");

                updateTargetValue();
                if (hasSetTargetNumber()) {
                    // if has number, send msg
                    String targetPhoneNumber = tvTarget.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    etMessage.setText(message);

                    if (message.length() > 0) {
                        addSentMessageToListView();
                        new SendMessageTask(MessageActivity.this, targetPhoneNumber, message).execute();
                        showBackButton();
                    } else {
                        Log.i(TAG, "Message text has no length");
                    }
                } else {
                    // Hasn't set target value
                    Log.d(TAG, "Has not set target value");
                }
            }
        });
    }

    public void addSentMessageToListView() {
        Message messageInProgress = new Message(etMessage.getText().toString(), MessageType.SENT, MessageStatus.IN_PROGRESS, "Me");
        messageInProgress.setDate(-1);  // TODO need to set date
        arrayOfMessages.add(messageInProgress);
        etMessage.setText(null);
        adapter.notifyDataSetChanged();
        lvMessages.smoothScrollToPosition(adapter.getCount() - 1);
    }

    public void addReceivedMessageToListView(String messageText, String handleID, long date) {
        Message receivedMessage = new Message(messageText, MessageType.RECEIVED, MessageStatus.SUCCESSFUL, handleID);
        receivedMessage.setDate(date);
        arrayOfMessages.add(receivedMessage);
        adapter.notifyDataSetChanged();
        lvMessages.smoothScrollToPosition(adapter.getCount() - 1);
    }

    private void enableSendButton() {
        btnSend.setEnabled(true);
    }

    public void messageStatusReceived(JSONObject messageStatusJSON) throws JSONException {
        boolean messageSuccessful = messageStatusJSON.getString("messageStatus").equals("successful");

        // Find most recent sent message in progress & same text
        Message sentMessage = null;
        for (int i = arrayOfMessages.size() - 1; i >= 0; i--) {
            Message curMessage = arrayOfMessages.get(i);
            if (curMessage.messageStatus == MessageStatus.IN_PROGRESS && curMessage.text.equals(messageStatusJSON.getString("message"))) {
                sentMessage = curMessage;
                break;
            }
        }

        if (sentMessage != null) {
            // Found most recent message. Sent its status, date, & cROWID
            sentMessage.messageStatus = messageSuccessful ? MessageStatus.SUCCESSFUL : MessageStatus.UNSUCCESSFUL;

            long date = messageStatusJSON.getLong("date");
            sentMessage.setDate(date);
            adapter.notifyDataSetChanged();

            if (chatROWID == -1) {
                chatROWID = messageStatusJSON.getLong("cROWID");
                Log.i(TAG, "Setting chatROWID = " + chatROWID);
            }
        } else {
            Log.e(TAG, "Could not find sent message that matched sent response JSON");
        }
    }

    private void setTvTargetListener() {
        if (!isNewChat) {
            // If previous chat, Set the Handle ID
            targetString = getIntent().getStringExtra(Constants.chatHandlesString);
            tvTarget.setText(targetString);
            etTarget.setText(targetString);

            // Show back button because is previous chat
            showBackButton();
        } else {
            // If new chat, listen for if tap on Target textview
            tvTarget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    tvTarget.setVisibility(View.INVISIBLE);
                    etTarget.setVisibility(View.VISIBLE);
                    ibCheckmark.setVisibility(View.VISIBLE);
                    etTarget.requestFocus();
                }
            });
        }
    }

    private boolean hasSetTargetNumber(){
        return ! tvTarget.getText().toString().equals(getString(R.string.insert_number));
    }

    private void setIbCheckmarkListener() {
        if (isNewChat) {
            ibCheckmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateTargetValue();
                }
            });
        }
    }

    private void updateTargetValue() {
        String valueOfTarget = etTarget.getText().toString().trim();
        if (valueOfTarget.length() > 0) {
            tvTarget.setText(valueOfTarget);
        } else {
            tvTarget.setText(getString(R.string.insert_number));
            Log.i(TAG, "Target value is invalid");
            Toast.makeText(
                    getApplicationContext(),
                    "Please add valid target",
                    Toast.LENGTH_SHORT)
                    .show();
        }
        etTarget.setText(valueOfTarget);    // set etTarget to trimmed string
        tvTarget.setVisibility(View.VISIBLE);
        etTarget.setVisibility(View.INVISIBLE);
        ibCheckmark.setVisibility(View.INVISIBLE);
    }

    public void addSocket(Socket socket) {
        Log.i(TAG, "Adding socket to list of sockets");
        listOfSockets.add(socket);
    }

    // Construct data and set in custom adapter
    private void initMessagesListAdapter() {
        if (!isNewChat) {
            // grab all messages from chat in sqlite db
            PieSQLiteOpenHelper dbHelper = PieMessageApplication.getInstance().getDbHelper();
            arrayOfMessages = dbHelper.getAllMessagesOfChat(chatROWID);
        } else {
            // If new chat, initiate new array of messages
            arrayOfMessages = new ArrayList<>();
        }
        adapter = new MessagesAdapter(this, arrayOfMessages);

        // Attach adapter to listView
        lvMessages = (ListView) findViewById(R.id.lvMessages);
        lvMessages.setAdapter(adapter);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
    }

    // TODO Delete this method once MessageResponseTask finishes
    public void disconnectSockets() {
        for (int i = 0; i < listOfSockets.size(); i++) {
            try {
                Log.i(TAG, "Closing socket");
                listOfSockets.get(i).close();
            } catch (IOException e) {
                Log.e(TAG, "Closing buffered reader was unsuccessful");
                e.printStackTrace();
            }
        }

        asyncTask.keepAlive = false;

        asyncTask.cancel(true);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroying activity");
        unbindToReceiveService();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        bindToReceiveService();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Pausing activity");
        unbindToReceiveService();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putString("etTarget", etTarget.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        String etTargetText = savedInstanceState.getString("etTarget");
        etTarget.setText(etTargetText);
        super.onRestoreInstanceState(savedInstanceState);
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
            receiveMessagesService.registerActivity(MessageActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service has disconnected");
            boundReceiveService = false;
        }
    };

    @Override
    public void onReceivedMessages(final String receiveMessagesJsonString) {
        Log.i(TAG, "JSON received message - " + receiveMessagesJsonString);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSONObject receivedMessagesJSON = null;
                JSONArray foundMessagesJSON = null;
                try {
                    receivedMessagesJSON = new JSONObject(receiveMessagesJsonString);

                    foundMessagesJSON = receivedMessagesJSON.getJSONArray("incomingMessages");

                    for (int i = 0; i < foundMessagesJSON.length(); i++) {
                        JSONObject foundMessageJSON = (JSONObject) foundMessagesJSON.get(i);

                        printFoundMessages(foundMessageJSON);   // todo testing

                        String messageText = foundMessageJSON.getString("text");
                        String handleID = foundMessageJSON.getString("h.id");
                        long curChatROWID = foundMessageJSON.getLong("c.ROWID");
                        int is_from_me = foundMessageJSON.getInt("is_from_me");
                        long date = foundMessageJSON.getLong("date");
                        Log.i(TAG, messageText);

                        // If message belongs to this chat & is not from me
                        if (curChatROWID == MessageActivity.this.chatROWID && is_from_me == 0) {   // TODO need better way to detect if chat
                            addReceivedMessageToListView(messageText, handleID, date);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setBackButtonListener() {
        ImageButton ibMABackArrow = (ImageButton) findViewById(R.id.ibMABackArrow);
        ibMABackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatsIntent = new Intent(MessageActivity.this, ChatsActivity.class);
                startActivity(chatsIntent);
            }
        });
    }

    private void showBackButton() {
        TextView tvTo = (TextView) findViewById(R.id.tvTo);
        ImageButton ibMABackArrow = (ImageButton) findViewById(R.id.ibMABackArrow);

        // Hide To: textview
        tvTo.setVisibility(View.GONE);

        // Show back arrow
        ibMABackArrow.setVisibility(View.VISIBLE);
    }

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

    private void printFoundMessages(JSONObject foundMessageJSON) throws JSONException {
        for (int i = 0; i < foundMessageJSON.names().length(); i++) {
            String key = foundMessageJSON.names().getString(i);
            String value = foundMessageJSON.get(key).toString();
            Log.i(TAG, key + " - " + value);
        }
    }
}
