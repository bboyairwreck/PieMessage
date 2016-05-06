package com.ericchee.bboyairwreck.piemessage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private String prefSocketAddressKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.prefSocketAddressKey = getString(R.string.pref_socket_address_key);

        // Check if Set IP Address
        if (!sharedPreferences.contains(prefSocketAddressKey)) {
            // Show IP Set up
            setContentView(R.layout.activity_main);
            sharedPreferences.getString(prefSocketAddressKey, "127.0.0.1");

            Button btnStartPie = (Button) findViewById(R.id.btnStartPie);
            btnStartPie.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Get value and
                    EditText etSocketAddress = (EditText) findViewById(R.id.etSocketAddress);

                    // Check if IP is valid string
                    if (validIP(etSocketAddress.getText().toString().trim())) {
                        // Save preference
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(prefSocketAddressKey, etSocketAddress.getText().toString().trim());
                        editor.apply();

                        // Start ReceiveMessagesService and load ChatActivity
                        PieMessageApplication.getInstance().startReceieveMessagesService();
                        startChatActivity();
                    } else {
                        CharSequence text = "In valid IP address";
                        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // Load to ChatActivity
            startChatActivity();
        }

    }

    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private void startChatActivity() {
        Intent chatActivityIntent = new Intent(this, ChatsActivity.class);
        startActivity(chatActivityIntent);
        finish();
    }
}
